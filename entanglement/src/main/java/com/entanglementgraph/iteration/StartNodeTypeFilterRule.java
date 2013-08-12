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
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.mongodb.BasicDBObject;

import java.util.List;

/**
 * A rule that can be used in cases where a subgraph should only be built if the cursor start position is located
 * on a node of a particular type. If the initial cursor node is of the specified type, then it is added.
 * If the initial cursor node is not of the specified type, then this rule causes the termination of the iteration.
 *
 * @author Keith Flanagan
 */
public class StartNodeTypeFilterRule extends AbstractRule {
  private final String startTypeName;

  private boolean rootNodeWasValidStartType;

  public StartNodeTypeFilterRule(String startTypeName) {
    this.startTypeName = startTypeName;
  }

  @Override
  public List<GraphOperation> iterationStarted(String cursorName, EntityKeys<? extends Node> currentPosition) throws RuleException {
    rootNodeWasValidStartType = currentPosition.getType().equals(startTypeName);
    List<GraphOperation> ops = super.iterationStarted(cursorName, currentPosition);
    if (!rootNodeWasValidStartType) {
      return ops;
    }

    try {
      BasicDBObject currentNode = sourceGraph.getNodeDao().getByKey(currentPosition);
      ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, currentNode));
    } catch (Exception e) {
      throw new RuleException("Failed to retrieve start node content", e);
    }

    return ops;
  }

  @Override
  public boolean ruleMatches(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
    return !rootNodeWasValidStartType;
  }

  @Override
  public HandlerAction apply(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode) throws RuleException {
    return new HandlerAction(NextEdgeIteration.TERMINATE);
  }
}
