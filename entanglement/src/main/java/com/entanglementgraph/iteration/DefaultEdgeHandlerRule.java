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
import com.entanglementgraph.revlog.commands.EdgeModification;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;

/**
 * An edge handler that simply stores the destination node as-is.
 *
 * User: keith
 * Date: 25/07/13; 16:06
 *
 * @author Keith Flanagan
 */
public class DefaultEdgeHandlerRule implements EntityHandlerRule {

  @Override
  public boolean canHandleNodeEdgePair(GraphConnection sourceGraph, GraphConnection destinationGraph,
                                       GraphCursor currentPosition, BasicDBObject edge, BasicDBObject remoteNode) {
    return true;
  }

  @Override
  public HandlerAction apply(GraphConnection sourceGraph, GraphConnection destinationGraph,
                             GraphCursor currentPosition, BasicDBObject edge, BasicDBObject remoteNode) {
    HandlerAction action = new HandlerAction(NextEdgeIteration.CONTINUE_AS_NORMAL);
    action.getOperations().add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, edge));
    return action;
  }
}
