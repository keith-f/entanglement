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

import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A rule that collects all visited node and edge objects to an in-memory for collection. This rule can be useful
 * when the set of result nodes/edges is known to be small enough to fit entirely in RAM, and the results that you're
 * after are easily obtainable via a couple of queries.
 *
 * This rule's <code>HandlerAction</code> regarding iteration behaviour and further rule processing is configurable.
 * By default, this rule doesn't change iteration behaviour (continues as normal), and allows other rules to process
 * after this rule has executed. These defaults are set since this rule will match any node/edge.
 *
 * @author Keith Flanagan
 */
public class CollectAllNodesAndEdgesRule extends AbstractRule {
  private static final Logger logger = Logger.getLogger(CollectAllNodesAndEdgesRule.class.getName());

  private NextEdgeIteration iterationBehaviour;
  private boolean processFurtherRules;

  private final Map<EntityKeys<? extends Node>, BasicDBObject> keyToNode;
  private final Map<EntityKeys<? extends Edge>, BasicDBObject> keyToEdge;
  private final Map<String, Set<EntityKeys<? extends Node>>> nodesByType;
  private final Map<String, Set<EntityKeys<? extends Edge>>> edgesByType;

  private final Map<EntityKeys<? extends Node>, Set<EntityKeys<? extends Edge>>> edgesFromNode;
  private final Map<EntityKeys<? extends Node>, Set<EntityKeys<? extends Edge>>> edgesToNode;

  public CollectAllNodesAndEdgesRule() {
    this(NextEdgeIteration.CONTINUE_AS_NORMAL, true);
  }

  public CollectAllNodesAndEdgesRule(boolean processFurtherRules) {
    this(NextEdgeIteration.CONTINUE_AS_NORMAL, processFurtherRules);
  }

  public CollectAllNodesAndEdgesRule(NextEdgeIteration iterationBehaviour, boolean processFurtherRules) {
    this.iterationBehaviour = iterationBehaviour;
    this.processFurtherRules = processFurtherRules;
    this.keyToNode = new HashMap<>();
    this.keyToEdge = new HashMap<>();
    this.nodesByType = new HashMap<>();
    this.edgesByType = new HashMap<>();
    this.edgesFromNode = new HashMap<>();
    this.edgesToNode = new HashMap<>();
  }

  @Override
  public boolean ruleMatches(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
      throws RuleException  {
    return true;
  }

  @Override
  public HandlerAction apply(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
    action.setProcessFurtherRules(true);

    EntityKeys<? extends Node> from = outgoingEdge ? currentPosition : remoteNodeId;
    EntityKeys<? extends Node> to = outgoingEdge ? remoteNodeId : currentPosition;
    addNode(currentPosition, rawLocalNode);
    addNode(remoteNodeId, rawRemoteNode);
    addEdge(edgeId, rawEdge, from, to);

    return action;
  }


  private void addNode(EntityKeys<? extends Node> nodeKeys, BasicDBObject rawNode) {
    if (keyToNode.containsKey(nodeKeys)) {
      return;
    }
    // Index by key
    keyToNode.put(nodeKeys, rawNode);

    // Index by type
    Set<EntityKeys<? extends Node>> nodesOfType = nodesByType.get(nodeKeys.getType());
    if (nodesOfType == null) {
      nodesOfType = new HashSet<>();
      nodesByType.put(nodeKeys.getType(), nodesOfType);
    }
    nodesOfType.add(nodeKeys);
  }

  private void addEdge(EntityKeys<? extends Edge> edgeKeys, BasicDBObject rawEdge,
                       EntityKeys<? extends Node> from, EntityKeys<? extends Node> to) {
    if (keyToEdge.containsKey(edgeKeys)) {
      return;
    }
    // Index by key
    keyToEdge.put(edgeKeys, rawEdge);

    // Index by type
    Set<EntityKeys<? extends Edge>> edges = edgesByType.get(edgeKeys.getType());
    if (edges == null) {
      edges = new HashSet<>();
      edgesByType.put(edgeKeys.getType(), edges);
    }
    edges.add(edgeKeys);

    // Index from edge
    edges = edgesFromNode.get(from);
    if (edges == null) {
      edges = new HashSet<>();
      edgesFromNode.put(from, edges);
    }
    edges.add(edgeKeys);

    // Index to edge
    edges = edgesToNode.get(to);
    if (edges == null) {
      edges = new HashSet<>();
      edgesToNode.put(to, edges);
    }
    edges.add(edgeKeys);
  }


  public NextEdgeIteration getIterationBehaviour() {
    return iterationBehaviour;
  }

  public void setIterationBehaviour(NextEdgeIteration iterationBehaviour) {
    this.iterationBehaviour = iterationBehaviour;
  }

  public boolean isProcessFurtherRules() {
    return processFurtherRules;
  }

  public void setProcessFurtherRules(boolean processFurtherRules) {
    this.processFurtherRules = processFurtherRules;
  }

  public Map<EntityKeys<? extends Node>, BasicDBObject> getKeyToNode() {
    return keyToNode;
  }

  public Map<EntityKeys<? extends Edge>, BasicDBObject> getKeyToEdge() {
    return keyToEdge;
  }

  public Map<String, Set<EntityKeys<? extends Node>>> getNodesByType() {
    return nodesByType;
  }

  public Map<String, Set<EntityKeys<? extends Edge>>> getEdgesByType() {
    return edgesByType;
  }

  public Map<EntityKeys<? extends Node>, Set<EntityKeys<? extends Edge>>> getEdgesFromNode() {
    return edgesFromNode;
  }

  public Map<EntityKeys<? extends Node>, Set<EntityKeys<? extends Edge>>> getEdgesToNode() {
    return edgesToNode;
  }

  public <T> T getNodeDeserialisedAs(EntityKeys<? extends Node> key, Class<T> type) throws DbObjectMarshallerException {
    // FIXME reimplement this
    return null;
//    BasicDBObject doc = keyToNode.get(key);
//    return sourceGraph.getMarshaller().deserialize(doc, type);
  }

  public <T> T getEdgeDeserialisedAs(EntityKeys<? extends Edge> key, Class<T> type) throws DbObjectMarshallerException {
    // FIXME reimplement this
    return null;
//    BasicDBObject doc = keyToEdge.get(key);
//    return sourceGraph.getMarshaller().deserialize(doc, type);
  }

  public <T> Set<T> deserialiseNodesByType(String typeName, Class<T> type) throws DbObjectMarshallerException {
    // FIXME reimplement this
    return null;
//    Set<T> nodes = new HashSet<>();
//    for (EntityKeys<? extends Node> key : nodesByType.get(typeName)) {
//      BasicDBObject doc = keyToNode.get(key);
//      nodes.add(sourceGraph.getMarshaller().deserialize(doc, type));
//    }
//    return nodes;
  }

  public <T> Set<T> deserialiseEdgesByType(String typeName, Class<T> type) throws DbObjectMarshallerException {
    // FIXME reimplement this
    return null;
//    Set<T> edges = new HashSet<>();
//    for (EntityKeys<? extends Edge> key : edgesByType.get(typeName)) {
//      BasicDBObject doc = keyToEdge.get(key);
//      edges.add(sourceGraph.getMarshaller().deserialize(doc, type));
//    }
//    return edges;
  }

  public Set<EntityKeys<? extends Edge>> findEdgesByTypeFromNode(String edgeTypeName, EntityKeys<? extends Node> node)
      throws DbObjectMarshallerException {
    Set<EntityKeys<? extends Edge>> allEdges = edgesFromNode.get(node);
    Set<EntityKeys<? extends Edge>> matchingEdges = new HashSet<>();

    for (EntityKeys<? extends Edge> edge : allEdges) {
      if (edge.getType().equals(edgeTypeName)) {
        matchingEdges.add(edge);
      }
    }
    return matchingEdges;
  }

  public Set<EntityKeys<? extends Edge>> findEdgesByTypeToNode(String edgeTypeName, EntityKeys<? extends Node> node)
      throws DbObjectMarshallerException {
    Set<EntityKeys<? extends Edge>> allEdges = edgesToNode.get(node);
    Set<EntityKeys<? extends Edge>> matchingEdges = new HashSet<>();

    for (EntityKeys<? extends Edge> edge : allEdges) {
      if (edge.getType().equals(edgeTypeName)) {
        matchingEdges.add(edge);
      }
    }
    return matchingEdges;
  }

  public EntityKeys<? extends Edge> findFirstEdgeByTypeFromNode(String edgeTypeName, EntityKeys<? extends Node> node)
      throws DbObjectMarshallerException {
    return findEdgesByTypeFromNode(edgeTypeName, node).iterator().next();
  }
  public EntityKeys<? extends Edge> findFirstEdgeByTypeToNode(String edgeTypeName, EntityKeys<? extends Node> node)
      throws DbObjectMarshallerException {
    return findEdgesByTypeToNode(edgeTypeName, node).iterator().next();
  }
}
