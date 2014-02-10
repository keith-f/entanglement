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

import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Converts an Iterable of DBObject Edges (eg, a DBCursor), and returns an Iterable of NodeEdgeNodeTuple instances
 * by performing appropriate database lookups behind the scenes.
 *
 * If a hanging edge is encountered (and edge that references a node by keyset for which no 'node' document exists
 * in the graph), then one of two things can occur:
 * <ul>
 *   <li>Do nothing - in this case, one or both of the 'node' parts of the tuple will be NULL</li>
 *   <li>Create a 'dummy' DBObject document. This has the advantage of being compatible with exports to tools that
 *   don't understand hanging edges. This is the default option.</li>
 * </ul>
 *
 * @author Keith Flanagan
 */
public class EdgeIteratorToNENTupleIterator implements Iterable<GraphCursor.NodeEdgeNodeTuple> {
  private static final Logger logger = Logger.getLogger(EdgeIteratorToNENTupleIterator.class.getSimpleName());

  private final GraphConnection conn;
//  private final BasicDBObject subjectNodePosition;
  private final BasicDBObject subjectNode;
  private final boolean edgesAreOutgoing;
  private final boolean fillNodeDocsOfHangingEdges; //If true, dummy node objects will be created for hanging edges
  private final Callable<DBCursor> queryExecutor;

  /**
   * Creates an edge iterator for the specified source (subject) node.
   *
   * @param conn a graph connection to use
   * @param fillNodeDocsOfHangingEdges If true, dummy node objects will be created for hanging edges.
   *                                   If false, the 'node' parts of hanging edges will remain NULL.
   * @param subjectNodePosition The node whose edges are to be iterated
   * @param subjectNode The full document of the node whose edges are to be iterated
   * @param edgesAreOutgoing Set TRUE to iterate outgoing edges. Set FALSE to iterate incoming edges
   * @param queryExecutor The custom query executor to use
   */
  public EdgeIteratorToNENTupleIterator(GraphConnection conn, boolean fillNodeDocsOfHangingEdges,
                                        EntityKeys<? extends Node> subjectNodePosition, BasicDBObject subjectNode,
                                        boolean edgesAreOutgoing, Callable<DBCursor> queryExecutor) {
    this.conn = conn;
    this.fillNodeDocsOfHangingEdges = fillNodeDocsOfHangingEdges;

//    this.subjectNodePosition = subjectNodePosition;
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
        return null;
//        BasicDBObject edgeObj = (BasicDBObject) edgeItr.next();
//        try {
//          EntityKeys<? extends Node> queryKeys = edgesAreOutgoing
//              ? marshaller.deserialize(edgeObj, Edge.class).getTo()
//              : marshaller.deserialize(edgeObj, Edge.class).getFrom();
//
////          EntityKeys<? extends Node> destinationKeys = marshaller.deserialize(edgeObj, Edge.class).getTo();
//          Node queryNode = conn.getNodeDao().getByKey(queryKeys);
//          if (queryNode == null) {
//            //This is probably a 'hanging' edge - we have the node reference, but no node exists.
//            logger.info("Potential hanging edge found: "+queryKeys);
//            if (fillNodeDocsOfHangingEdges) {
////              BasicDBObject filler = new BasicDBObject();
////              filler.put(GraphEntityDAO.FIELD_KEYS, marshaller.serialize(queryKeys));
////              filler.put(GraphEntityDAO.FIELD_VIRTUAL, true); // Flag this 'node' as fake
////              logger.info("Created 'filler' document for missing node: "+filler);
////              queryNode = filler;
//              queryNode = new Node(queryKeys);
//              queryNode.setVirtual(true);
//            }
//          }
//
//
//          GraphCursor.NodeEdgeNodeTuple nodeEdgeNode = edgesAreOutgoing
//              ? new GraphCursor.NodeEdgeNodeTuple(subjectNode, edgeObj, queryNode)
//              : new GraphCursor.NodeEdgeNodeTuple(queryNode, edgeObj, subjectNode);
//          return nodeEdgeNode;
//        } catch (Exception e) {
//          throw new RuntimeException("Failed to iterate destination nodes for: "+ subjectNode, e);
//        }

      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("remove() is not supported by this Iterator.");
      }
    };
  }
}
