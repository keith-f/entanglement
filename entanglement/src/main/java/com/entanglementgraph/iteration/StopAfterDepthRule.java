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
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.revlog.commands.EdgeModification;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.mongodb.BasicDBObject;

import java.util.logging.Logger;

/**
 * A rule that causes graph iterations to stop when a particular depth (steps away from the starting node)
 * is reached.
 *
 * @author Keith Flanagan
 */
public class StopAfterDepthRule extends AbstractRule {
  private static final Logger logger = Logger.getLogger(StopAfterDepthRule.class.getName());
  private final int targetDepth;

  public StopAfterDepthRule(int targetDepth) {
    this.targetDepth = targetDepth;
  }

  @Override
  public HandlerAction preEdgeIteration(String cursorName, int currentDepth, EntityKeys<? extends Node> currentPosition) throws RuleException {
    /*
     * Greater than or equal, since if the 'current' depth is already the target depth, then there's no point in
     * iterating any edges
     */
    if (currentDepth >= targetDepth) {
      HandlerAction action = new HandlerAction(NextEdgeIteration.TERMINATE_BRANCH);
      return action;
    }
    return super.preEdgeIteration(cursorName, currentDepth, currentPosition);
  }

  @Override
  public boolean ruleMatches(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException  {
//    logger.info(String.format("Current depth: %d; Target depth: %d; Current position: %s; Remote node: %s\n" +
//        "Edge: %s", currentDepth, targetDepth, currentPosition, remoteNodeId, edgeId));
    return currentDepth >= targetDepth;
  }

  @Override
  public HandlerAction apply(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
    HandlerAction action = new HandlerAction(NextEdgeIteration.TERMINATE_BRANCH);
    return action;
  }
}
