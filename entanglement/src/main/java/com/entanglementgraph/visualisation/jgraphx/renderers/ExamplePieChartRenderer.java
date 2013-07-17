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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.Rotation;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 01/07/13
 * Time: 14:36
 * To change this template use File | Settings | File Templates.
 */
public class ExamplePieChartRenderer implements CustomCellRenderer {

  public ExamplePieChartRenderer() {
  }

  @Override
  public JComponent renderCellContent(Object value) {
    PieDataset dataset = createDataset();
    // based on the dataset we create the chart
    JFreeChart chart = createChart(dataset, "Title... " + value);
    // we put the chart into a panel
    ChartPanel chartPanel = new ChartPanel(chart);
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
