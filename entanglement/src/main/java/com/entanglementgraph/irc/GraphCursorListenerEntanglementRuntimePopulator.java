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
package com.entanglementgraph.irc;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.cursor.GraphCursorListener;

/**
 * A <code>GraphCursorImplementation</code> that (re)populates an internal data structure within an
 * <code>EntanglementRuntime</code> instance whenever a graph cursor is created or modified.
 *
 * User: keith
 * Date: 18/07/13; 14:21
 *
 * @author Keith Flanagan
 */
public class GraphCursorListenerEntanglementRuntimePopulator implements GraphCursorListener {

  private final EntanglementRuntime runtime;

  public GraphCursorListenerEntanglementRuntimePopulator(EntanglementRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void notifyNewGraphCursor(GraphCursor cursor) {
//    runtime.getGraphCursors().put(cursor.getName(), cursor);
  }

  @Override
  public void notifyGraphCursorMoved(GraphCursor previous, GraphCursor current) {
    /*
     * All we need to do here is add the graph cursor to the registry. Hazelcast events will take care of the rest.
     */
    runtime.addGraphCursor(current);
  }
}
