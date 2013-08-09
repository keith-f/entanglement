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

package com.entanglementgraph.specialistnodes;

import com.entanglementgraph.graph.data.Node;

/**
 * A node that contains enough information to generate a chart of some kind.
 * Implementations typically use JFreeChart data structures for convenience, but these are generic datastores and
 * could be rendered by different chart libraries if required.
 * @author Keith Flanagan
 */
public class AbstractChartNode extends Node {
  public static enum ChartType {
    AREA,
    BAR,
    BOX_AND_WHISKER,
    BUBBLE,
    CANDLESTICK,
    GANTT,
    HIGH_LOW,
    HISTOGRAM,
    LINE,
    POLAR,
    RING,
    SCATTERPLOT,
    STACKED_AREA,
    STACKED_BAR,
    STACKED_XY_AREA,
    TIMESERIES,
    XY_AREA,
    XY_BAR,
    XY_LINE
  }

  private String chartTitle;
  private String axisTitleX;
  private String axisTitleY;
  private ChartType suggestedType;

  public AbstractChartNode() {
    chartTitle = "Title";
    axisTitleX = "X axis";
    axisTitleY = "Y axis";
  }

  public AbstractChartNode(String chartTitle, String axisTitleX, String axisTitleY, ChartType suggestedType) {
    this.chartTitle = chartTitle;
    this.axisTitleX = axisTitleX;
    this.axisTitleY = axisTitleY;
    this.suggestedType = suggestedType;
  }

  public String getChartTitle() {
    return chartTitle;
  }

  public void setChartTitle(String chartTitle) {
    this.chartTitle = chartTitle;
  }

  public String getAxisTitleX() {
    return axisTitleX;
  }

  public void setAxisTitleX(String axisTitleX) {
    this.axisTitleX = axisTitleX;
  }

  public String getAxisTitleY() {
    return axisTitleY;
  }

  public void setAxisTitleY(String axisTitleY) {
    this.axisTitleY = axisTitleY;
  }

  public ChartType getSuggestedType() {
    return suggestedType;
  }

  public void setSuggestedType(ChartType suggestedType) {
    this.suggestedType = suggestedType;
  }
}
