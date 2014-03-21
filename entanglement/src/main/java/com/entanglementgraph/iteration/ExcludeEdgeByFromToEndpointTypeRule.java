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

/**
 * A rule that causes graph iterations to stop when an edge is encountered that starts and ends at nodes of
 * specified types.
 * This rule allows you to exclude specific edges.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class ExcludeEdgeByFromToEndpointTypeRule extends AbstractRule {
  private String fromNodeTargetType;
  private String toNodeTargetType;

  public ExcludeEdgeByFromToEndpointTypeRule(String fromNodeTargetType, String toNodeTargetType) {
    this.fromNodeTargetType = fromNodeTargetType;
    this.toNodeTargetType = toNodeTargetType;
  }

//  @Override
//  public boolean ruleMatches(String cursorName, int currentDepth,
//                             EntityKeys<? extends Node> currentPosition,
//                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
//                             EntityKeys<? extends Node> remoteNodeId,
//                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException  {
//    //TODO reimplement
//    return false;
////    try {
////      Edge edge = sourceGraph.getMarshaller().deserialize(rawEdge, Edge.class);
////      return edge.getFrom().getType().equals(fromNodeTargetType)
////          && edge.getTo().getType().equals(toNodeTargetType);
////    } catch (Exception e) {
////      throw new RuleException("Failed to process rule", e);
////    }
//  }
//
//  @Override
//  public HandlerAction apply(String cursorName, int currentDepth,
//                             EntityKeys<? extends Node> currentPosition,
//                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
//                             EntityKeys<? extends Node> remoteNodeId,
//                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
//    HandlerAction action = new HandlerAction(NextEdgeIteration.TERMINATE_BRANCH);
//
////    if (includeNodeInDestination) {
////      action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, rawRemoteNode));
////      action.getOperations().add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, rawEdge));
////    }
//
//    return action;
//  }
}
