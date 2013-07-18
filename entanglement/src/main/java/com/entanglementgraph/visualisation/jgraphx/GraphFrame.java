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

import com.mxgraph.swing.handler.mxRubberband;

import javax.swing.*;

/**
 * A JFrame that can be used to visualise JGraphX graphs with custom cell renderers
 *
 * User: keith
 * Date: 17/07/13; 15:00
 *
 * @author Keith Flanagan
 */
public class GraphFrame {
  private JFrame frame;
  private EntanglementMXGraphComponent graphComponent;

  public GraphFrame(String title, EntanglementMxGraph graph) {
    graphComponent = new EntanglementMXGraphComponent(graph);

    // Adds rubberband selection
    new mxRubberband(graphComponent);


    frame = new JFrame(title);
    frame.getContentPane().add(graphComponent);
    frame.setSize(400, 320);
    frame.setVisible(true);
  }

  public JFrame getFrame() {
    return frame;
  }
}
