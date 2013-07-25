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
package com.entanglementgraph.iteration;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.cursor.GraphCursorException;
import com.entanglementgraph.graph.GraphEntityDAO;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that enables you to build graph iterators using an Entanglement <code>GraphCursor</code> to traverse
 * nodes in a source graph (or view) and copy relevant or matching nodes/edges into a destination graph.
 *
 * A <code>GraphCursor</code> is taken as the start point for the query. By default, all edges from this starting
 * point are iterated outwards and nodes/edges that are encountered are transitively added to the result set.
 *
 * It is possible to customise this iteration based on entity type and/or specific individuals in the graph.
 * For example, in the simplest case, iteration can be blocked when particular node type is encountered. The
 * iteration process will then back-track to the next available edge, and continue the iteration from there.
 *
 * More complex result modifications are also possible, such as 'skipping' a particular named node, or node of a given
 * type, resulting in a set of disjoint subgraphs in the result set.
 *
 * User: keith
 * Date: 25/07/13; 14:09
 *
 * @author Keith Flanagan
 */
public class GraphIterator {

  public static enum NodeBasedModificationAction {
    /**
     * When triggered by a particular node or type of node, causes further cursor iterations to halt. The node that
     * caused the iterations to cease is included in the final result set.
     */
    BLOCK_ITERATION_AND_INCLUDE,
    /**
     * When triggered by a particular node or type of node, causes further cursor iterations to halt. The node that
     * caused the iterations to cease is NOT included in the final result set.
     */
    BLOCK_ITERATION_AND_EXCLUDE,
    /**
     * When triggered by a particular node or type of node, iterations continue as normal, but this particular node
     * is NOT included in the result set. This can be useful when you're iterating over an integrated dataset joined
     * by one or more 'hub' nodes and you want the result set to contain a discrete set of graphs to contain everything
     * except the hub nodes.
     */
    CONTINUE_AND_EXCLUDE,

    /**
     * Similar to <code>CONTINUE_AND_EXCLUDE</code>, except the two edges that would otherwise link to the common
     * hub node are merged together and given a new type and identity.
     */
    CONTINUE_AND_EXCLUDE_BUT_JOIN_CHILD_NODES
  }

  public static class TypeBasedModifier {
    private String typeName;
    private NodeBasedModificationAction actionType;

    public TypeBasedModifier(String typeName, NodeBasedModificationAction actionType) {
      this.typeName = typeName;
      this.actionType = actionType;
    }

    public String getTypeName() {
      return typeName;
    }

    public void setTypeName(String typeName) {
      this.typeName = typeName;
    }

    public NodeBasedModificationAction getActionType() {
      return actionType;
    }

    public void setActionType(NodeBasedModificationAction actionType) {
      this.actionType = actionType;
    }
  }

//  public static class IndividualBasedModifier {
//
//  }


  private GraphConnection destination;
  private final Map<String, TypeBasedModifier> nodeTypeModifiers;
  private GraphCursor cursor;

  public GraphIterator(GraphConnection destination) {
    this.destination = destination;
    nodeTypeModifiers = new HashMap<>();
  }

  private GraphIterator(GraphIterator previous) {
    this.nodeTypeModifiers = previous.nodeTypeModifiers;
  }

  public void addNodeTypeModifier(TypeBasedModifier modifier) {
    nodeTypeModifiers.put(modifier.getTypeName(), modifier);
  }

  public void iterate(GraphConnection graphConn) throws GraphCursorException, DbObjectMarshallerException {
    for (GraphCursor.NodeEdgeNodeTuple nen : cursor.iterateAndResolveIncomingEdgeDestPairs(graphConn)) {
      DBObject remoteNode = nen.getRawSourceNode();
      DBObject edge = nen.getRawEdge();

      EntityKeys<Node> nodeId = MongoUtils.parseKeyset(graphConn.getMarshaller(), remoteNode, GraphEntityDAO.FIELD_KEYS);
      EntityKeys<Edge> edgeId = MongoUtils.parseKeyset(graphConn.getMarshaller(), edge, GraphEntityDAO.FIELD_KEYS);

      handleEdge(edge);

      //Check if the node or edge match any of the custom modifier rules.
      if (nodeTypeModifiers.containsKey(nodeId.getType())) {

      } else {
        defaultHandler(edge, remoteNode);
      }
    }
  }

  protected void handleEdge(DBObject edge) {

  }

  protected void defaultHandler(DBObject edge, DBObject node) {

  }
}
