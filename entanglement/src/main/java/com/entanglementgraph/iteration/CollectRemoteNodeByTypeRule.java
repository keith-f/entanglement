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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A rule that collects node objects of a specified type. The nodes are stored in memory for collection once the
 * graph iteration has completed.
 *
 * @author Keith Flanagan
 */
public class CollectRemoteNodeByTypeRule extends AbstractRule {
  private static final Logger logger = Logger.getLogger(CollectRemoteNodeByTypeRule.class.getName());
//  private final String targetType;
//  private final Set<BasicDBObject> collectedObjects;
//
//  public CollectRemoteNodeByTypeRule(String targetType) {
//    this.targetType = targetType;
//    this.collectedObjects = new HashSet<>();
//  }
//
//
//  @Override
//  public boolean ruleMatches(String cursorName, int currentDepth,
//                             EntityKeys<? extends Node> currentPosition,
//                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
//                             EntityKeys<? extends Node> remoteNodeId,
//                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
//      throws RuleException  {
//    return remoteNodeId.getType().equals(targetType);
//  }
//
//  @Override
//  public HandlerAction apply(String cursorName, int currentDepth,
//                             EntityKeys<? extends Node> currentPosition,
//                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
//                             EntityKeys<? extends Node> remoteNodeId,
//                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
//    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
//    action.setProcessFurtherRules(true);
//    collectedObjects.add(rawRemoteNode);
//    return action;
//  }
//
//  public Set<BasicDBObject> getCollectedObjects() {
//    return collectedObjects;
//  }
//
//  public String getTargetType() {
//    return targetType;
//  }
}
