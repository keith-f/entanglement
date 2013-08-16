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
 *
 * @author Keith Flanagan
 */
public class Visualiser {

  private final Graph<DBObject, DBObject> graph;

  /**
   * the visual component and renderer for the graph
   */
  private VisualizationViewer<DBObject, DBObject> vv;

  public Visualiser(Graph<DBObject, DBObject> graph, CustomVertexRenderer customVertexRenderer,
                    int layoutDimensionX, int layoutDimensionY,
                    int displayDimensionX, int displayDimensionY) {
    this.graph = graph;


    Layout<DBObject, DBObject> layout = new FRLayout<>(graph);
    layout.setSize(new Dimension(layoutDimensionX, layoutDimensionY));
//    Layout<Integer, Number> layout = new FRLayout2<Integer,Number>(graph);
    vv =  new VisualizationViewer<>(layout);
    customVertexRenderer.setVisualiser(vv);
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

  public VisualizationViewer<DBObject, DBObject> getVv() {
    return vv;
  }
}
