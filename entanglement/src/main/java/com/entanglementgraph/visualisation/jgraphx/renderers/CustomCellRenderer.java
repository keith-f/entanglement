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
package com.entanglementgraph.visualisation.jgraphx.renderers;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.view.mxCellState;

import javax.swing.*;

/**
 * Implementations of this class take some data bean, and return a JComponent that can be rendered as either a
 * node or an edge in a JGraphX visualisation.
 *
 * User: keith
 * Date: 01/07/13
 * Time: 14:10
 * @author Keith Flanagan
 */
public interface CustomCellRenderer<T> {
  /**
   * This method is called by our <code>EntanglementPassThroughCanvas</code> for rendering each from of a particular
   * cell. For this reason, this method should be <i>fast</i>.
   *
   * @param state the 'state' object associated with the JGraphX cell to be rendered. This is obtained from the canvas.
   * @param value the Java data bean (value) of the cell to be rendered.
   * @param parentGraphComponent the main Swing component that contains the graph.
   * @param canvas a reference to the <code>EntanglementPassThroughCanvas</code> used for the <code>parentGraphComponent</code>.
   */
  public void renderCellContentToCanvas(mxCellState state, T value, mxGraphComponent parentGraphComponent, mxInteractiveCanvas canvas);

  /**
   * This method is typically called by <code>renderCellContentToCanvas</code>, but may also be used in isolation
   * as a slightly more standard (i.e. less JGraphX-ey) way to obtain a swing component that represents a particular
   * data bean.
   *
   * Depending on your visualisation requirements, repeated calls to this method may choose to either return a new
   * <code>JComponent</code> for each call, or to return a cached value from a previous call - for example, when
   * <code>value</code> hasn't changed since the last call.
   *
   * @param value the Java data bean (value) of the cell to be rendered.
   * @return a JComponent a Swing component that represents the data bean value.
   */
  public JComponent renderCellContent(T value);
//  public mxInteractiveCanvas renderCellContent(T value);
}
