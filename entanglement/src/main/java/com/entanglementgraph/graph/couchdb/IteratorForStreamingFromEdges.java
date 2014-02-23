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
import com.entanglementgraph.graph.couchdb.viewparsers.NodesAndEdgesViewRowParser;
import com.entanglementgraph.util.EntityKeyElementCache;
import com.entanglementgraph.util.InMemoryEntityKeyElementCache;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.*;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * An iterable that is capable of streaming edges starting at a specified node (either ALL edges, or edges by type).
 *
 * WARNING: this implementation is suitable only for nodes with 'reasonable' numbers of edges.
 * This method must keep track of all 'seen' edge identifiers, and currently uses RAM-based storage.
 * If there are many millions of edges associated with the specified node, you may run out of memory.
 *
 * @author Keith Flanagan
 */
public class IteratorForStreamingFromEdges<C extends Content, F extends Content, T extends Content>
    implements Iterable<Edge<C, F, T>> {

  private static final Logger logger = Logger.getLogger(IteratorForStreamingFromEdges.class.getSimpleName());

  private final CouchDbConnector db;
  private final EdgeDAO edgeDao;
  private final EntityKeys fromNodeKeyset;
  private final String qryEdgeTypeName;

  public IteratorForStreamingFromEdges(CouchDbConnector db, EdgeDAO edgeDao, EntityKeys fromNodeKeyset) {
    this.db = db;
    this.edgeDao = edgeDao;
    this.fromNodeKeyset = fromNodeKeyset;
    this.qryEdgeTypeName = null;
  }

  public IteratorForStreamingFromEdges(CouchDbConnector db, EdgeDAO edgeDao, EntityKeys fromNodeKeyset, String qryEdgeTypeName) {
    this.db = db;
    this.edgeDao = edgeDao;
    this.fromNodeKeyset = fromNodeKeyset;
    this.qryEdgeTypeName = qryEdgeTypeName;
  }



  @Override
  public Iterator<Edge<C, F, T>> iterator() {
    return new Iterator<Edge<C, F, T>>() {
      private final EntityKeyElementCache seenEdges = new InMemoryEntityKeyElementCache();
      private Iterator<Iterable<ViewResult.Row>> keysetRowItr = new KeysetRowIterator(db, fromNodeKeyset).iterator();
      private Iterator<ViewResult.Row> currentViewResults;
      private Edge nextEdge = null;

      /**
       * In the end, this keyset *should* end up being identical to the fromFullNodeKeyset.
       * It is populated from each node row that we read from View result sets.
       */
      private EntityKeys inferredFullNodeKeys = new EntityKeys();

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
          logger.info("There is currently no 'next' edge known. Going to attempt to find one.");
          // If there are no more result Rows and now more result sets, then there aren't any more edges
          if (!hasNextResultRow()) {
            logger.info("There aren't any more result sets to try. There really is no next edge.");
            return null;
          }

          //Read the next View result row.
          ViewResult.Row nextRow = currentViewResults.next();
          NodesAndEdgesViewRowParser parser = new NodesAndEdgesViewRowParser(nextRow);

          String nodeTypeName = parser.getNodeTypeName();
          String uidOrName = parser.getUidOrName();
          String nodeIdentifier = parser.getNodeIdentifer();
          int rowType = parser.getRowType();

          JsonNode otherNodeUids = parser.getOtherNodeUids();
          JsonNode otherNodeNames = parser.getOtherNodeNames();

          // Extract all node identifiers from the row key
          for (JsonNode identifier : otherNodeUids) {
            inferredFullNodeKeys.addUid(identifier.asText());
          }
          for (JsonNode identifier : otherNodeNames) {
            inferredFullNodeKeys.addName(identifier.asText());
          }

          if (rowType == NodesAndEdgesViewRowParser.RowType.NODE.getDbTypeIdx()) {
            logger.info("Found node entry with node type:"+nodeTypeName
                +", node identifier: "+uidOrName+":"+nodeIdentifier
//                +", other node identifiers: U:"+otherNodeUids.asText()+" N:"+otherNodeNames.asText()
                + ". Skipping since we're interested in edges only..."
            );
          } else if (rowType == NodesAndEdgesViewRowParser.RowType.EDGE_FROM_NODE.getDbTypeIdx()) {
            String edgeTypeName = parser.getEdgeTypeName();
            // Optionally limit to a particular entity type
            if (qryEdgeTypeName != null && !qryEdgeTypeName.equals(edgeTypeName)) {
              continue; //Not interested in this edge if it's type doesn't match the user-specified type.
            }
            JsonNode otherEdgeUids = parser.getOtherEdgeUids();
            JsonNode otherEdgeNames = parser.getOtherEdgeNames();
            logger.info("Found edge entry with node type:"+nodeTypeName+", node identifier: "+nodeIdentifier
                +", edge type: "+edgeTypeName
                +", edge UIDs: "+otherEdgeUids
                +", edge names: "+otherEdgeNames);
            EntityKeys fromEdgePartial = new EntityKeys(); // Partial because this may not be all the edge's identifiers
            fromEdgePartial.setType(edgeTypeName);
            for (JsonNode identifier : otherEdgeUids) {
              fromEdgePartial.addUid(identifier.asText());
            }
            for (JsonNode identifier : otherEdgeNames) {
              fromEdgePartial.addName(identifier.asText());
            }

            // Have we seen any of the partial edge names before?
            // Do this check first since it saves a DB lookup if we have.
            if (seenEdges.seenElementOf(fromEdgePartial)) {
              //We've seen at least one of the names before (and therefore returned it already).
              //Ensure all the names are cached, and proceed to read the next row.
              seenEdges.cacheElementsOf(fromEdgePartial);
              logger.info("We've seen this edge before. Skipping.");
              continue;
            }

            // Find all names for this edge.
//            EntityKeys fromEdgeFull = edgeDao.populateFullKeyset(fromEdgePartial);
            edge = edgeDao.getByKey(fromEdgePartial);
            if (seenEdges.seenElementOf(edge.getKeys())) {
              //We've seen at least one of the names before (and therefore returned it already).
              //Ensure all the names are cached, and proceed to read the next row.
              seenEdges.cacheElementsOf(edge.getKeys());
              logger.info("We've seen this edge before. Skipping.");
              continue;
            }

            //If we get here then this is a novel edge.
            // Cache all identifiers. Then find all EdgeModifications. Merge. Return the complete Edge.
            seenEdges.cacheElementsOf(fromEdgePartial);
            logger.info("Returning new edge.");

          } else {
            logger.info("Unsupported row type found in view: " + rowType + " on row: " + nextRow + ". Skipping");
          }
        } // End of while loop...

        return edge;
      }


      @Override
      public boolean hasNext() {
        if (nextEdge != null) {
          return true;
        }
        try {
          nextEdge = findNextEdge();
        } catch (GraphModelException e) {
          throw new RuntimeException("Failed to find the next item", e);
        }
        return nextEdge != null;
      }

      @Override
      public Edge next() {
        if (!hasNext()) {
          throw new NoSuchElementException("Attempt to iterate beyond the resultset.");
        }
        Edge toReturn = nextEdge;
        nextEdge = null;

        logger.info("Returning next edge: "+toReturn.toString()
            + "\nSpecified 'from' node was: "+fromNodeKeyset
            + "\nInferred 'from' node is currently: "+ inferredFullNodeKeys);
        return toReturn;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Remove operations not supported.");
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
          if (uidItr.hasNext()) {
            return new StreamRowsForNodeUidIterator(db, fullNodeKeyset.getType(), uidItr.next());
          } else
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
    private final String nodeTypeName;
    private final String nodeName;

    public StreamRowsForNodeNameIterator(CouchDbConnector db, String nodeTypeName, String nodeName) {
      this.db = db;
      this.nodeTypeName = nodeTypeName;
      this.nodeName = nodeName;
    }


    @Override
    public Iterator<ViewResult.Row> iterator() {
      final ViewQuery query =
          ViewQueryFactory.createNodesAndEdgesQuery(db)
              .startKey(ComplexKey.of(
                  nodeTypeName, IdentifierType.NAME.getDbString(), nodeName,
                  NodesAndEdgesViewRowParser.RowType.EDGE_FROM_NODE.getDbTypeIdx()))
              .endKey(ComplexKey.of(
                  nodeTypeName, IdentifierType.NAME.getDbString(), nodeName,
                  NodesAndEdgesViewRowParser.RowType.EDGE_FROM_NODE.getDbTypeIdx(), ComplexKey.emptyObject()));

      final StreamingViewResult result = db.queryForStreamingView(query);
//      final ViewResult result = db.queryView(query);
      final Iterator<ViewResult.Row> resultItr = result.iterator();

      return new Iterator<ViewResult.Row>() {
        int returnedCount = 0;

        @Override
        public boolean hasNext() {
          try {
            return resultItr.hasNext();
          } catch (Exception e) {
            // Workaround for Ektorp throwing a JsonMappingException when calling hasNext on an empty result set
            return false;
          }
        }

        @Override
        public ViewResult.Row next() {
          ViewResult.Row next = resultItr.next();
          returnedCount++;
          return next;
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
    private final String nodeTypeName;
    private final String nodeUid;

    public StreamRowsForNodeUidIterator(CouchDbConnector db, String nodeTypeName, String nodeUid) {
      this.db = db;
      this.nodeTypeName = nodeTypeName;
      this.nodeUid = nodeUid;
    }


    @Override
    public Iterator<ViewResult.Row> iterator() {
      return new Iterator<ViewResult.Row>() {

        ViewQuery query =
            ViewQueryFactory.createNodesAndEdgesQuery(db)
                .startKey(ComplexKey.of(
                    nodeTypeName, IdentifierType.UID.getDbString(), nodeUid,
                    NodesAndEdgesViewRowParser.RowType.EDGE_FROM_NODE.getDbTypeIdx()))
                .endKey(ComplexKey.of(
                    nodeTypeName, IdentifierType.UID.getDbString(), nodeUid,
                    NodesAndEdgesViewRowParser.RowType.EDGE_FROM_NODE.getDbTypeIdx(), ComplexKey.emptyObject()));

        final StreamingViewResult result = db.queryForStreamingView(query);
        final Iterator<ViewResult.Row> resultItr = result.iterator();


        @Override
        public boolean hasNext() {
          try {
            return resultItr.hasNext();
          } catch (Exception e) {
            // Workaround for Ektorp throwing a JsonMappingException when calling hasNext on an empty result set
            return false;
          }
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
