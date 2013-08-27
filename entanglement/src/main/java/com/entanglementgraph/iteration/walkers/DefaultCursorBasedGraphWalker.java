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

package com.entanglementgraph.iteration.walkers;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.iteration.DepthFirstGraphIterator;
import com.entanglementgraph.iteration.GraphIteratorException;
import com.entanglementgraph.util.GraphConnection;

/**
 * An <code>CursorBasedGraphWalker</code> that contains functionality common to most graph walker
 * implementations. This implementation is a fully functional <code>CursorBasedGraphWalker</code>. No custom rules
 * have been added, meaning that if run over a graph, the result will be an identical clone.
 *
 * You should extend this class and add your own custom iteration rules in order to customise the resulting
 * destination graph. See <code>DepthBasedSubgraphCreator</code> as an example of how to write a simple custom
 * graph walker.
 *
 * @author Keith Flanagan
 */
public class DefaultCursorBasedGraphWalker implements CursorBasedGraphWalker {
  protected EntanglementRuntime runtime;
  protected GraphCursor.CursorContext cursorContext;
  protected DepthFirstGraphIterator graphIterator;
  protected GraphConnection sourceGraph;
  protected GraphConnection destinationGraph;
  protected GraphCursor startPosition;

  @Override
  public void setRuntime(EntanglementRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void setCursorContext(GraphCursor.CursorContext cursorContext) {
    this.cursorContext = cursorContext;
  }

  @Override
  public void setSourceGraph(GraphConnection sourceGraph) {
    this.sourceGraph = sourceGraph;
  }

  @Override
  public void setDestinationGraph(GraphConnection destinationGraph) {
    this.destinationGraph = destinationGraph;
  }

  @Override
  public void setStartPosition(GraphCursor startPosition) {
    this.startPosition = startPosition;
  }

  @Override
  public void initialise() throws GraphWalkerException {
    graphIterator = new DepthFirstGraphIterator(sourceGraph, destinationGraph, runtime, cursorContext, true);
  }

  public void execute() throws GraphWalkerException {
    try {
      graphIterator.execute(startPosition);
    } catch (GraphIteratorException e) {
      throw new GraphWalkerException("Failed to execute graph walker", e);
    }
  }

}
