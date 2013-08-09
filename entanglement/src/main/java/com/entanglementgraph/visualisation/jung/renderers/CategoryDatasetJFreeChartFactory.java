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

import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.visualisation.jung.CustomRendererException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.logging.Logger;

/**
 * User: keith
 * Date: 09/08/13; 12:04
 *
 * @author Keith Flanagan
 */
public class CategoryDatasetJFreeChartFactory {
  private static final Logger logger = Logger.getLogger(CategoryDatasetJFreeChartFactory.class.getName());

  public static JFreeChart createSuggestedChart(CategoryChartNode node) throws CustomRendererException{
    if (node.getSuggestedType() == null) {
      return createLineChart(node);
    }
    switch (node.getSuggestedType()) {
      case AREA:
        return createAreaChart(node);
      case BAR:
        return createBarChart(node);
      case LINE:
        return createLineChart(node);
      case STACKED_AREA:
        return createStackedAreaChart(node);
      case STACKED_BAR:
        return createStackedBarChart(node);
      default:
        throw new CustomRendererException(
            String.format("Chart type %s is either not currently implemented " +
                "or not supported for category dataset nodes.", node.getSuggestedType()));
    }
  }

  private static void setCategoryLabelPositions(CategoryChartNode node, JFreeChart chart) {
    CategoryPlot plot = (CategoryPlot)chart.getPlot();
    CategoryAxis xAxis = (CategoryAxis)plot.getDomainAxis();
    switch (node.getLabelPosition()) {
      case DOWN_45:
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
        break;
      case DOWN_90:
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
        break;
      case STANDARD:
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
        break;
      case UP_45:
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        break;
      case UP_90:
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        break;
      default:
        logger.info("Unrecognised label position: "+node.getLabelPosition());
    }

  }

  public static JFreeChart createAreaChart(CategoryChartNode node) {
    DefaultCategoryDataset jfreeDataset = new DefaultCategoryDataset();
    node.getDataset().convertToJFreeChart(jfreeDataset);

    JFreeChart chart = ChartFactory.createAreaChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    setCategoryLabelPositions(node, chart);
    return chart;
  }

  public static JFreeChart createBarChart(CategoryChartNode node) {
    DefaultCategoryDataset jfreeDataset = new DefaultCategoryDataset();
    node.getDataset().convertToJFreeChart(jfreeDataset);

    JFreeChart chart = ChartFactory.createBarChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    setCategoryLabelPositions(node, chart);
    return chart;
  }


  public static JFreeChart createLineChart(CategoryChartNode node) {
    DefaultCategoryDataset jfreeDataset = new DefaultCategoryDataset();
    node.getDataset().convertToJFreeChart(jfreeDataset);

    JFreeChart chart = ChartFactory.createLineChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    setCategoryLabelPositions(node, chart);
    return chart;
  }

  public static JFreeChart createStackedAreaChart(CategoryChartNode node) {
    DefaultCategoryDataset jfreeDataset = new DefaultCategoryDataset();
    node.getDataset().convertToJFreeChart(jfreeDataset);

    JFreeChart chart = ChartFactory.createStackedAreaChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    setCategoryLabelPositions(node, chart);
    return chart;
  }

  public static JFreeChart createStackedBarChart(CategoryChartNode node) {
    DefaultCategoryDataset jfreeDataset = new DefaultCategoryDataset();
    node.getDataset().convertToJFreeChart(jfreeDataset);

    JFreeChart chart = ChartFactory.createStackedBarChart(
        node.getChartTitle(),   // chart title
        node.getAxisTitleX(),   // Category axis
        node.getAxisTitleY(),   // Value axis
        jfreeDataset,           // data
        PlotOrientation.VERTICAL,
        true,                   // include legend
        true,                   // include tooltips
        false);
    setCategoryLabelPositions(node, chart);
    return chart;
  }


}
