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
package com.entanglementgraph.export;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.iteration.*;
import com.entanglementgraph.util.GraphConnection;

/**
 * Takes an Entanglement graph and a graph cursor to export a cursor-centric subgraph (to a different Entanglement
 * graph) up to the specified depth (steps away from the initial cursor node).
 *
 * @author Keith Flanagan
 */
public class DepthBasedSubgraphCreator {
  private final DepthFirstGraphIterator graphIterator;

  public DepthBasedSubgraphCreator(
      GraphConnection sourceGraph, GraphConnection destinationGraph, EntanglementRuntime runtime,
      GraphCursor.CursorContext cursorContext, int targetDepth) {
    graphIterator = new DepthFirstGraphIterator(sourceGraph, destinationGraph, runtime, cursorContext, true);

    graphIterator.addRule(new StopAfterDepthRule(targetDepth));
  }

  public void execute(GraphCursor startPosition) throws GraphIteratorException {
    graphIterator.execute(startPosition);
  }
}
