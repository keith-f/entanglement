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
import org.ektorp.CouchDbConnector;
import org.ektorp.StreamingViewResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;

import java.util.Iterator;

/**
 * An iterable that is capable of streaming nodes from the database (either ALL nodes, or nodes by type).
 *
 * WARNING: this implementation is suitable only for small graphs.
 * Due to the nature of data integration, the number of graph elements is only known after iterating.
 * This method must keep track of all 'seen' node identifiers, and is therefore only suitable for datasets whose
 * identifiers fit into RAM. However, node content is streamed, so iterating reasonably large datasets should be possible.
 *
 * @author Keith Flanagan
 */
public class IteratorForStreamingAllNodes<C extends Content> implements Iterable<Node<C>> {

  private final CouchDbConnector db;
  private final NodeDAO nodeDao;
//  private final EntityKeys fromFullNodeKeyset;

  public IteratorForStreamingAllNodes(CouchDbConnector db, NodeDAO nodeDao) { //}, EntityKeys fromFullNodeKeyset) {
    this.db = db;
    this.nodeDao = nodeDao;
//    this.fromFullNodeKeyset = fromFullNodeKeyset;
  }
  @Override
  public Iterator<Node<C>> iterator() {
    //TODO would it be more efficient to query a view that doesn't contain node updates? All we need here are identifiers
    //TODO we're also iterating over more than just 'node' row types (0). We don't need to iterate over everything here...
    ViewQuery query = ViewQueryFactory.createReducedNodesAndEdgesQuery(db);

    // query = query.key(ComplexKey.of(typeName)); // Optionally limit to a particular entity type
    StreamingViewResult result = db.queryForStreamingView(query.reduce(true).group(true).groupLevel(4));
    final Iterator<ViewResult.Row> resultItr = result.iterator();

    return new Iterator<Node<C>>() {
      private final EntityKeyElementCache seenEdges = new InMemoryEntityKeyElementCache();
      private Node<C> nextNode = null;

      private Node<C> findNext() throws GraphModelException {
        Node<C> next = null;
        while(resultItr.hasNext()) {
          ViewResult.Row row = resultItr.next();
          Iterator<JsonNode> keyNodeItr = row.getKeyAsNode().iterator();
          String entityType = keyNodeItr.next().asText(); // Entity type (eg, 'Gene', 'Chromosome')
          String keyType = keyNodeItr.next().asText();    // Key type (either 'N' or 'U' for Name/UID, respectively)
          String identifier = keyNodeItr.next().asText(); // The entity name/UID
          int rowType = keyNodeItr.next().asInt();        // 0 for node info, 1 for 'from' edge.
          // We don't need further key items

          if (rowType != NodeDAOCouchDbImpl.RowType.NODE.getDbTypeIdx()) {
            // FIXME we wouldn't need this if we used a more appropriate CouchDB View that didn't contain edge info as well.
            continue;
          }

          EntityKeys partialKeyset = new EntityKeys();
          partialKeyset.setType(entityType);
          if (keyType.equals(IdentifierType.NAME.getDbString())) {
            partialKeyset.addName(identifier);
          } else if (keyType.equals(IdentifierType.UID.getDbString())) {
            partialKeyset.addUid(identifier);
          } else {
            throw new RuntimeException("Unsupported identifier type: "+keyType);
          }

          if (seenEdges.seenElementOf(partialKeyset)) {
            continue; //We've seen this identifier as the name of another entity. Skip to avoid returning duplicates.
          }

          next = nodeDao.getByKey(partialKeyset);
          break; // We've found the next node
        }

        return next;
      }

      @Override
      public boolean hasNext() {
        if (nextNode != null) {
          return true;
        }
        try {
          nextNode = findNext();
        } catch (GraphModelException e) {
          throw new RuntimeException("Failed to find the next item", e);
        }
        return nextNode != null;
      }

      @Override
      public Node<C> next() {
        if (!hasNext()) {
          throw new RuntimeException("Attempt to iterate beyond the resultset.");
        }
        Node<C> toReturn = nextNode;
        nextNode = null;
        return toReturn;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Remove operations not supported.");
      }
    };
  }
}
