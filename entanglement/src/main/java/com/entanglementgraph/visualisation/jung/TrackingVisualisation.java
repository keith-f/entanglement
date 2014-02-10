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

import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.Node;
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


  private Layout<Node, Edge> layout;
  private VisualizationViewer<Node, Edge> jungViewer;

  private int layoutDimensionX;
  private int layoutDimensionY;
  private int displayDimensionX;
  private int displayDimensionY;

  private UpdateType updateType;

  private Animator animator;

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

  public void update(Graph<Node, Edge> newJungGraph) {

    if (jungViewer == null) {
      createNewVisualizationViewer(newJungGraph);
    } else {
      Graph<Node, Edge> existingGraph = layout.getGraph();
      updateExistingVisualization(existingGraph, newJungGraph);
      relayout(existingGraph);
    }
  }

  private void relayout(Graph<Node, Edge> g) {
    layout.initialize();
    Relaxer relaxer = new VisRunner((IterativeContext) layout);
    relaxer.stop();
    relaxer.prerelax();
    StaticLayout<Node, Edge> staticLayout = new StaticLayout<>(g, layout);
    LayoutTransition<Node, Edge> lt =
        new LayoutTransition<>(jungViewer, jungViewer.getGraphLayout(), staticLayout);
    if (animator != null) {
      animator.stop();
    }
    animator = new Animator(lt);
    animator.start();
    jungViewer.repaint();
  }

  private void createNewVisualizationViewer(Graph<Node, Edge> jungGraph) {

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
    jungViewer.getRenderContext().setEdgeLabelTransformer(new Transformer<Edge, String>() {
      @Override
      public String transform(Edge edge) {
        return edge.getKeys().getType();
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

  private void updateExistingVisualization(Graph<Node, Edge> existingGraph, Graph<Node, Edge> newGraph) {
    switch (updateType) {
      case APPEND_ON_CURSOR_MOVE:
        mergeGraphs(existingGraph, newGraph);
        break;
      case REPLACE_ON_CURSOR_MOVE:
        layout.setGraph(newGraph);
        break;
    }
  }

  private void mergeGraphs(Graph<Node, Edge> existingGraph, Graph<Node, Edge> newGraph) {
    for (Node vertex : newGraph.getVertices()) {
      //FIXME do this properly. For now, just add the objects and see what happens...
      existingGraph.addVertex(vertex);
    }

    for (Edge edge : newGraph.getEdges()) {
      //FIXME do this properly. For now, just add the objects and see what happens...
      Pair<Node> pair = newGraph.getEndpoints(edge);
      existingGraph.addEdge(edge, pair.getFirst(), pair.getSecond());
    }
  }

  public VisualizationViewer<Node, Edge> getJungViewer() {
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

  public Animator getAnimator() {
    return animator;
  }
}
