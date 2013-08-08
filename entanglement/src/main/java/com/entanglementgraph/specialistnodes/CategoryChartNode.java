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

import java.util.HashMap;
import java.util.Map;

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

  public static class CategoryDataset {
    private final Map<String, Map<String, Number>> rowKeyToColKeyToValue = new HashMap<>();

    public void addValue(Number value, String rowKey, String colKey) {
      Map<String, Number> colKeyToValue = rowKeyToColKeyToValue.get(rowKey);
      if (colKeyToValue == null) {
        colKeyToValue = new HashMap<>();
        rowKeyToColKeyToValue.put(rowKey, colKeyToValue);
      }
      colKeyToValue.put(colKey, value);
    }

    public void convertToJFreeChart(DefaultCategoryDataset jfreeDataset) {
      for (Map.Entry<String, Map<String, Number>> entry : rowKeyToColKeyToValue.entrySet()) {
        Comparable rowKey = entry.getKey();
        for (Map.Entry<String, Number> colToValue : entry.getValue().entrySet()) {
          jfreeDataset.addValue(colToValue.getValue(), rowKey, colToValue.getKey());
        }
      }
    }
  }

  private CategoryDataset dataset;

  public CategoryChartNode() {
    keys.setType(getTypeName());
    dataset = new CategoryChartNode.CategoryDataset();
  }

  public CategoryChartNode.CategoryDataset getDataset() {
    return dataset;
  }

  public void setDataset(CategoryChartNode.CategoryDataset dataset) {
    this.dataset = dataset;
  }
}
