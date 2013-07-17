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
package com.entanglementgraph.visualisation.jgraphx;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.view.mxGraph;

/**
 * A custom extension of JGraphX project's <code>mxGraphComponent</code> class. All this extension does is to create
 * a custom canvas so that we can override the default JGraphX renderer.
 *
 * User: keith
 * Date: 17/07/13; 16:14
 *
 * @author Keith Flanagan
 */
public class EntanglementMXGraphComponent extends mxGraphComponent {
  private static final long serialVersionUID = 4683716829748931448L;

  public EntanglementMXGraphComponent(mxGraph graph) {
    super(graph);
  }

  @Override
  public mxInteractiveCanvas createCanvas()
  {
    return new EntanglementPassThroughCanvas(this);
  }
}
