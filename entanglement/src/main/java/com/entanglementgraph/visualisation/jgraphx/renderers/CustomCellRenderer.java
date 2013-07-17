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

import com.mxgraph.swing.view.mxInteractiveCanvas;

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
  public JComponent renderCellContent(T value);
//  public mxInteractiveCanvas renderCellContent(T value);
}
