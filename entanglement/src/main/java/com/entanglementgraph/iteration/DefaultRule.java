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
 * A rule that simply stores the edges and nodes unchanged in the destination graph.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class DefaultRule implements EntityRule {

  private GraphConnection sourceGraph;
  private GraphConnection destinationGraph;

  @Override
  public void setSourceGraph(GraphConnection sourceGraph) {
    this.sourceGraph = sourceGraph;
  }

  @Override
  public void setDestinationGraph(GraphConnection destinationGraph) {
    this.destinationGraph = destinationGraph;
  }

  @Override
  public boolean ruleMatches(GraphCursor currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
                             boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId) {
    return true;
  }

  @Override
  public HandlerAction apply(GraphCursor currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
                             boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId) {
    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
    BasicDBObject remoteNode = outgoingEdge ? nenTuple.getRawDestinationNode() : nenTuple.getRawSourceNode();

    action.getOperations().add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, remoteNode));
    action.getOperations().add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, nenTuple.getRawEdge()));
    return action;
  }
}
