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
import org.jfree.data.xy.XYDataset;

/**
 * A node that represents an X/Y dataset for a chart.
 *
 * @author Keith Flanagan
 */
public class XYChartNode extends AbstractChartNode {

  private static final String CV_NAME = XYChartNode.class.getSimpleName();
  public static String getTypeName() {
    return CV_NAME;
  }

  private XYDataset dataset;

  public XYChartNode() {
    keys.setType(getTypeName());
  }

  public XYDataset getDataset() {
    return dataset;
  }

  public void setDataset(XYDataset dataset) {
    this.dataset = dataset;
  }
}
