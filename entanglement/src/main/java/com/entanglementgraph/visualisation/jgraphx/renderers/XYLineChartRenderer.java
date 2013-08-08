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

import com.entanglementgraph.specialistnodes.PieChartNode;
import com.entanglementgraph.specialistnodes.XYChartNode;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.view.mxCellState;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.Rotation;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A renderer that can be used to draw line charts for <code>XYChartNode</code> instances.
 *
 * @author Keith Flanagan
 */
public class XYLineChartRenderer implements CustomCellRenderer<XYChartNode> {
  private static final Logger logger = Logger.getLogger(XYLineChartRenderer.class.getSimpleName());

  private Map<String, Object> rendererProperties;
  private ChartPanel chartPanel;

  public XYLineChartRenderer() {
  }

  public void setRendererProperties(Map<String, Object> rendererProperties) {
    this.rendererProperties = rendererProperties;
  }


  @Override
  public void renderCellContentToCanvas(mxCellState state, XYChartNode value,
                                        mxGraphComponent parentGraphComponent, mxInteractiveCanvas canvas) {
    Graphics2D g = canvas.getGraphics();
    Point translate = canvas.getTranslate();
    CellRendererPane rendererPane = canvas.getRendererPane();

    JLabel backgroundLabel = new JLabel(value.getChartTitle());
    JComponent mainComponent = renderCellContent(value);

    backgroundLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    backgroundLabel.setHorizontalAlignment(JLabel.CENTER);
    backgroundLabel.setVerticalAlignment(JLabel.TOP);
    backgroundLabel.setBackground(parentGraphComponent.getBackground().darker());
    backgroundLabel.setOpaque(true);

//    System.out.println("StateX: "+state.getX()+", StateY: "+state.getY()+"; translation: "+translate.getX()+", "+translate.getY()
//        + "; dimensions: "+state.getWidth()+", "+state.getHeight());

//    System.out.println("Components: "+centralContent.getComponents().length);

    rendererPane.paintComponent(g, backgroundLabel, parentGraphComponent,
        (int) state.getX() + translate.x, (int) state.getY() + translate.y,
        (int) state.getWidth(), (int) state.getHeight(), true);

    rendererPane.paintComponent(g, mainComponent, parentGraphComponent,
        (int) state.getX() + translate.x, (int) (state.getY() + translate.y + backgroundLabel.getPreferredSize().getHeight()),
        (int) state.getWidth(), (int) (state.getHeight() - backgroundLabel.getPreferredSize().getHeight()), true);
  }

  @Override
  public JComponent renderCellContent(XYChartNode value) {
    if (chartPanel != null) {
      return chartPanel;
    }

    JFreeChart chart = createChart(value);
    chartPanel = new ChartPanel(chart);
    // default size
//  chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

    return chartPanel;
  }

  public JFreeChart createChart(XYChartNode value) {
    logger.info("Creating chart: ");
    JFreeChart chart = ChartFactory.createXYLineChart(
        value.getChartTitle(),  // chart title
        value.getAxisTitleX(),
        value.getAxisTitleY(),
        value.getDataset(),     // data
        PlotOrientation.HORIZONTAL,
        true,                   // include legend
        true,                   // include tooltips
        false);                 // include URLs
    return chart;
  }
}
