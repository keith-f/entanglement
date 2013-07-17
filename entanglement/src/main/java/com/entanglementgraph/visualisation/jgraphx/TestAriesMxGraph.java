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

import com.entanglementgraph.visualisation.jgraphx.renderers.ExamplePieChartRenderer;
import com.mxgraph.swing.handler.mxRubberband;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 01/07/13
 * Time: 14:26
 * To change this template use File | Settings | File Templates.
 */
public class TestAriesMxGraph extends JFrame {

  public static void main(String[] args)
  {
    EntanglementMxGraph graph = new EntanglementMxGraph();
    graph.addCustomRendererForBeanType(Integer.class, ExamplePieChartRenderer.class);


    Object parent = graph.getDefaultParent();

    graph.getModel().beginUpdate();
    try
    {

      Object v1 = graph.insertVertex(parent, null, "Hello", 20, 20, 80, 30);
      Object v2 = graph.insertVertex(parent, null, "World!", 240, 150, 80, 30);
      graph.insertEdge(parent, null, "Edge1", v1, v2);

      Object v3 = graph.insertVertex(parent, null, new Integer(45), 20, 180, 80, 80);
      graph.insertEdge(parent, null, "Edge2", v2, v3);
    }
    finally
    {
      graph.getModel().endUpdate();
    }

    EntanglementMXGraphComponent graphComponent = new EntanglementMXGraphComponent(graph);

//    mxGraphComponent graphComponent = new mxGraphComponent(graph)
//    {
//      private static final long serialVersionUID = 4683716829748931448L;
//
//      public mxInteractiveCanvas createCanvas()
//      {
//        return new EntanglementPassThroughCanvas(this);
//      }
//    };



    // Adds rubberband selection
    new mxRubberband(graphComponent);


    TestAriesMxGraph frame = new TestAriesMxGraph();
    frame.getContentPane().add(graphComponent);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(400, 320);
    frame.setVisible(true);
  }


}
