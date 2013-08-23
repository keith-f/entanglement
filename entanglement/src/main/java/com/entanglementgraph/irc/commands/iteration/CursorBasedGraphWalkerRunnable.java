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
package com.entanglementgraph.irc.commands.iteration;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.iteration.CursorBasedGraphWalker;
import com.entanglementgraph.iteration.GraphWalkerException;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.specialistnodes.XYChartNode;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.visualisation.jung.JungGraphFrame;
import com.entanglementgraph.visualisation.jung.MongoToJungGraphExporter;
import com.entanglementgraph.visualisation.jung.TrackingVisualisation;
import com.entanglementgraph.visualisation.jung.renderers.CategoryDatasetChartRenderer;
import com.entanglementgraph.visualisation.jung.renderers.CustomRendererRegistry;
import com.entanglementgraph.visualisation.jung.renderers.XYDatasetChartRenderer;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.hazelcast.concurrent.ThreadUtils;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import com.scalesinformatics.util.UidGenerator;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A runnable for executing a graph walker.
 *
 * FIXME there is far too much functionality here. We should be running walkers only. Move export code elsewhere.
 *
 * User: keith
 * Date: 19/08/13; 15:38
 *
 * @author Keith Flanagan
 */
public class CursorBasedGraphWalkerRunnable implements Runnable {
//  private static final Logger logger = Logger.getLogger(CursorBasedGraphWalkerRunnable.class.getName());

  private final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();
  
  private final BotLogger logger;

  protected final EntanglementRuntime runtime;
  protected final GraphConnection sourceGraph;
  protected final GraphConnection destinationGraph;
  protected final CursorBasedGraphWalker walker;
  protected final EntityKeys<? extends Node> startPosition;

  protected EntityDisplayNameRegistry displayNameFactories;
  protected CustomRendererRegistry customVertexRenderers;
  private boolean enableGui;
  private boolean enablePng;
  private boolean enableJpeg;
  private boolean enableBmp;
  private long exportAnimationSeconds;
  protected String outputDirPath;

  private int layoutSizeX;
  private int layoutSizeY;
  private int displaySizeX;
  private int displaySizeY;

  private JungGraphFrame frame;

  public CursorBasedGraphWalkerRunnable(
      BotLogger logger, EntanglementRuntime runtime, GraphConnection sourceGraph, GraphConnection destinationGraph,
      CursorBasedGraphWalker walker, EntityKeys<? extends Node> startPosition) {
    this.logger = logger;
    this.runtime = runtime;
    this.sourceGraph = sourceGraph;
    this.destinationGraph = destinationGraph;
    this.walker = walker;
    this.startPosition = startPosition;
  }

  @Override
  public void run() {
    try {
      //Initialise a temporary graph cursor.
      GraphCursor tmpCursor = new GraphCursor(UidGenerator.generateUid(), startPosition);
      runtime.getCursorRegistry().addCursor(tmpCursor);

      //Configure walker

      walker.setRuntime(runtime);
      GraphCursor.CursorContext cursorContext = new GraphCursor.CursorContext(sourceGraph, runtime.getHzInstance());
      walker.setCursorContext(cursorContext);
      walker.setSourceGraph(sourceGraph);
      walker.setDestinationGraph(destinationGraph);
      walker.setStartPosition(tmpCursor);

      // For for a walk
      logger.println("Exporting a subgraph from %s to %s", sourceGraph.getGraphName(), destinationGraph.getGraphName());
      walker.initialise();
      walker.execute();
      logger.println("Iteration of %s by %s completed. Destination graph is: %s/%s/%s",
          sourceGraph.getGraphName(), walker.getClass().getName(),
          destinationGraph.getPoolName(), destinationGraph.getDatabaseName(), destinationGraph.getGraphName());

      //Remove temporary graph cursor
      runtime.getCursorRegistry().removeCursor(tmpCursor);

      // At this point, we've run the walker. The destination graph should contain the required nodes/edges.

      // Now, optionally run image exporters
      runExporters(sourceGraph.getMarshaller(), destinationGraph);
    } catch (Exception e) {
      e.printStackTrace();
      logger.getBot().printException(logger.getChannel(), "Graph walker failure", e);
    }

  }

  private void runExporters(DbObjectMarshaller marshaller, GraphConnection graph)
      throws DbObjectMarshallerException, IOException, GraphModelException, RevisionLogException {

    Graph<DBObject, DBObject> jungGraph = null;
    if (enableGui || enablePng || enableBmp || enableJpeg) {
      logger.println("Creating in-memory JUNG representation of the destinationGraph Entanglement graph: %s", destinationGraph.getGraphName());
      jungGraph = entanglementToJung(destinationGraph);
    }


    if (enableGui) {
      runGui(jungGraph);
    }

    if (enablePng || enableBmp || enableJpeg) {
      doImageFileExports(jungGraph, outputDirPath);
    }

  }


  private Graph<DBObject, DBObject> entanglementToJung(GraphConnection graph)
      throws DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {
    // Create an in-memory Jung graph representation of destGraph.
    MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
    dbToJung.addEntireGraph(graph);
    return dbToJung.getGraph();
  }

  private void runGui(Graph<DBObject, DBObject> jungGraph) {
    // Used for interactive GUIs
    TrackingVisualisation trackingVis = new TrackingVisualisation(
        customVertexRenderers, TrackingVisualisation.UpdateType.REPLACE_ON_CURSOR_MOVE,
        layoutSizeX, layoutSizeY, displaySizeX, displaySizeY);

    // Pass this graph to the TrackingVisualisation
    logger.println("Updating tracking visualisation");
    trackingVis.update(jungGraph);

    if (frame == null) {
      //This is the first refresh. We need a JFrame to display the visualisation.
      frame = new JungGraphFrame(trackingVis.getJungViewer());
      frame.getFrame().setVisible(true);
    }
  }


  private void doImageFileExports(Graph<DBObject, DBObject> jungGraph, String outputDirPath) throws IOException {
    VisualizationViewer<DBObject, DBObject> vv = createVisualisationViewerForFileExports(jungGraph);
    VisualizationImageServer<DBObject, DBObject> vis = createImageServerForFileExports(vv);

    // Export to image
    BufferedImage image = (BufferedImage) vis.getImage(
        new Point2D.Double(vis.getGraphLayout().getSize().getWidth() / 2,
        vis.getGraphLayout().getSize().getHeight() / 2),
        new Dimension(vis.getGraphLayout().getSize()));

    if (enablePng) {
      File outFile =generateOutputFile(outputDirPath, ".png", layoutSizeX, layoutSizeY, exportAnimationSeconds);
      logger.println("Writing file %s", entFormat.format(outFile).toString());
      ImageIO.write(image, "png", outFile);
    }
    if (enableJpeg) {
      File outFile =generateOutputFile(outputDirPath, ".jpeg", layoutSizeX, layoutSizeY, exportAnimationSeconds);
      logger.println("Writing file %s", entFormat.format(outFile).toString());
      ImageIO.write(image, "jpeg", outFile);
    }
    if (enableBmp) {
      File outFile =generateOutputFile(outputDirPath, ".bmp", layoutSizeX, layoutSizeY, exportAnimationSeconds);
      logger.println("Writing file %s", entFormat.format(outFile).toString());
      ImageIO.write(image, "bmp", outFile);
    }

  }

  protected File generateOutputFile(String directory, String extension) {
    File outputDir = new File(directory);
    if (!outputDir.exists()) {
      boolean success = outputDir.mkdirs();
    }
    String startNodeDisplayName = displayNameFactories.createNameForEntity(startPosition).replace(",", "_").replace(":", "_");
    File outputFile = new File(outputDir, startNodeDisplayName+extension);
    return outputFile;
  }

  protected File generateOutputFile(String directory, String extension, int xDim, int yDim, long animationSeconds) {
    File outputDir = new File(directory);
    if (!outputDir.exists()) {
      boolean success = outputDir.mkdirs();
    }
    String startNodeDisplayName = displayNameFactories.createNameForEntity(startPosition).replace(",", "_").replace(":", "_");
    File outputFile = new File(outputDir, startNodeDisplayName+"-"+xDim+"x"+yDim+"-"+animationSeconds+"s"+extension);
    return outputFile;
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
    logger.println("Animating the image export layout for %s seconds ...", entFormat.format(exportAnimationSeconds).toString());
    Animator animator = new Animator(lt);
    animator.setSleepTime(1);
    animator.start();
//    logger.println("Waiting for animation to finish");
    ThreadUtils.sleep(exportAnimationSeconds * 1000);
    animator.stop();
    logger.println("Animation finished.");
    return vv;
  }

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


  public EntityDisplayNameRegistry getDisplayNameFactories() {
    return displayNameFactories;
  }

  public void setDisplayNameFactories(EntityDisplayNameRegistry displayNameFactories) {
    this.displayNameFactories = displayNameFactories;
  }

  public CustomRendererRegistry getCustomVertexRenderers() {
    return customVertexRenderers;
  }

  public void setCustomVertexRenderers(CustomRendererRegistry customVertexRenderers) {
    this.customVertexRenderers = customVertexRenderers;
  }

  public boolean isEnableGui() {
    return enableGui;
  }

  public void setEnableGui(boolean enableGui) {
    this.enableGui = enableGui;
  }

  public boolean isEnablePng() {
    return enablePng;
  }

  public void setEnablePng(boolean enablePng) {
    this.enablePng = enablePng;
  }

  public boolean isEnableJpeg() {
    return enableJpeg;
  }

  public void setEnableJpeg(boolean enableJpeg) {
    this.enableJpeg = enableJpeg;
  }

  public boolean isEnableBmp() {
    return enableBmp;
  }

  public void setEnableBmp(boolean enableBmp) {
    this.enableBmp = enableBmp;
  }

  public long getExportAnimationSeconds() {
    return exportAnimationSeconds;
  }

  public void setExportAnimationSeconds(long exportAnimationSeconds) {
    this.exportAnimationSeconds = exportAnimationSeconds;
  }

  public int getLayoutSizeX() {
    return layoutSizeX;
  }

  public void setLayoutSizeX(int layoutSizeX) {
    this.layoutSizeX = layoutSizeX;
  }

  public int getLayoutSizeY() {
    return layoutSizeY;
  }

  public void setLayoutSizeY(int layoutSizeY) {
    this.layoutSizeY = layoutSizeY;
  }

  public int getDisplaySizeX() {
    return displaySizeX;
  }

  public void setDisplaySizeX(int displaySizeX) {
    this.displaySizeX = displaySizeX;
  }

  public int getDisplaySizeY() {
    return displaySizeY;
  }

  public void setDisplaySizeY(int displaySizeY) {
    this.displaySizeY = displaySizeY;
  }

  public JungGraphFrame getFrame() {
    return frame;
  }

  public void setFrame(JungGraphFrame frame) {
    this.frame = frame;
  }

  public String getOutputDirPath() {
    return outputDirPath;
  }

  public void setOutputDirPath(String outputDirPath) {
    this.outputDirPath = outputDirPath;
  }
}
