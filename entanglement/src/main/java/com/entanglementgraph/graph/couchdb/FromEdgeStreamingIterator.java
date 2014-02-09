/*
 * Copyright 2013 Keith Flanagan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.util.EntityKeyElementCache;
import com.entanglementgraph.util.InMemoryEntityKeyElementCache;
import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.*;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * A set of compound iterators that return the full merged outgoing Edges from a specified Node.
 *
 * For each key in a full node keyset, queries the appropriate CouchDB View that provides a list of Edge identifiers.
 * For each Edge identifier, the full keyset is resolved. If this edge hasn't already been returned to the caller,
 * then the Edge is composed from the relevant EdgeModification document updates.
 *
 *
 * 1) Found edge: e (while looping over the reduced dataset).
 * 2) [optional if we permit multiple IDs for edges] Resolve all other names (full keyset) that e is known by. For example, f and g.
 * 3) We now have: e, f, g.
 * 4) Now, find all EdgeModification instances for the full (or partial) keyset. In the same way we do for nodes, sort the EdgeModifications by timestamp and perform a ‘merge’.
 * 5) Return the completed Edge to the user.
 *
 * @author Keith Flanagan
 */
public class FromEdgeStreamingIterator<C extends Content, F extends Content, T extends Content> implements Iterable<Edge<C, F, T>> {
  private static final Logger logger = Logger.getLogger(FromEdgeStreamingIterator.class.getName());

  private final CouchDbConnector db;
  private final EdgeDAO edgeDao;
  private final EntityKeys fromFullNodeKeyset;

  public FromEdgeStreamingIterator(CouchDbConnector db, EdgeDAO edgeDao, EntityKeys fromFullNodeKeyset) {
    this.db = db;
    this.edgeDao = edgeDao;
    this.fromFullNodeKeyset = fromFullNodeKeyset;
  }

  @Override
  public Iterator<Edge<C, F, T>> iterator() {
    return new Iterator<Edge<C, F, T>>() {

      private EntityKeyElementCache seenEdges = new InMemoryEntityKeyElementCache();
      private Iterator<Iterable<ViewResult.Row>> keysetRowItr = new KeysetRowIterator(db, fromFullNodeKeyset).iterator();
      private Iterator<ViewResult.Row> currentViewResults;

      private Edge<C, F, T> nextEdge = null;

      /**
       * In the end, this keyset *should* end up being identical to the fromFullNodeKeyset.
       * It is populated from each node row that we read from View result sets.
       */
      private EntityKeys inferredFullKeys = new EntityKeys();

      /**
       * Checks if there is at least one more Row from at least one View query.
       * @return
       */
      private boolean hasNextResultRow() {
        while ((currentViewResults == null || !currentViewResults.hasNext()) && keysetRowItr.hasNext()) {
          currentViewResults = keysetRowItr.next().iterator(); // Perform new View query using the next EntityKeys key
        }
        if (currentViewResults == null || !currentViewResults.hasNext()) {
          return false;
        } else {
          return true;
        }
      }

      /**
       * Returns an Edge that we haven't seen before, or null if no novel Edge can be found.
       * This method may need to iterate over multiple View Rows, or even multiple View result sets before a novel
       * edge is found (especially if many edges have many shared identifiers).
       * @return
       */
      private Edge<C, F, T> findNextEdge() throws GraphModelException {
        Edge<C, F, T> edge = null;

        /*
         * Here, we only really want one row, but we may have to read several before we find an edge that is 'new'
         */
        while (edge == null) {
          // If there are no more result Rows and now more result sets, then there aren't any more edges
          if (!hasNextResultRow()) {
            return null;
          }

          /*
           * Read the next View result row.
           * Here, a keys will look like:
           * [node_type, a, 0, [a, b, c]]               (node type, node name, followed by 0, followed by node synonyms)
           * [node_type, a, 1, edge_type, [x, y, z]]    (node type, node name, followed by 1, followed by edge names for edges FROM the named node)
           *
           * Values will be of the form:
           * TODO... (NodeMod docs for nodes, EdgeMod docs for edges??)
           */
          ViewResult.Row nextRow = currentViewResults.next();

          JsonNode key = nextRow.getKeyAsNode();
          String nodeType = key.get(0).asText();
          String nodeName = key.get(1).asText();
          int docType = key.get(2).asInt();

          if (docType == 0) {
            JsonNode otherNameList = key.get(3);
            logger.info("Found node entry with node type:"+nodeType+", node name: "+nodeName+", other node names: "+otherNameList.asText());
            // Add new node synonyms to the list to check

            for (JsonNode otherNameEntry : otherNameList) {
              inferredFullKeys.addName(otherNameEntry.asText());
            }
          } else if (docType == 1) {
            String edgeType = key.get(3).asText();
            JsonNode otherNameList = key.get(4);
            logger.info("Found edge entry with node type:"+nodeType+", node name: "+nodeName
                +", edge type: "+edgeType+", edge names: "+otherNameList.asText());
            EntityKeys fromEdgePartial = new EntityKeys(); // Partial because this may not be all the edge's identifiers
            fromEdgePartial.setType(edgeType);
            for (JsonNode edgeNameEntry : otherNameList) {
              fromEdgePartial.addName(edgeNameEntry.asText());
            }

            // Have we seen any of the partial edge names before?
            // Do this check first since it saves a DB lookup if we have.
            if (seenEdges.seenElementOf(fromEdgePartial)) {
              //We've seen at least one of the names before (and therefore returned it already).
              //Ensure all the names are cached, and proceed to read the next row.
              seenEdges.cacheElementsOf(fromEdgePartial);
              continue;
            }

            // Find all names for this edge.
            EntityKeys fromEdgeFull = edgeDao.populateFullKeyset(fromEdgePartial);
            if (seenEdges.seenElementOf(fromEdgeFull)) {
              //We've seen at least one of the names before (and therefore returned it already).
              //Ensure all the names are cached, and proceed to read the next row.
              seenEdges.cacheElementsOf(fromEdgeFull);
              continue;
            }

            //If we get here then this is a novel edge.
            // Cache all identifiers. Then find all EdgeModifications. Merge. Return the complete Edge.
            seenEdges.cacheElementsOf(fromEdgePartial);

            edge = edgeDao.getByKey(fromEdgeFull);
          } else {
            throw new GraphModelException("Unsupported document type found in view: "+docType+" on row: "+nextRow);
          }
        } // End of while loop...

        return edge;
      }


      @Override
      public boolean hasNext() {
        // Here, we need to find a valid Edge before we know whether there is one.
        if (nextEdge == null) {
          // If we do find an Edge, then cache it so that we don't need to find it for the next request.
          try {
            nextEdge = findNextEdge();
          } catch (GraphModelException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to perform iterator operation", e);
          }
        }
        return nextEdge != null;
      }

      @Override
      public Edge<C, F, T> next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No more elements in this iterator.");
        }
        Edge<C, F, T> toReturn = nextEdge;
        nextEdge = null;

        logger.info("Returning next edge: "+toReturn.toString()
            + "\nSpecified 'from' node was: "+fromFullNodeKeyset
            + "\nInferred 'from' node is currently: "+inferredFullKeys);
        return toReturn;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Removal is not supported");
      }
    };
  }

  /**
   * A class that takes a node's EntityKeys object and iterates over all identifiers present.
   * For each key, returns an instance of <code>Iterator<ViewResult.Row></code>.
   * Therefore, as each identifier is iterated, all View rows associated with that identifier are queried from the
   * database.
   */
  private static class KeysetRowIterator implements Iterable<Iterable<ViewResult.Row>> {
    private final CouchDbConnector db;
    private final EntityKeys fullNodeKeyset;

    public KeysetRowIterator(CouchDbConnector db, EntityKeys fullNodeKeyset) {
      this.db = db;
      this.fullNodeKeyset = fullNodeKeyset;
    }


    @Override
    public Iterator<Iterable<ViewResult.Row>> iterator() {
      return new Iterator<Iterable<ViewResult.Row>>() {

        final Iterator<String> uidItr = fullNodeKeyset.getUids().iterator();
        final Iterator<String> nameItr = fullNodeKeyset.getNames().iterator();

        @Override
        public boolean hasNext() {
          return uidItr.hasNext() || nameItr.hasNext();
        }

        @Override
        public Iterable<ViewResult.Row> next() {
//          if (uidItr.hasNext()) {
//            return new StreamRowsForNodeUidIterator(db, uidItr.next());
//          } else
          if (nameItr.hasNext()) {
            return new StreamRowsForNodeNameIterator(db, fullNodeKeyset.getType(), nameItr.next());
          } else {
            throw new NoSuchElementException("No more elements in this iterator.");
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("Removal is not supported");
        }
      };
    }
  }

  /**
   * Given a node name, streams all rows from a view starting with the node name.
   */
  private static class StreamRowsForNodeNameIterator implements Iterable<ViewResult.Row> {

    private final CouchDbConnector db;
    private final String nodeType;
    private final String nodeName;

    public StreamRowsForNodeNameIterator(CouchDbConnector db, String nodeType, String nodeName) {
      this.db = db;
      this.nodeType = nodeType;
      this.nodeName = nodeName;
    }


    @Override
    public Iterator<ViewResult.Row> iterator() {
      return new Iterator<ViewResult.Row>() {

        ViewQuery query = new ViewQuery()
            .dbPath(db.path())
            .designDocId(EdgeDAOCouchDbImpl.DESIGN_DOC_ID)
            .viewName("nodes_and_edges")
            .key(ComplexKey.of(nodeType, nodeName))
            .reduce(true)
            .group(true)
            .groupLevel(2);

        final StreamingViewResult result = db.queryForStreamingView(query);
        final Iterator<ViewResult.Row> resultItr = result.iterator();


        @Override
        public boolean hasNext() {
          return resultItr.hasNext();
        }

        @Override
        public ViewResult.Row next() {
          return resultItr.next();
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("Removal is not supported");
        }
      };
    }
  }

  /**
   * Given a node UID, streams all rows from a view starting with the UID.
   */
  private static class StreamRowsForNodeUidIterator implements Iterable<ViewResult.Row> {

    private final CouchDbConnector db;
    private final String nodeUid;

    public StreamRowsForNodeUidIterator(CouchDbConnector db, String nodeUid) {
      this.db = db;
      this.nodeUid = nodeUid;
    }


    @Override
    public Iterator<ViewResult.Row> iterator() {
      return new Iterator<ViewResult.Row>() {

        ViewQuery query = new ViewQuery()
            .dbPath(db.path())
            .designDocId(EdgeDAOCouchDbImpl.DESIGN_DOC_ID)
            .viewName("nodes_and_edges_by_node_uid")
            .key(ComplexKey.of(nodeUid))
            .reduce(true)
            .group(true)
            .groupLevel(2);

        final StreamingViewResult result = db.queryForStreamingView(query);
        final Iterator<ViewResult.Row> resultItr = result.iterator();


        @Override
        public boolean hasNext() {
          return resultItr.hasNext();
        }

        @Override
        public ViewResult.Row next() {
          return resultItr.next();
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("Removal is not supported");
        }
      };
    }
  }
}
