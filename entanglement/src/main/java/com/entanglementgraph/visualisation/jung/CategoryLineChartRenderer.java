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

package com.entanglementgraph.visualisation.jung;

import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import org.apache.commons.collections15.Transformer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class CategoryLineChartRenderer implements CustomVertexAppearance {
  private static final Logger logger = Logger.getLogger(CategoryLineChartRenderer.class.getName());

  private final DbObjectMarshaller marshaller;

  private Transformer<DBObject, Icon> iconTransformer;

  public CategoryLineChartRenderer(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }

  @Override
  public Transformer<DBObject, Icon> getIconTransformer() {
    if (iconTransformer == null) {
      iconTransformer = new Transformer<DBObject, Icon>() {
        JFreeChart cachedChart = null;
        ImageIcon cachedIcon = null;
        @Override
        public Icon transform(DBObject dbObject) {
          if (cachedChart == null) {
            CategoryChartNode node = marshaller.deserialize(dbObject, CategoryChartNode.class);
            cachedChart = createChart(dbObject);
            BufferedImage objBufferedImage=cachedChart.createBufferedImage(200, 200);
            cachedIcon = new ImageIcon();
            cachedIcon.setImage(objBufferedImage);
          }
          return cachedIcon;
        }

        public JFreeChart createChart(CategoryChartNode value) {
          logger.info("Creating chart for: "+value);
          JFreeChart chart = ChartFactory.createLineChart(
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
      };
    }
    return iconTransformer;
  }

  @Override
  public Transformer<DBObject, Shape> getShapeTransformer() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Transformer<DBObject, String> getVertexLabelTransformer() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Transformer<DBObject, String> getTooltipTransformer() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
