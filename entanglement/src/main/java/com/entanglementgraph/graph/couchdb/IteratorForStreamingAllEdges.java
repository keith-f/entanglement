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

/**
 * An iterable that is capable of streaming edges from the database (either ALL edges, or edges by type).
 *
 * WARNING: this implementation is suitable only for small graphs.
 * Due to the nature of data integration, the number of graph elements is only known after iterating.
 * This method must keep track of all 'seen' node identifiers, and is therefore only suitable for datasets whose
 * identifiers fit into RAM. However, node content is streamed, so iterating reasonably large datasets should be possible.
 *
 * @author Keith Flanagan
 */
public class IteratorForStreamingAllEdges<C extends Content> implements Iterable<Edge> {

  private final CouchDbConnector db;
  private final EdgeDAO edgeDao;
  private final String typeName;

  public IteratorForStreamingAllEdges(CouchDbConnector db, EdgeDAO edgeDao) {
    this.db = db;
    this.edgeDao = edgeDao;
    this.typeName = null;
  }

  public IteratorForStreamingAllEdges(CouchDbConnector db, EdgeDAO edgeDao, String typeName) {
    this.db = db;
    this.edgeDao = edgeDao;
    this.typeName = typeName;
  }

  @Override
  public Iterator<Edge> iterator() {
    //TODO would it be more efficient to query a view that doesn't contain node updates? All we need here are identifiers
    //TODO we're also iterating over more than just 'node' row types (0). We don't need to iterate over everything here...
    ViewQuery query = ViewQueryFactory.createEdgesQuery(db);

    if (typeName != null) {
      query = query.startKey(ComplexKey.of(typeName)); // Optionally limit to a particular entity type
    }

    StreamingViewResult result = db.queryForStreamingView(query);
    final Iterator<ViewResult.Row> resultItr = result.iterator();

    return new Iterator<Edge>() {
      private final EntityKeyElementCache seenEdges = new InMemoryEntityKeyElementCache();
      private Edge nextEdge = null;

      private Edge findNext() throws GraphModelException {
        Edge next = null;
        while(resultItr.hasNext()) {
          ViewResult.Row row = resultItr.next();
          Iterator<JsonNode> keyNodeItr = row.getKeyAsNode().iterator();
          String entityType = keyNodeItr.next().asText(); // Entity type (eg, 'has-location', 'part-of')
          String keyType = keyNodeItr.next().asText();    // Key type (either 'N' or 'U' for Name/UID, respectively)
          String identifier = keyNodeItr.next().asText(); // The entity name/UID
          int rowType = keyNodeItr.next().asInt();        // 0 for edge info
          // We don't need further key items

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

          next = edgeDao.getByKey(partialKeyset);
          seenEdges.cacheElementsOf(next.getKeys());
          break; // We've found the next edge
        }

        return next;
      }

      @Override
      public boolean hasNext() {
        if (nextEdge != null) {
          return true;
        }
        try {
          nextEdge = findNext();
        } catch (GraphModelException e) {
          throw new RuntimeException("Failed to find the next item", e);
        }
        return nextEdge != null;
      }

      @Override
      public Edge next() {
        if (!hasNext()) {
          throw new RuntimeException("Attempt to iterate beyond the resultset.");
        }
        Edge toReturn = nextEdge;
        nextEdge = null;
        return toReturn;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Remove operations not supported.");
      }
    };
  }
}
