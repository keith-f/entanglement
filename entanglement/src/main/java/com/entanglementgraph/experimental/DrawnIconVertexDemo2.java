/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package com.entanglementgraph.experimental;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelRenderer;
import org.apache.commons.collections15.Transformer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.Rotation;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A demo that shows drawn Icons as vertices
 *
 * @author Tom Nelson 
 *
 */
public class DrawnIconVertexDemo2 {

  /**
   * the graph
   */
  Graph<Integer,Number> graph;

  /**
   * the visual component and renderer for the graph
   */
  VisualizationViewer<Integer,Number> vv;

  public DrawnIconVertexDemo2() {

    // create a simple graph for the demo
    graph = new DirectedSparseGraph<Integer,Number>();
    System.out.println("Generating dataset ...");
    Integer[] v = createVertices(100);
    System.out.println("Generation complete!");
    createEdges(v);

    Layout<Integer, Number> layout = new FRLayout<Integer,Number>(graph);
//    layout.setSize(new Dimension(1000, 800));
//    Layout<Integer, Number> layout = new FRLayout2<Integer,Number>(graph);
    vv =  new VisualizationViewer<Integer,Number>(layout);
    //BasicVisualizationServer

    vv.setPreferredSize(new Dimension(350,350)); //Sets the viewing area size
    vv.getRenderContext().setVertexLabelTransformer(new Transformer<Integer,String>(){

      public String transform(Integer v) {
        return String.format("Vertex %d", v);
      }});
    vv.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.cyan));
    vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.cyan));
    vv.setDoubleBuffered(true);



//    vv.getRenderContext().setVertexIconTransformer(new Transformer<Integer,Icon>() {
//
//      /*
//       * Implements the Icon interface to draw an Icon with background color and
//       * a text label
//       */
//      public Icon transform(final Integer v) {
//        return new Icon() {
//
//          public int getIconHeight() {
//            return 20;
//          }
//
//          public int getIconWidth() {
//            return 20;
//          }
//
//          public void paintIcon(Component c, Graphics g,
//                                int x, int y) {
//            if(vv.getPickedVertexState().isPicked(v)) {
//              g.setColor(Color.yellow);
//            } else {
//              g.setColor(Color.red);
//            }
//            g.fillOval(x, y, 20, 20);
//            if(vv.getPickedVertexState().isPicked(v)) {
//              g.setColor(Color.black);
//            } else {
//              g.setColor(Color.white);
//            }
//            g.drawString(""+v, x+6, y+15);
//
//          }};
//      }});


//    vv.getRenderContext().setVertexIconTransformer(new Transformer<Integer,Icon>() {
//
//      /*
//       * Implements the Icon interface to draw an Icon with background color and
//       * a text label
//       */
//      public Icon transform(final Integer v) {
//        JFreeChart chart = renderCellContent2(v);
//        BufferedImage objBufferedImage=chart.createBufferedImage(200, 200);
////        ByteArrayOutputStream bas = new ByteArrayOutputStream();
////        try {
////          ImageIO.write(objBufferedImage, "png", bas);
////        } catch (IOException e) {
////          e.printStackTrace();
////        }
////        byte[] byteArray=bas.toByteArray();
//
//        ImageIcon icon = new ImageIcon();
//        icon.setImage(objBufferedImage);
//        System.out.println("Icon height: "+icon.getIconHeight());
//
//        return icon;
//      }});

    // ******
    vv.getRenderContext().setVertexIconTransformer(new Transformer<Integer,Icon>() {
      public Icon transform(final Integer v) {
//        if (Math.random() < 0.05) {
//          return new ChartIcon<>(vv, v);
//        } else {
         return new TestIcon<>(vv, v);
//        return null;
//      }

      }});

//    vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {
//      @Override
//      public <T> Component getVertexLabelRendererComponent(JComponent jComponent, Object o, Font font, boolean b, T t) {
//        return new JButton("Text: "+b+t);
//      }
//    });

//    vv.getRenderContext().setLabelOffset(0);

//    vv.getRenderContext().setVertexShapeTransformer(new Transformer<Integer, Shape>() {
//      @Override
//      public Shape transform(Integer integer) {
//        Rectangle rect = new Rectangle(200, 200);
//        return rect;
//      }
//    });

    vv.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<Integer>(vv.getPickedVertexState(), Color.white,  Color.yellow));
    vv.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<Number>(vv.getPickedEdgeState(), Color.black, Color.lightGray));

    vv.setBackground(Color.white);

    // add my listener for ToolTips
    vv.setVertexToolTipTransformer(new ToStringLabeller<Integer>());

    // create a frome to hold the graph
    final JFrame frame = new JFrame();
    Container content = frame.getContentPane();
    final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
    content.add(panel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final ModalGraphMouse gm = new DefaultModalGraphMouse<Integer,Number>();
    vv.setGraphMouse(gm);

    final ScalingControl scaler = new CrossoverScalingControl();

    JButton plus = new JButton("+");
    plus.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scaler.scale(vv, 1.1f, vv.getCenter());
      }
    });
    JButton minus = new JButton("-");
    minus.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scaler.scale(vv, 1/1.1f, vv.getCenter());
      }
    });

    JPanel controls = new JPanel();
    controls.add(plus);
    controls.add(minus);
    controls.add(((DefaultModalGraphMouse<Integer,Number>) gm).getModeComboBox());
    content.add(controls, BorderLayout.SOUTH);

    frame.pack();
    frame.setVisible(true);
  }


  /**
   * create some vertices
   * @param count how many to create
   * @return the Vertices in an array
   */
  private Integer[] createVertices(int count) {
    Integer[] v = new Integer[count];
    for (int i = 0; i < count; i++) {
      v[i] = new Integer(i);
      graph.addVertex(v[i]);
    }
    return v;
  }

  /**
   * create edges for this demo graph
   * @param v an array of Vertices to connect
   */
  void createEdges(Integer[] v) {
    for (int i=0; i<v.length; i++) {
      for (int j=0; j<v.length; j++) {
        if (Math.random() < 0.05) {
          graph.addEdge(new Double(Math.random()), v[i], v[j], EdgeType.DIRECTED);
        }
        if (Math.random() < 0.05) {
          break;
        }
      }
    }
//    graph.addEdge(new Double(Math.random()), v[0], v[1], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[0], v[3], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[0], v[4], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[4], v[5], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[3], v[5], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[1], v[2], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[1], v[4], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[8], v[2], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[3], v[8], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[6], v[7], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[7], v[5], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[0], v[9], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[9], v[8], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[7], v[6], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[6], v[5], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[4], v[2], EdgeType.DIRECTED);
//    graph.addEdge(new Double(Math.random()), v[5], v[4], EdgeType.DIRECTED);
  }

  /**
   * a driver for this demo
   */
  public static void main(String[] args)
  {
    new DrawnIconVertexDemo2();
  }



  private JFreeChart chart = null;
  public JFreeChart renderCellContent2(Object value) {
    if (chart != null) {
      return chart;
    }

    PieDataset dataset = createDataset();
    // based on the dataset we create the chart
    chart = createChart(dataset, "Title... " + value);
    return chart;
  }


  private ChartPanel chartPanel = null;
  public JComponent renderCellContent(Object value) {
    if (chartPanel != null) {
      return chartPanel;
    }

    PieDataset dataset = createDataset();
    // based on the dataset we create the chart
    JFreeChart chart = createChart(dataset, "Title... " + value);

    // we put the chart into a panel
    chartPanel = new ChartPanel(chart);
    // default size
//  chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

    return chartPanel;
  }


  private PieDataset createDataset() {
    System.out.println("Create dataset");
    DefaultPieDataset result = new DefaultPieDataset();
    result.setValue("Linux", 29);
    result.setValue("Mac", 20);
    result.setValue("Windows", 51);
    return result;

  }

  public JFreeChart createChart(PieDataset dataset, String title) {

    System.out.println("Create chart");
    JFreeChart chart = ChartFactory.createPieChart3D(title,          // chart title
        dataset,                // data
        true,                   // include legend
        true,
        false);

    PiePlot3D plot = (PiePlot3D) chart.getPlot();
    plot.setStartAngle(290);
    plot.setDirection(Rotation.CLOCKWISE);
    plot.setForegroundAlpha(0.5f);
    return chart;

  }
}
