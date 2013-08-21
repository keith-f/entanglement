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
import com.entanglementgraph.util.EntityKeyElementCache;
import com.entanglementgraph.util.InMemoryEntityKeyElementCache;
import com.mongodb.BasicDBObject;

import java.util.logging.Logger;

/**
 * Keeps track of each node and edge visited. Stops graph iteration if either the current edge or destination node have
 * been seen before.
 *
 * This rule is essential for preventing infinite recursion in cyclic graphs. This rule should normally be added
 * as a the highest of the 'high priority' rules so that it gets executed before any others.
 *
 * The current rule implementation uses an in-memory data structure to store the IDs of edges and nodes as the
 * graph is iterated. Future or alternative implementations may use database storage if iterating massive numbers of
 * nodes/edges is required.
 *
 * @author Keith Flanagan
 */
public class StopIfSeenEdgeOrDestNodeRule extends AbstractRule {
  private static final Logger logger = Logger.getLogger(StopIfSeenEdgeOrDestNodeRule.class.getName());
  private final EntityKeyElementCache seenNodes;
  private final EntityKeyElementCache seenEdges;

  public StopIfSeenEdgeOrDestNodeRule() {
    this.seenNodes = new InMemoryEntityKeyElementCache();
    this.seenEdges = new InMemoryEntityKeyElementCache();
  }

  @Override
  public HandlerAction preEdgeIteration(String cursorName, int currentDepth, EntityKeys<? extends Node> currentPosition) throws RuleException {
    /*
     * Check to see if we've landed on a node that we've visited before.
     */
    if (seenNodes.seenElementOf(currentPosition)) {
      HandlerAction action = new HandlerAction(NextEdgeIteration.TERMINATE_BRANCH);
      return action;
    }

    // This is a new node. Cache it and continue.
    seenNodes.cacheElementsOf(currentPosition);
    return super.preEdgeIteration(cursorName, currentDepth, currentPosition);
  }

  @Override
  public boolean ruleMatches(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException  {
    /*
     * Check to see if we've seen this edge before.
     */
    if (seenEdges.seenElementOf(edgeId)) {
      return true;
    }
    // This is a new edge. Cache it and continue.
    seenEdges.cacheElementsOf(edgeId);
    return false;
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
