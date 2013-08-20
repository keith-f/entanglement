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

import com.entanglementgraph.graph.GraphEntityDAO;
import com.entanglementgraph.graph.data.GraphEntity;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
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

  private static final Color DEFAULT_NORMAL_NODE_COLOUR = Color.BLACK;
  private static final Color DEFAULT_VIRTUAL_NODE_COLOUR = Color.ORANGE;

  private static final Stroke THICKER = new BasicStroke(2);
  private static final Stroke DOTTED = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2,2}, 0);


  protected final DbObjectMarshaller marshaller;
  protected final EntityDisplayNameRegistry displayNameFactory;

  protected final VisualizationViewer<V, E> vv;
  protected final V vertexData;

  protected int iconWidth;
  protected int iconHeight;
  protected int circleWidth;
  protected int circleHeight;
  private Color pickedColour;
  private Color unpickedColour;

  public DefaultNodeIcon(DbObjectMarshaller marshaller, EntityDisplayNameRegistry displayNameFactory,
                         VisualizationViewer<V, E> vv, V vertexData) {
    this(marshaller, displayNameFactory, vv, vertexData, DEFAULT_PICKED_COLOUR, DEFAULT_UNPICKED_COLOUR);
  }

  public DefaultNodeIcon(DbObjectMarshaller marshaller, EntityDisplayNameRegistry displayNameFactory,
                         VisualizationViewer<V, E> vv, V vertexData, Color pickedColour, Color unpickedColour) {
    this.marshaller = marshaller;
    this.displayNameFactory = displayNameFactory;
    this.vv = vv;
    this.vertexData = vertexData;
    this.pickedColour = pickedColour;
    this.unpickedColour = unpickedColour;
    this.iconWidth = DEFAULT_DIMENSION;
    this.iconHeight = DEFAULT_DIMENSION;
    this.circleWidth = DEFAULT_DIMENSION;
    this.circleHeight = DEFAULT_DIMENSION;
  }

  public int getIconHeight() {
    return iconHeight;
  }

  public int getIconWidth() {
    return iconWidth;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g;

    // Set true if this node is from a 'hanging edge' - i.e., its existence has been inferred.
    boolean isVirtual = vertexData.containsField(GraphEntityDAO.FIELD_VIRTUAL);

    // This avoids the node being drawn in a strange position (wrt edge lines) when the icon size is 'large'
    if (iconWidth > circleWidth) {
      x = x + (iconWidth / 2) - (circleWidth / 2);
    }
    if (iconHeight > circleHeight) {
      y = y + (iconHeight / 2) - (circleHeight / 2);
    }

    Color fillColour;
    if (vv.getPickedVertexState().isPicked(vertexData)) {
      fillColour = pickedColour;
    } else {
      fillColour = unpickedColour;
    }



    /*
     * Draw a normal circle if this node exists as a document.
     * Otherwise, indicate that this node doesn't actually exist (for example, as a hanging edge) by .
     */

    if (isVirtual) {
      Stroke original = g2.getStroke();
      g2.setStroke(DOTTED);
      g.setColor(fillColour);
      g2.drawOval(x, y, circleWidth, circleHeight);
      g2.setStroke(original);
    } else {
      g.setColor(fillColour);
      g.fillOval(x, y, circleWidth, circleHeight);
      g.setColor(DEFAULT_NORMAL_NODE_COLOUR);
      g.drawOval(x, y, circleWidth, circleHeight);
    }


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

  /**
   * Covenience function to deserialise objects
   */
  public <T extends GraphEntity> T deserialzeOrError(DBObject document, Class<T> type) {
    try {
      return marshaller.deserialize(document, type);
    } catch (DbObjectMarshallerException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to deserialize document to type: "+type+", doc: "+document);
    }
  }

  public Color getPickedColour() {
    return pickedColour;
  }

  public void setPickedColour(Color pickedColour) {
    this.pickedColour = pickedColour;
  }

  public Color getUnpickedColour() {
    return unpickedColour;
  }

  public void setUnpickedColour(Color unpickedColour) {
    this.unpickedColour = unpickedColour;
  }

  public EntityDisplayNameRegistry getDisplayNameFactory() {
    return displayNameFactory;
  }

  public DbObjectMarshaller getMarshaller() {
    return marshaller;
  }

  public void setIconWidth(int iconWidth) {
    this.iconWidth = iconWidth;
  }

  public void setIconHeight(int iconHeight) {
    this.iconHeight = iconHeight;
  }

  public int getCircleWidth() {
    return circleWidth;
  }

  public void setCircleWidth(int circleWidth) {
    this.circleWidth = circleWidth;
  }

  public int getCircleHeight() {
    return circleHeight;
  }

  public void setCircleHeight(int circleHeight) {
    this.circleHeight = circleHeight;
  }
}
