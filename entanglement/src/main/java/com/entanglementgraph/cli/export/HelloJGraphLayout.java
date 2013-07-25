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

package com.entanglementgraph.cli.export;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 24/06/2013
 * Time: 14:29
 * To change this template use File | Settings | File Templates.
 */
public class HelloJGraphLayout extends JFrame{
  /**
   *
   */
  private static final long serialVersionUID = -2707712944901661771L;

  public HelloJGraphLayout()
  {
    super("Hello, World!");

    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();



    graph.getModel().beginUpdate();
    try
    {
      Object v1 = graph.insertVertex(parent, null, "Hello", 20, 20, 80, 30);
      Object v2 = graph.insertVertex(parent, null, "World!", 240, 150,80, 30);
      graph.insertEdge(parent, null, "Edge", v1, v2);
    }
    finally
    {
      graph.getModel().endUpdate();
    }

    // Define layout
    mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);

    //Layout with single layout
    // set some properties
    layout.setForceConstant( 40); // the higher, the more separated
    layout.setDisableEdgeStyle( false); // true transforms the edges and makes them direct lines

    // layout graph
    layout.execute(graph.getDefaultParent());

    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    getContentPane().add(graphComponent);
  }

  public static void main(String[] args)
  {
    HelloJGraphLayout frame = new HelloJGraphLayout();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(400, 320);
    frame.setVisible(true);
  }
}
