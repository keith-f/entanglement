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

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
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
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * User: keith
 * Date: 19/08/13; 15:38
 *
 * @author Keith Flanagan
 */
public class RunCursorBasedGraphWalkerCommand extends AbstractEntanglementCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Runs the specified"+ CursorBasedGraphWalker.class.getSimpleName()+" implementation, which iterates " +
        "over a source graph, performs some queries or executes some rules before writing the results into a " +
        "destination graph.");
    return txt.toString();
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();

    params.add(new RequiredParam("walker", String.class, "The full or simple class name of the graph walker to run."));
    params.add(new RequiredParam("temp-cluster", String.class, "The name of a configured MongoDB cluster to use for storing temporary graphs."));
    params.add(new OptionalParam("destination-conn", String.class, "The graph connection name of the destination graph. If this parameter is not specified, then a temporary graph with a randomised name will be created."));

    params.add(new OptionalParam("enable-gui", Boolean.class, Boolean.FALSE.toString(), "If set true, displays the destination graph ."));
    params.add(new OptionalParam("track", Boolean.class, Boolean.TRUE.toString(), "Specifies whether the GUI should track cursor events and update itself when the cursor moves to a new position."));
    params.add(new OptionalParam("layout-size-x", Integer.class, "800", "The width (in pixels) that the node layout engine will use for performing layouts."));
    params.add(new OptionalParam("layout-size-y", Integer.class, "800", "The height (in pixels) that the node layout engine will use for performing layouts."));
    params.add(new OptionalParam("display-size-x", Integer.class, "850", "The preferred width (in pixels) of the graph viewport."));
    params.add(new OptionalParam("display-size-y", Integer.class, "850", "The preferred height (in pixels) of the graph viewport."));

    params.add(new OptionalParam("output-dir", String.class, "subgraph-exports/", "The path to a directory that exported graph files will be written to."));
    params.add(new OptionalParam("enable-png", Boolean.class, Boolean.FALSE.toString(), "If set true, saves an image of the destination graph as a PNG file."));
    params.add(new OptionalParam("enable-jpeg", Boolean.class, Boolean.FALSE.toString(), "If set true, saves an image of the destination graph as a JPEG file."));
    params.add(new OptionalParam("enable-bmp", Boolean.class, Boolean.FALSE.toString(), "If set true, saves an image of the destination graph as a BMP file."));

    return params;
  }

  public RunCursorBasedGraphWalkerCommand() {
    super(AbstractEntanglementCommand.Requirements.GRAPH_CONN_NEEDED, AbstractEntanglementCommand.Requirements.CURSOR_NEEDED);
  }

  private String tempCluster;

  private EntityDisplayNameRegistry displayNameFactories;
  private CustomRendererRegistry customVertexRenderers;
  private boolean enableGui;
  private boolean enablePng;
  private boolean enableJpeg;
  private boolean enableBmp;

  boolean track;
  private int layoutSizeX;
  private int layoutSizeY;
  private int displaySizeX;
  private int displaySizeY;

//  private TrackingVisualisation trackingVisualisation;
  private JungGraphFrame frame;

  @Override
  protected Message _processLine() throws UserException, BotCommandException {

    //String graphConnName = parsedArgs.get("conn").getStringValue();
    String walkerName = parsedArgs.get("walker").getStringValue();
    tempCluster = parsedArgs.get("temp-cluster").getStringValue();
    String destinationConnName = parsedArgs.get("destination-conn").getStringValue();



    enableGui = parsedArgs.get("enable-gui").parseValueAsBoolean();
    track = parsedArgs.get("track").parseValueAsBoolean();
    layoutSizeX = parsedArgs.get("layout-size-x").parseValueAsInteger();
    layoutSizeY = parsedArgs.get("layout-size-y").parseValueAsInteger();
    displaySizeX = parsedArgs.get("display-size-x").parseValueAsInteger();
    displaySizeY = parsedArgs.get("display-size-y").parseValueAsInteger();


    String outputDirPath = parsedArgs.get("output-dir").getStringValue();
    enablePng = parsedArgs.get("enable-png").parseValueAsBoolean();
    enableJpeg = parsedArgs.get("enable-jpeg").parseValueAsBoolean();
    enableBmp = parsedArgs.get("enable-bmp").parseValueAsBoolean();





    CursorBasedGraphWalker walker;
    try {
      walker = new CursorBasedGraphWalker.Provider(graphConn.getClassLoader()).getForName(walkerName);
    } catch (GraphWalkerException e) {
      e.printStackTrace();
      throw new UserException("Failed to find or initialise a "+CursorBasedGraphWalker.class.getSimpleName()
          + " instance named: "+walkerName);
    }

    try {
      // Use either a named destination connection, or create a temporary graph.
      GraphConnection destination;
      if (destinationConnName != null) {
        destination = state.getUserObject().createGraphConnectionFor(destinationConnName);
      } else {
        destination = createTemporaryGraphConnection(tempCluster);
      }

      bot.infoln(channel, "Exporting a subgraph from %s to %s", graphConnName, destination.getGraphName());

      walker.setRuntime(state.getUserObject());
      walker.setCursorContext(cursorContext);
      walker.setSourceGraph(graphConn);
      walker.setDestinationGraph(destination);
      walker.setStartPosition(cursor);

      walker.initialise();
      walker.execute();

      logger.println("Iteration of %s by %s completed. Destination graph is: %s/%s/%s",
          graphConnName, walker.getClass().getName(),
          graphConn.getPoolName(), graphConn.getDatabaseName(), graphConn.getGraphName());


      Graph<DBObject, DBObject> jungGraph = null;
      if (enableGui || enablePng || enableBmp || enableJpeg) {
        logger.println("Creating in-memory JUNG representation of the destination Entanglement graph: %s", destination.getGraphName());
        // Renderers are used for GUI and file export visualisations.
        configureDefaultRenderers();
        jungGraph = entanglementToJung(destination);
      }


      if (enableGui) {
        runGui(jungGraph);
      }

      if (enablePng || enableBmp || enableJpeg) {
        doImageFileExports(jungGraph, outputDirPath);
      }

      Message msg = new Message(channel, "Completed.");
      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }


  /**
   * If custom renderers (eg, set by a subclass) have not been configured, then create default ones instead.
   */
  private void configureDefaultRenderers() {
    if (displayNameFactories == null) {
      displayNameFactories = new EntityDisplayNameRegistry();
    }
    if (customVertexRenderers == null) {
      customVertexRenderers = new CustomRendererRegistry(graphConn.getMarshaller(), displayNameFactories);
      customVertexRenderers.addTypeToRendererMapping(CategoryChartNode.getTypeName(), CategoryDatasetChartRenderer.class);
      customVertexRenderers.addTypeToRendererMapping(XYChartNode.getTypeName(), XYDatasetChartRenderer.class);
    }
  }

  private Graph<DBObject, DBObject> entanglementToJung(GraphConnection destGraph)
      throws DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {
    // Create an in-memory Jung graph representation of destGraph.
    MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
    dbToJung.addEntireGraph(destGraph);
    return dbToJung.getGraph();
  }

  private void runGui(Graph<DBObject, DBObject> jungGraph) {
    // Used for interactive GUIs
    TrackingVisualisation trackingVis = new TrackingVisualisation(
        customVertexRenderers,
        track
            ? TrackingVisualisation.UpdateType.APPEND_ON_CURSOR_MOVE
            : TrackingVisualisation.UpdateType.REPLACE_ON_CURSOR_MOVE,
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
      ImageIO.write(image, "png", generateOutputFile(outputDirPath, ".png"));
    }
    if (enableJpeg) {
      ImageIO.write(image, "jpeg", generateOutputFile(outputDirPath, ".jpeg"));
    }
    if (enableBmp) {
      ImageIO.write(image, "bmp", generateOutputFile(outputDirPath, ".bmp"));
    }

  }

  private File generateOutputFile(String directory, String extension) {
    File outputDir = new File(directory);
    if (!outputDir.exists()) {
      boolean success = outputDir.mkdirs();
    }
    String startNodeDisplayName = displayNameFactories.createNameForEntity(cursor.getPosition());
    File outputFile = new File(outputDir, startNodeDisplayName+extension);
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
}
