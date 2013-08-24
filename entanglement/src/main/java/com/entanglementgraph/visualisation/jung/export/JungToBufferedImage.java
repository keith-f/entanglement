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

package com.entanglementgraph.visualisation.jung.export;

import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.visualisation.jung.renderers.CustomRendererRegistry;
import com.entanglementgraph.visualisation.jung.renderers.CustomVertexRenderer;
import com.mongodb.DBObject;
import com.scalesinformatics.hazelcast.concurrent.ThreadUtils;
import com.scalesinformatics.uibot.BotLogger;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.util.Animator;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class JungToBufferedImage {
//  private static final Logger logger = Logger.getLogger(JungToBufferedImage.class.getName());
  private final BotLogger logger;
  private final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();
  private int layoutSizeX;
  private int layoutSizeY;
  private long animationSeconds;
  private final CustomVertexRenderer customVertexRenderers;
  private final Graph<DBObject, DBObject> jungGraph;

  public JungToBufferedImage(BotLogger logger, int layoutSizeX, int layoutSizeY, long animationSeconds,
                             CustomVertexRenderer customRendererRegistry, Graph<DBObject, DBObject> jungGraph) {
    this.logger = logger;
    this.layoutSizeX = layoutSizeX;
    this.layoutSizeY = layoutSizeY;
    this.animationSeconds = animationSeconds;
    this.customVertexRenderers = customRendererRegistry;
    this.jungGraph = jungGraph;
  }

  private void log(String string) {
    if (logger != null) {
      logger.println(string);
    }
  }

  private void log(String string, Object ... params) {
    if (logger != null) {
      logger.println(string, params);
    }
  }

  public BufferedImage createImage() {
    VisualizationViewer<DBObject, DBObject> vv = createVisualisationViewerForFileExports(jungGraph);
    VisualizationImageServer<DBObject, DBObject> vis = createImageServerForFileExports(vv);

    // Export to image
    BufferedImage image = (BufferedImage) vis.getImage(
        new Point2D.Double(vis.getGraphLayout().getSize().getWidth() / 2,
            vis.getGraphLayout().getSize().getHeight() / 2),
        new Dimension(vis.getGraphLayout().getSize()));
    return image;
  }

  private VisualizationViewer<DBObject, DBObject> createVisualisationViewerForFileExports(Graph<DBObject, DBObject> graph) {
    /*
     * Create an exporter-specific layout an VisualisationViewer here. We can't just use a TrackingVisualisation
     * here because we'd get artifacts from the animation present in the exported image file.
     */
    Layout<DBObject, DBObject> layout = new FRLayout<>(graph);
    layout.setSize(new Dimension(layoutSizeX, layoutSizeY));
    VisualizationViewer<DBObject, DBObject> vv =  new VisualizationViewer<>(layout);
    vv.setDoubleBuffered(false);
    customVertexRenderers.setVisualiser(vv);

//    StaticLayout<DBObject, DBObject> staticLayout = new StaticLayout<>(graph, layout, new Dimension(layoutSizeX, layoutSizeY));
    LayoutTransition<DBObject, DBObject> lt =
        new LayoutTransition<>(vv, vv.getGraphLayout(), vv.getGraphLayout());
    log("Animating the image export layout for %s seconds ...", entFormat.format(animationSeconds).toString());
    Animator animator = new Animator(lt);
    animator.setSleepTime(1);
    animator.start();
//    logger.println("Waiting for animation to finish");
    ThreadUtils.sleep(animationSeconds * 1000);
    animator.stop();
    log("Animation finished.");
    return vv;
  }
  //FIXME delete this - it's now in JungToBufferedImage
  private VisualizationImageServer<DBObject, DBObject> createImageServerForFileExports(VisualizationViewer<DBObject, DBObject> vv) {
    // Create a VisualizationImageServer
    // vv is the VisualizationViewer containing the Jung graph (within the tracking visualisation)
    VisualizationImageServer<DBObject, DBObject> vis =
        new VisualizationImageServer<>(vv.getGraphLayout(), vv.getGraphLayout().getSize());

    vis.getRenderContext().setVertexLabelTransformer(customVertexRenderers.getVertexLabelTransformer());
    vis.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.cyan));
    vis.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.cyan));

    vis.getRenderContext().setVertexIconTransformer(customVertexRenderers.getVertexIconTransformer());
    vis.getRenderContext().setVertexFillPaintTransformer(new PickableVertexPaintTransformer<>(vis.getPickedVertexState(), Color.white, Color.yellow));
    vis.getRenderContext().setEdgeDrawPaintTransformer(new PickableEdgePaintTransformer<>(vis.getPickedEdgeState(), Color.black, Color.lightGray));

    return vis;
  }
}
