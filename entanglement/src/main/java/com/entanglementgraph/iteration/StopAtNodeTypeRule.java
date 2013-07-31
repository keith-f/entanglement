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
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;

/**
 * A rule that causes graph iterations to stop when a particular node type is encountered.
 * The rule can be configured to include or exclude the target node type in the destination graph, as desired.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class StopAtNodeTypeRule extends AbstractRule {
  private String nodeType;
  private boolean includeNodeInDestination;

  public StopAtNodeTypeRule(String nodeType, boolean includeNodeInDestination) {
    this.nodeType = nodeType;
    this.includeNodeInDestination = includeNodeInDestination;
  }

  @Override
  public boolean ruleMatches(String cursorName, EntityKeys<? extends Node> currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
                             boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId) throws RuleException  {
    return nodeId.getType().equals(nodeType);
  }

  @Override
  public HandlerAction apply(String cursorName, EntityKeys<? extends Node> currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
                             boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId) throws RuleException {
    HandlerAction action = new HandlerAction(NextEdgeIteration.TERMINATE_BRANCH);
    BasicDBObject remoteNode = outgoingEdge ? nenTuple.getRawDestinationNode() : nenTuple.getRawSourceNode();

    if (includeNodeInDestination) {
      action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, remoteNode));
      action.getOperations().add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, nenTuple.getRawEdge()));
    }

    return action;
  }
}
