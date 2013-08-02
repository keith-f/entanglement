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
package com.entanglementgraph.experimental;


import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;

import javax.swing.*;
import java.awt.*;

/**
 * User: keith
 * Date: 01/08/13; 14:52
 *
 * @author Keith Flanagan
 */
public class JungTest {
  public static void main(String[] args) {
    // Graph<V, E> where V is the type of the vertices
    // and E is the type of the edges
//    Graph<Integer, String> g = new SparseMultigraph<Integer, String>();
    Graph<Integer, String> g = new DirectedSparseMultigraph<Integer, String>();
    // Add some vertices. From above we defined these to be type Integer.
    g.addVertex((Integer)1);
    g.addVertex((Integer)2);
    g.addVertex((Integer)3);
    // Add some edges. From above we defined these to be of type String
    // Note that the default is for undirected edges.
    g.addEdge("Edge-A", 1, 2); // Note that Java 1.5 auto-boxes primitives
    g.addEdge("Edge-B", 2, 3);
    // Let's see what we have. Note the nice output from the
    // SparseMultigraph<V,E> toString() method
    System.out.println("The graph g = " + g.toString());
    // Note that we can use the same nodes and edges in two different graphs.
    Graph<Integer, String> g2 = new SparseMultigraph<Integer, String>();
    g2.addVertex((Integer)1);
    g2.addVertex((Integer)2);
    g2.addVertex((Integer)3);
    g2.addEdge("Edge-A", 1,3);
    g2.addEdge("Edge-B", 2,3, EdgeType.DIRECTED);
    g2.addEdge("Edge-C", 3, 2, EdgeType.DIRECTED);
    g2.addEdge("Edge-P", 2,3); // A parallel edge
    System.out.println("The graph g2 = " + g2.toString());



    // Visualise graph

//    SimpleGraphView sgv = new SimpleGraphView(); //We create our graph in here
    // The Layout<V, E> is parameterized by the vertex and edge types
    Layout<Integer, String> layout = new CircleLayout(g);
    layout.setSize(new Dimension(300,300)); // sets the initial size of the space
    // The BasicVisualizationServer<V,E> is parameterized by the edge types
    BasicVisualizationServer<Integer,String> vv =
        new BasicVisualizationServer<Integer,String>(layout);
    vv.setPreferredSize(new Dimension(350,350)); //Sets the viewing area size

    JFrame frame = new JFrame("Simple Graph View");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(vv);
    frame.pack();
    frame.setVisible(true);
  }
}
