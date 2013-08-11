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
package com.entanglementgraph.visualisation.jung.renderers;

import com.entanglementgraph.specialistnodes.XYChartNode;
import com.entanglementgraph.visualisation.jung.CustomRendererException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.logging.Logger;

/**
 * User: keith
 * Date: 09/08/13; 12:04
 *
 * @author Keith Flanagan
 */
public class XYDatasetJFreeChartFactory {
  private static final Logger logger = Logger.getLogger(XYDatasetJFreeChartFactory.class.getName());

  public static JFreeChart createSuggestedChart(XYChartNode node) throws CustomRendererException{
    if (node.getSuggestedType() == null) {
      return createScatterPlot(node);
    }
    switch (node.getSuggestedType()) {
      case TIMESERIES:
        return createTimeSeriesChart(node);
      case POLAR:
        return createPolarChart(node);
      case SCATTERPLOT:
        return createScatterPlot(node);
      case XY_AREA:
        return createXYAreaChart(node);
      case XY_STEP_AREA:
        return createXYStepAreaChart(node);
      case XY_STEP:
        return createXYStepChart(node);
      default:
        throw new CustomRendererException(
            String.format("Chart type %s is either not currently implemented " +
                "or not supported for XY dataset nodes.", node.getSuggestedType()));
    }
  }

//  private static void setCategoryLabelPositions(XYChartNode node, JFreeChart chart) {
//    CategoryPlot plot = (CategoryPlot)chart.getPlot();
//    CategoryAxis xAxis = (CategoryAxis)plot.getDomainAxis();
//    switch (node.getLabelPosition()) {
//      case DOWN_45:
//        xAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
//        break;
//      case DOWN_90:
//        xAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
//        break;
//      case STANDARD:
//        xAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
//        break;
//      case UP_45:
//        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
//        break;
//      case UP_90:
//        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
//        break;
//      default:
//        logger.info("Unrecognised label position: "+node.getLabelPosition());
//    }
//  }

  public static JFreeChart createTimeSeriesChart(XYChartNode node) {
//    XYDataset jfreeDataset = new DefaultXYDataset();
    TimeSeriesCollection jfreeDataset = new TimeSeriesCollection();
    node.getDataset().convertToTimeSeries(jfreeDataset);

    JFreeChart chart = ChartFactory.createTimeSeriesChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Time axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }

  public static JFreeChart createPolarChart(XYChartNode node) {
    XYSeriesCollection jfreeDataset = new XYSeriesCollection();
    node.getDataset().convertToXYDataset(jfreeDataset);

    JFreeChart chart = ChartFactory.createPolarChart(
        node.getChartTitle(),   // chart title
        jfreeDataset,           // data
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }


  public static JFreeChart createScatterPlot(XYChartNode node) {
    XYSeriesCollection jfreeDataset = new XYSeriesCollection();
    node.getDataset().convertToXYDataset(jfreeDataset);

    JFreeChart chart = ChartFactory.createScatterPlot(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }

  public static JFreeChart createXYAreaChart(XYChartNode node) {
    XYSeriesCollection jfreeDataset = new XYSeriesCollection();
    node.getDataset().convertToXYDataset(jfreeDataset);

    JFreeChart chart = ChartFactory.createXYAreaChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }

  public static JFreeChart createXYLineChart(XYChartNode node) {
    XYSeriesCollection jfreeDataset = new XYSeriesCollection();
    node.getDataset().convertToXYDataset(jfreeDataset);

    JFreeChart chart = ChartFactory.createXYLineChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }

  public static JFreeChart createXYStepAreaChart(XYChartNode node) {
    XYSeriesCollection jfreeDataset = new XYSeriesCollection();
    node.getDataset().convertToXYDataset(jfreeDataset);

    JFreeChart chart = ChartFactory.createXYStepAreaChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }

  public static JFreeChart createXYStepChart(XYChartNode node) {
//    DefaultXYDataset jfreeDataset = new DefaultXYDataset();
    XYSeriesCollection jfreeDataset = new XYSeriesCollection();
    node.getDataset().convertToXYDataset(jfreeDataset);

    JFreeChart chart = ChartFactory.createXYStepChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    return chart;
  }


}
