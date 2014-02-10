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
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.commands.NodeModification;
import com.mongodb.BasicDBObject;

import java.util.logging.Logger;

/**
 * A rule that you to exclude edges of a specified type, but has no impact on graph iteration. We're not stopping
 * at a given type here, but excluding it from the target graph, and continuing the graph iteration as if nothing had
 * happened.
 *
 * Even though the edge is skipped, you can optionally specify whether one or both of the nodes are added instead.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class SkipEdgeByTypeRule extends AbstractRule {
  private static final Logger logger = Logger.getLogger(SkipEdgeByTypeRule.class.getName());

  public static enum Option {
    INCLUDE_FROM_NODE,
    INCLUDE_TO_NODE,
    INCLUDE_LOCAL_NODE,
    INCLUDE_REMOTE_NODE;
  }

  private String targetEdgeType;
  private Option[] options;

  public SkipEdgeByTypeRule(String targetEdgeType, Option... options) {
    this.targetEdgeType = targetEdgeType;
    this.options = options;
  }

  @Override
  public boolean ruleMatches(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException  {
    return edgeId.getType().equals(targetEdgeType);
  }

  @Override
  public HandlerAction apply(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
    //TODO reimplement
    return null;
//    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
//    action.setProcessFurtherRules(false);
//    logger.info("Excluding edge based on type: "+targetEdgeType);
//
//
//    BasicDBObject fromNode = outgoingEdge ? rawLocalNode : rawRemoteNode;
//    BasicDBObject toNode = !outgoingEdge ? rawLocalNode : rawRemoteNode;
//    for (Option option : options) {
//      switch (option) {
//        case INCLUDE_FROM_NODE:
//          action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__OVERWRITE_EXSITING, fromNode));
//          break;
//        case INCLUDE_TO_NODE:
//          action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__OVERWRITE_EXSITING, toNode));
//          break;
//        case INCLUDE_LOCAL_NODE:
//          action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__OVERWRITE_EXSITING, rawLocalNode));
//          break;
//        case INCLUDE_REMOTE_NODE:
//          action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__OVERWRITE_EXSITING, rawRemoteNode));
//          break;
//        default:
//          throw new RuleException("Unsupported option type: "+option);
//      }
//    }
//
//    return action;
  }
}
