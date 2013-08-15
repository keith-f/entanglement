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

import com.mongodb.DBObject;
import edu.uci.ics.jung.visualization.VisualizationViewer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * User: keith
 * Date: 01/08/13; 16:38
 *
 * @author Keith Flanagan
 */
public class DefaultNodeIcon<V extends DBObject, E extends DBObject> implements Icon {
  private static final Logger logger = Logger.getLogger(DefaultNodeIcon.class.getName());

  private static final int DEFAULT_DIMENSION = 20;

  private static final Color DEFAULT_PICKED_COLOUR = Color.YELLOW;
  private static final Color DEFAULT_UNPICKED_COLOUR = Color.GRAY;

  protected final VisualizationViewer<V, E> vv;
  protected final V vertexData;

  private int dimension;
  private Color pickedColour;
  private Color unpickedColour;

  public DefaultNodeIcon(VisualizationViewer<V, E> vv, V vertexData) {
    this(vv, vertexData, DEFAULT_PICKED_COLOUR, DEFAULT_UNPICKED_COLOUR);
  }

  public DefaultNodeIcon(VisualizationViewer<V, E> vv, V vertexData, Color pickedColour, Color unpickedColour) {
    this.vv = vv;
    this.vertexData = vertexData;
    this.pickedColour = pickedColour;
    this.unpickedColour = unpickedColour;
    this.dimension = DEFAULT_DIMENSION;
  }

  public int getIconHeight() {
    return dimension;
  }

  public int getIconWidth() {
    return dimension;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (vv.getPickedVertexState().isPicked(vertexData)) {
      g.setColor(pickedColour);
    } else {
      g.setColor(unpickedColour);
    }
    g.fillOval(x, y, dimension, dimension);
    g.setColor(Color.black);
    g.drawOval(x, y, dimension, dimension);

    // If we want a text label within the node (space for about 1 character)
    String label = createNodeLabel();
    if (label != null) {
      if (vv.getPickedVertexState().isPicked(vertexData)) {
        g.setColor(Color.cyan);
      } else {
        g.setColor(Color.black);
      }
      g.drawString(label, x + 6, y + 15);
    }
  }

  /**
   * Subclasses can override this method to create text that is rendered as part of the icon.
   *
   * Default implementation doesn't generate a label.
   * @return
   */
  protected String createNodeLabel() {
    return null;
  }
}
