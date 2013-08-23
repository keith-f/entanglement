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

import com.entanglementgraph.graph.GraphEntityDAO;
import com.entanglementgraph.visualisation.jung.renderers.CustomVertexRenderer;
import com.mongodb.DBObject;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.util.Animator;
import org.apache.commons.collections15.Transformer;

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

//  private EntanglementToJungConverter entanglementToJung;

  private final CustomVertexRenderer customVertexRenderer;


  private Layout<DBObject, DBObject> layout;
  private VisualizationViewer<DBObject, DBObject> jungViewer;

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

  public void update(Graph<DBObject, DBObject> newJungGraph) {

    if (jungViewer == null) {
      createNewVisualizationViewer(newJungGraph);
    } else {
      Graph<DBObject, DBObject> existingGraph = layout.getGraph();
      updateExistingVisualization(existingGraph, newJungGraph);
      relayout(existingGraph);
    }
  }

  private void relayout(Graph<DBObject, DBObject> g) {
    layout.initialize();
    Relaxer relaxer = new VisRunner((IterativeContext) layout);
    relaxer.stop();
    relaxer.prerelax();
    StaticLayout<DBObject, DBObject> staticLayout = new StaticLayout<>(g, layout);
    LayoutTransition<DBObject, DBObject> lt =
        new LayoutTransition<>(jungViewer, jungViewer.getGraphLayout(), staticLayout);
    Animator animator = new Animator(lt);
    animator.start();
    jungViewer.repaint();
  }

  private void createNewVisualizationViewer(Graph<DBObject, DBObject> jungGraph) {

    layout = new FRLayout<>(jungGraph);
    layout.setSize(new Dimension(layoutDimensionX, layoutDimensionY));

    jungViewer =  new VisualizationViewer<>(layout);
    jungViewer.setDoubleBuffered(true);
    customVertexRenderer.setVisualiser(jungViewer);

    jungViewer.setPreferredSize(new Dimension(displayDimensionX, displayDimensionY)); //Sets the viewing area size
    jungViewer.getRenderContext().setVertexLabelTransformer(customVertexRenderer.getVertexLabelTransformer());
    jungViewer.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.cyan));
    jungViewer.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.cyan));
//    jungViewer.getRenderContext().setEdgeLabelTransformer(customVertexRenderer.getEdgeLabelTransformer());
    jungViewer.getRenderContext().setEdgeLabelTransformer(new Transformer<DBObject, String>() {
      @Override
      public String transform(DBObject dbObject) {
        return (String) dbObject.get(GraphEntityDAO.FIELD_KEYS_TYPE);
      }
    });

    jungViewer.getRenderContext().setVertexIconTransformer(customVertexRenderer.getVertexIconTransformer());
//    jungViewer.getRenderContext().setVertexShapeTransformer(customVertexRenderer.getVertexShapeTransformer());
    jungViewer.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<>(jungViewer.getPickedVertexState(), Color.white, Color.yellow));
    jungViewer.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<>(jungViewer.getPickedEdgeState(), Color.black, Color.lightGray));


//    jungViewer.setVertexToolTipTransformer(new ToStringLabeller<DBObject>());
    jungViewer.setVertexToolTipTransformer(customVertexRenderer.getTooltipTransformer());

    jungViewer.setBackground(Color.white);
  }

  private void updateExistingVisualization(Graph<DBObject, DBObject> existingGraph, Graph<DBObject, DBObject> newGraph) {
    switch (updateType) {
      case APPEND_ON_CURSOR_MOVE:
        mergeGraphs(existingGraph, newGraph);
        break;
      case REPLACE_ON_CURSOR_MOVE:
        layout.setGraph(newGraph);
        break;
    }
  }

  private void mergeGraphs(Graph<DBObject, DBObject> existingGraph, Graph<DBObject, DBObject> newGraph) {
    for (DBObject vertex : newGraph.getVertices()) {
      //FIXME do this properly. For now, just add the objects and see what happens...
      existingGraph.addVertex(vertex);
    }

    for (DBObject edge : newGraph.getEdges()) {
      //FIXME do this properly. For now, just add the objects and see what happens...
      Pair<DBObject> pair = newGraph.getEndpoints(edge);
      existingGraph.addEdge(edge, pair.getFirst(), pair.getSecond());
    }
  }

  public VisualizationViewer<DBObject, DBObject> getJungViewer() {
    return jungViewer;
  }

  public int getLayoutDimensionX() {
    return layoutDimensionX;
  }

  public int getLayoutDimensionY() {
    return layoutDimensionY;
  }

  public int getDisplayDimensionX() {
    return displayDimensionX;
  }

  public int getDisplayDimensionY() {
    return displayDimensionY;
  }

  public CustomVertexRenderer getCustomVertexRenderer() {
    return customVertexRenderer;
  }
}
