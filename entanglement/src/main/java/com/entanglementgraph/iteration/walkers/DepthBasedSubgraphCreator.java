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
import com.entanglementgraph.iteration.*;
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalker;
import com.entanglementgraph.iteration.walkers.DefaultCursorBasedGraphWalker;
import com.entanglementgraph.iteration.walkers.GraphWalkerException;
import com.entanglementgraph.util.GraphConnection;

/**
 * A <code>CursorBasedGraphWalker</code> implementation that iterates from the specified start location of a source
 * graph and produces a target graph cenetered around this start point at N levels deep.
 * Useful for creating summaries of a graph cursor's immediate surroundings.
 *
 * @author Keith Flanagan
 */
public class DepthBasedSubgraphCreator extends DefaultCursorBasedGraphWalker {
  private static final int DEFAULT_DEPTH = 1;

  private int targetDepth;

  public DepthBasedSubgraphCreator()  {
    this.targetDepth = DEFAULT_DEPTH;
  }

  public DepthBasedSubgraphCreator(int targetDepth) {
    this.targetDepth = targetDepth;
  }

  @Override
  public void initialise() throws GraphWalkerException {
    super.initialise();

    graphIterator.addRule(new StopAfterDepthRule(targetDepth));
  }

  public int getTargetDepth() {
    return targetDepth;
  }

  public void setTargetDepth(int targetDepth) {
    this.targetDepth = targetDepth;
  }
}
