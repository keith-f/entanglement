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
import org.jfree.data.time.TimeSeriesDataItem;

/**
 * A node that represents a time series dataset for a chart.
 *
 * @author Keith Flanagan
 */
public class TimeSeriesChartNode extends AbstractChartNode {

  private static final String CV_NAME = TimeSeriesChartNode.class.getSimpleName();
  public static String getTypeName() {
    return CV_NAME;
  }

  private TimeSeriesDataItem dataset;

  public TimeSeriesChartNode() {
    keys.setType(getTypeName());
  }

  public TimeSeriesDataItem getDataset() {
    return dataset;
  }

  public void setDataset(TimeSeriesDataItem dataset) {
    this.dataset = dataset;
  }
}
