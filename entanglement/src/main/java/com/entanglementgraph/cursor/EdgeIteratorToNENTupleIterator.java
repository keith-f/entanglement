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

package com.entanglementgraph.cursor;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Converts an Iterable of DBObject Edges (eg, a DBCursor), and returns an Iterable of NodeEdgeNodeTuple instances
 * by performing appropriate database lookups behind the scenes
 *
 * @author Keith Flanagan
 */
public class EdgeIteratorToNENTupleIterator implements Iterable<GraphCursor.NodeEdgeNodeTuple> {
  private static final Logger logger = Logger.getLogger(EdgeIteratorToNENTupleIterator.class.getSimpleName());

  private final DbObjectMarshaller marshaller;
  private final GraphConnection conn;
  private final EntityKeys<? extends Node> subjectNodePosition;
  private final BasicDBObject subjectNode;
  private final boolean edgesAreOutgoing;
  private final Callable<DBCursor> queryExecutor;

  public EdgeIteratorToNENTupleIterator(GraphConnection conn,
                                        EntityKeys<? extends Node> subjectNodePosition, BasicDBObject subjectNode,
                                        boolean edgesAreOutgoing, Callable<DBCursor> queryExecutor) {
    this.conn = conn;
    this.marshaller = conn.getMarshaller();

    this.subjectNodePosition = subjectNodePosition;
    this.edgesAreOutgoing = edgesAreOutgoing;
    this.queryExecutor = queryExecutor;
    this.subjectNode = subjectNode;
  }

  @Override
  public Iterator<GraphCursor.NodeEdgeNodeTuple> iterator() {
    final DBCursor edgeItr;
    try {
//      edgeItr = conn.getEdgeDao().iterateEdgesFromNode(position);
      edgeItr = queryExecutor.call();
    } catch (Exception e) {
      throw new RuntimeException("Failed to query database", e);
    }
    return new Iterator<GraphCursor.NodeEdgeNodeTuple>() {

      @Override
      public boolean hasNext() {
        return edgeItr.hasNext();
      }

      @Override
      public GraphCursor.NodeEdgeNodeTuple next() {
        BasicDBObject edgeObj = (BasicDBObject) edgeItr.next();
        try {
          EntityKeys<? extends Node> queryKeys = edgesAreOutgoing
              ? marshaller.deserialize(edgeObj, Edge.class).getTo()
              : marshaller.deserialize(edgeObj, Edge.class).getFrom();

//          EntityKeys<? extends Node> destinationKeys = marshaller.deserialize(edgeObj, Edge.class).getTo();
          BasicDBObject queryNode = conn.getNodeDao().getByKey(queryKeys);
          if (queryNode == null) {
            //This is probably a 'hanging' edge - we have the node reference, but no node exists.
            logger.info("Potential hanging edge found: "+queryKeys);
          }


          GraphCursor.NodeEdgeNodeTuple nodeEdgeNode = edgesAreOutgoing
              ? new GraphCursor.NodeEdgeNodeTuple(subjectNode, edgeObj, queryNode)
              : new GraphCursor.NodeEdgeNodeTuple(queryNode, edgeObj, subjectNode);
          return nodeEdgeNode;
        } catch (Exception e) {
          throw new RuntimeException("Failed to iterate destination nodes for: "+ subjectNode, e);
        }

      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("remove() is not supported by this Iterator.");
      }
    };
  }
}
