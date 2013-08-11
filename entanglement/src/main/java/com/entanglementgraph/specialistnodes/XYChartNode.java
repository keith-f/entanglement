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

import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A node that represents an X/Y dataset for a chart.
 * This node can be used for holding data for 'typical' XY datasets, for JFreeChart 'time series' charts.
 * In that case of time series data, the 'X' values represent milliseconds and the 'Y' values represent the data
 * of the series.
 *
 * @author Keith Flanagan
 */
public class XYChartNode extends AbstractChartNode {

  private static final String CV_NAME = XYChartNode.class.getSimpleName();
  public static String getTypeName() {
    return CV_NAME;
  }

  public static class XYCompatibleDataset {
    /**
     * A map of series name to XY data. The XY chart data must be in the same form as described by the JFreeChart
     * Javadoc:
     * "the data (must be an array with length 2, containing two arrays of equal length, the first containing the
     * x-values and the second containing the y-values)."
     * http://www.jfree.org/jfreechart/api/javadoc/org/jfree/data/xy/DefaultXYDataset.html#addSeries%28java.lang.Comparable,%20double[][]%29
     */
    private final Map<String, double[][]> seriesToXYData = new HashMap<>();

    public void addSeries(String series, double[][] data) {
      seriesToXYData.put(series, data);
    }

    public void convertToXYDataset(DefaultXYDataset jfreeDataset) {
      for (Map.Entry<String, double[][]> seriesEntry : seriesToXYData.entrySet()) {
        XYSeries xySeries = new XYSeries(seriesEntry.getKey());
        double[][] data = seriesEntry.getValue();
        for (int i=0; i<data[0].length; i++) {
          xySeries.add(data[0][i], data[1][i]);
        }
      }
    }

    public void convertToTimeSeries(TimeSeriesCollection jfreeDataset) {
      for (Map.Entry<String, double[][]> seriesEntry : seriesToXYData.entrySet()) {
        TimeSeries timeSeries = new TimeSeries(seriesEntry.getKey());
        double[][] data = seriesEntry.getValue();
        for (int i=0; i<data[0].length; i++) {
          timeSeries.add(new Minute(new Date((long) data[0][i])), data[1][i]);
        }
      }
    }
  }

  private XYCompatibleDataset dataset;

  public XYChartNode() {
    keys.setType(getTypeName());
    this.dataset = new XYCompatibleDataset();
  }

  public XYCompatibleDataset getDataset() {
    return dataset;
  }

  public void setDataset(XYCompatibleDataset dataset) {
    this.dataset = dataset;
  }
}
