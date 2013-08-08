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

import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * User: keith
 * Date: 01/08/13; 16:38
 *
 * @author Keith Flanagan
 */
public class ChartIcon<V, E> implements Icon {
  private final VisualizationViewer<V, E> vv;
  private final V v;
  private final ImageIcon delegate;

  public ChartIcon(VisualizationViewer<V, E> vv, V v) {
    this.vv = vv;
    this.v = v;
    JFreeChart chart = renderCellContent2(v);
    chart.setBorderVisible(false);
    chart.setPadding(RectangleInsets.ZERO_INSETS);
    BufferedImage objBufferedImage=chart.createBufferedImage(200, 200);
//        ByteArrayOutputStream bas = new ByteArrayOutputStream();
//        try {
//          ImageIO.write(objBufferedImage, "png", bas);
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//        byte[] byteArray=bas.toByteArray();

    delegate = new ImageIcon();
    delegate.setImage(objBufferedImage);
    System.out.println("Icon height: "+delegate.getIconHeight());
  }

  public int getIconHeight() {
    return delegate.getIconHeight();
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int i, int i2) {
    delegate.paintIcon(component, graphics, i, i2);
  }

  public int getIconWidth() {
    return delegate.getIconWidth();
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
