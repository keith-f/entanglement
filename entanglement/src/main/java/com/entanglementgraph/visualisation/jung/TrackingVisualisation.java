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

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.visualisation.jung.renderers.CustomVertexRenderer;
import com.mongodb.DBObject;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;

import java.awt.*;

/**
 * Listens to events in order to update a Jung visualisation when new data becomes available, or changes need to be
 * made.
 *
 * The resulting visualisations can be displayed on-screen, or sent to an image file for archival.
 *
 * User: keith
 * Date: 15/08/13; 15:56
 *
 * @author Keith Flanagan
 */
public class TrackingVisualisation {

  /**
   * Specifies what happens to the existing visualisation when a cursor movement event occurs.
   */
  public static enum UpdateType {
    REPLACE_ON_CURSOR_MOVE,
    APPEND_ON_CURSOR_MOVE;
  }

  private EntanglementToJungConverter entanglementToJung;

  private final CustomVertexRenderer customVertexRenderer;


  private Layout<DBObject, DBObject> layout;
  private VisualizationViewer<DBObject, DBObject> vv;

  private int layoutDimensionX;
  private int layoutDimensionY;
  private int displayDimensionX;
  private int displayDimensionY;

  private UpdateType updateType;

  public TrackingVisualisation(CustomVertexRenderer customVertexRenderer, UpdateType updateType,
                               int layoutDimensionX, int layoutDimensionY,
                               int displayDimensionX, int displayDimensionY) {
    this.customVertexRenderer = customVertexRenderer;
    this.updateType = updateType;
    this.layoutDimensionX = layoutDimensionX;
    this.layoutDimensionY = layoutDimensionY;
    this.displayDimensionX = displayDimensionX;
    this.displayDimensionY = displayDimensionY;

  }

  private void update(Graph<DBObject, DBObject> newJungGraph) {

    if (vv == null) {
      createNewVisualizationViewer(newJungGraph);
    } else {
      updateExistingVisualization(newJungGraph);
      layout.reset();
    }
  }

  private void createNewVisualizationViewer(Graph<DBObject, DBObject> jungGraph) {

    layout = new FRLayout<>(jungGraph);
    layout.setSize(new Dimension(layoutDimensionX, layoutDimensionY));

    vv =  new VisualizationViewer<>(layout);
    vv.setDoubleBuffered(true);

    vv.setPreferredSize(new Dimension(displayDimensionX, displayDimensionY)); //Sets the viewing area size
    vv.getRenderContext().setVertexLabelTransformer(customVertexRenderer.getVertexLabelTransformer());
    vv.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.cyan));
    vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.cyan));

    vv.getRenderContext().setVertexIconTransformer(customVertexRenderer.getVertexIconTransformer());
//    vv.getRenderContext().setVertexShapeTransformer(customVertexRenderer.getVertexShapeTransformer());
    vv.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<>(vv.getPickedVertexState(), Color.white,  Color.yellow));
    vv.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<>(vv.getPickedEdgeState(), Color.black, Color.lightGray));


//    vv.setVertexToolTipTransformer(new ToStringLabeller<DBObject>());
    vv.setVertexToolTipTransformer(customVertexRenderer.getTooltipTransformer());

    vv.setBackground(Color.white);
  }

  private void updateExistingVisualization(Graph<DBObject, DBObject> jungGraph) {
    switch (updateType) {
      case APPEND_ON_CURSOR_MOVE:
        mergeGraphs(jungGraph);
        break;
      case REPLACE_ON_CURSOR_MOVE:
        layout.setGraph(jungGraph);
        break;
    }
  }

  private void mergeGraphs(Graph<DBObject, DBObject> newJungGraph) {
    for (DBObject vertex : newJungGraph.getVertices()) {

    }

    for (DBObject edge : newJungGraph.getEdges()) {

    }
  }

}
