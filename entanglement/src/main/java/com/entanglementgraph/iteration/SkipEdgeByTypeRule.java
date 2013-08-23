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

import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.mongodb.BasicDBObject;

import java.util.logging.Logger;

/**
 * A rule that you to exclude edges of a specified type, but has no impact on graph iteration. We're not stopping
 * at a given type here, but excluding it from the target graph, and continuing the graph iteration as if nothing had
 * happened.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class SkipEdgeByTypeRule extends AbstractRule {
  private static final Logger logger = Logger.getLogger(SkipEdgeByTypeRule.class.getName());

  private String targetEdgeType;

  public SkipEdgeByTypeRule(String targetEdgeType) {
    this.targetEdgeType = targetEdgeType;
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
    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
    action.setProcessFurtherRules(false);
    logger.info("Excluding edge based on type: "+targetEdgeType);
    return action;
  }
}
