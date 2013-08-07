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

import org.jfree.data.category.DefaultCategoryDataset;

/**
 * A node that represents a basic category dataset for a chart.
 *
 * @author Keith Flanagan
 */
public class CategoryChartNode extends AbstractChartNode {

  private static final String CV_NAME = CategoryChartNode.class.getSimpleName();
  public static String getTypeName() {
    return CV_NAME;
  }

  private DefaultCategoryDataset dataset;

  public CategoryChartNode() {
    keys.setType(getTypeName());
  }

  public DefaultCategoryDataset getDataset() {
    return dataset;
  }

  public void setDataset(DefaultCategoryDataset dataset) {
    this.dataset = dataset;
  }
}
