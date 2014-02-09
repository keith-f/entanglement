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

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.util.EntityKeyElementCache;
import com.entanglementgraph.util.InMemoryEntityKeyElementCache;
import org.ektorp.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
  private final CouchDbConnector db;
  private final EntityKeys fromFullNodeKeyset;

  public FromEdgeStreamingIterator(CouchDbConnector db, EntityKeys fromFullNodeKeyset) {
    this.db = db;
    this.fromFullNodeKeyset = fromFullNodeKeyset;
  }

  @Override
  public Iterator<Edge<C, F, T>> iterator() {
    return new Iterator<Edge<C, F, T>>() {
      private EntityKeyElementCache seenEdges = new InMemoryEntityKeyElementCache();

      private Iterator<Iterable<ViewResult.Row>> keysetRowItr = new KeysetRowIterator(db, fromFullNodeKeyset).iterator();

      private Iterator<ViewResult.Row> currentViewResults;

      private ViewResult.Row nextRow;

      @Override
      public boolean hasNext() {
        while ((currentViewResults == null || !currentViewResults.hasNext()) && keysetRowItr.hasNext()) {
          currentViewResults = keysetRowItr.next().iterator(); // Perform new View query using the next EntityKeys key
        }
        if (currentViewResults == null || !currentViewResults.hasNext()) {
          return false;
        } else {
          return true;
        }
      }

      @Override
      public Edge<C, F, T> next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No more elements in this iterator.");
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
          if (uidItr.hasNext()) {
            return new StreamRowsForNodeUidIterator(db, uidItr.next());
          } else if (nameItr.hasNext()) {
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
