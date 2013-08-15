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
import com.entanglementgraph.visualisation.jung.*;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import org.apache.commons.collections15.Transformer;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class CategoryDatasetChartRenderer extends DefaultVertexRenderer {
  private static final Logger logger = Logger.getLogger(CategoryDatasetChartRenderer.class.getName());

  private Transformer<DBObject, Icon> iconTransformer;

  public CategoryDatasetChartRenderer() {
  }

  @Override
  public void setVisualiser(Visualiser visualiser) {
    super.setVisualiser(visualiser);
  }

  @Override
  public Transformer<DBObject, Icon> getVertexIconTransformer() {
    if (iconTransformer == null) {
      iconTransformer = new Transformer<DBObject, Icon>() {
        JFreeChart cachedChart = null;
        ImageIcon cachedIcon = null;
        @Override
        public Icon transform(DBObject dbObject) {
          if (cachedChart == null) {
            CategoryChartNode node = null;
            try {
              node = marshaller.deserialize(dbObject, CategoryChartNode.class);
              cachedChart = CategoryDatasetJFreeChartFactory.createSuggestedChart(node);
              BufferedImage objBufferedImage=cachedChart.createBufferedImage(500, 500);
              cachedIcon = new ImageIcon();
              cachedIcon.setImage(objBufferedImage);
            } catch (DbObjectMarshallerException e) {
              e.printStackTrace();
              return new ErrorIcon();
            } catch (CustomRendererException e) {
              e.printStackTrace();
              return new ErrorIcon();
            }
          }
          return cachedIcon;
        }

      };
    }
    return iconTransformer;
  }

}
