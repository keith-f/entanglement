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
 * A rule that simply stores the edges and nodes unchanged in the destination graph.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class AddByDefaultRule extends AbstractRule {

//  @Override
//  public boolean ruleMatches(String cursorName, int currentDepth,
//                             EntityKeys<? extends Node> currentPosition,
//                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
//                             EntityKeys<? extends Node> remoteNodeId,
//                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
//                             throws RuleException {
//    return true;
//  }
//
//  @Override
//  public HandlerAction apply(String cursorName, int currentDepth,
//                             EntityKeys<? extends Node> currentPosition,
//                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
//                             EntityKeys<? extends Node> remoteNodeId,
//                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
//                             throws RuleException {
//    //TODO reimplement
//    return null;
////    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
////    action.setProcessFurtherRules(false);
////
////    action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, rawRemoteNode));
////    action.getOperations().add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, rawEdge));
////    return action;
//  }

}
