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
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.util.GraphConnection;

import java.util.LinkedList;
import java.util.List;

/**
 * User: keith
 * Date: 30/07/13; 14:25
 *
 * @author Keith Flanagan
 */
abstract public class AbstractRule implements EntityRule {
  protected GraphConnection sourceGraph;
  protected GraphConnection destinationGraph;
  protected EntanglementRuntime entanglementRuntime;
//  protected GraphCursor.CursorContext cursorContext;

//  @Override
//  public void setSourceGraph(GraphConnection sourceGraph) {
//    this.sourceGraph = sourceGraph;
//  }
//
//  @Override
//  public void setDestinationGraph(GraphConnection destinationGraph) {
//    this.destinationGraph = destinationGraph;
//  }
//
//  @Override
//  public void setEntanglementRuntime(EntanglementRuntime entanglementRuntime) {
//    this.entanglementRuntime = entanglementRuntime;
//  }
//
////  @Override
////  public void setCursorContext(GraphCursor.CursorContext cursorContext) {
////    this.cursorContext = cursorContext;
////  }
//
//  @Override
//  public List<GraphOperation> iterationStarted(String cursorName, EntityKeys<? extends Node> currentPosition)
//      throws RuleException {
//    return new LinkedList<>();
//  }
//
//  @Override
//  public HandlerAction preEdgeIteration(String cursorName, int currentEdgeDepth,
//                                        EntityKeys<? extends Node> currentPosition) throws RuleException {
//    // The default action is to perform no action, and not to affect other rules.
//    HandlerAction defaultAction = new HandlerAction();
//    defaultAction.setProcessFurtherRules(true);
//    return defaultAction;
//  }
//
//  @Override
//  public List<GraphOperation> iterationFinished(String cursorName)
//      throws RuleException {
//    return new LinkedList<>();
//  }

}
