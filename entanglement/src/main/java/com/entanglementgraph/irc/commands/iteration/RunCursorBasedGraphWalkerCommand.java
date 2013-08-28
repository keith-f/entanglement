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
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalker;
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalkerRunnable;
import com.entanglementgraph.iteration.walkers.GraphWalkerException;
import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.specialistnodes.XYChartNode;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.visualisation.jung.JungGraphFrame;
import com.entanglementgraph.visualisation.jung.MongoToJungGraphExporter;
import com.entanglementgraph.visualisation.jung.TrackingVisualisation;
import com.entanglementgraph.visualisation.jung.imageexport.ImageUtil;
import com.entanglementgraph.visualisation.jung.imageexport.JungToBufferedImage;
import com.entanglementgraph.visualisation.jung.imageexport.OutputFileUtil;
import com.entanglementgraph.visualisation.jung.renderers.CategoryDatasetChartRenderer;
import com.entanglementgraph.visualisation.jung.renderers.CustomRendererRegistry;
import com.entanglementgraph.visualisation.jung.renderers.XYDatasetChartRenderer;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import edu.uci.ics.jung.graph.Graph;

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
    txt.append("Runs the specified "+ CursorBasedGraphWalker.class.getSimpleName()+" implementation, which iterates " +
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
    params.add(new OptionalParam("export-animation-seconds", Long.class, "15", "Sets the length of time (in seconds) that layout algorithms are run for (used only for image file exports)."));
    params.add(new OptionalParam("export-filename-base", String.class, "The file name 'stem' to use for image files that are exported. If not specified, the name of the destination graph will be used."));

    return params;
  }

  public RunCursorBasedGraphWalkerCommand() {
    super(AbstractEntanglementCommand.Requirements.GRAPH_CONN_NEEDED, AbstractEntanglementCommand.Requirements.CURSOR_NEEDED);
  }

  private String tempCluster;
  protected GraphConnection destination;

  protected EntityDisplayNameRegistry displayNameFactories;
  protected CustomRendererRegistry customVertexRenderers;
  private boolean enableGui;
  private boolean enablePng;
  private boolean enableJpeg;
  private boolean enableBmp;
  private long exportAnimationSeconds;
  protected String outputDirPath;

  boolean track;
  private int layoutSizeX;
  private int layoutSizeY;
  private int displaySizeX;
  private int displaySizeY;

  private String exportFilenameBase;

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


    outputDirPath = parsedArgs.get("output-dir").getStringValue();
    enablePng = parsedArgs.get("enable-png").parseValueAsBoolean();
    enableJpeg = parsedArgs.get("enable-jpeg").parseValueAsBoolean();
    enableBmp = parsedArgs.get("enable-bmp").parseValueAsBoolean();
    exportAnimationSeconds = parsedArgs.get("export-animation-seconds").parseValueAsLong();
    exportFilenameBase = parsedArgs.get("export-filename-base").getStringValue();





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
      if (destinationConnName != null) {
        destination = state.getUserObject().createGraphConnectionFor(destinationConnName);
      } else {
        destination = createTemporaryGraph(tempCluster);
      }

      if (exportFilenameBase == null) {
        exportFilenameBase = destination.getGraphName();
      }

      // Renderers are used for GUI and file export visualisations.
      configureDefaultRenderers();

      CursorBasedGraphWalkerRunnable worker = new CursorBasedGraphWalkerRunnable(
          logger, state.getUserObject(), graphConn, destination, walker, cursor.getPosition());

      // Runs the graph walker. Results have been placed in the destination graph.
      worker.run();

      // Now, optionally run one or more image exporters over the destination graph.
      runVisualisations(destination.getMarshaller(), destination);

      Message msg = new Message(channel, "Completed. Destination graph is: "+destination.getGraphName());
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

  private void runVisualisations(DbObjectMarshaller marshaller, GraphConnection graph)
      throws DbObjectMarshallerException, GraphModelException, IOException {
    Graph<DBObject, DBObject> jungGraph = null;
    if (enableGui || enablePng || enableBmp || enableJpeg) {
      logger.println("Creating in-memory JUNG representation of the destinationGraph Entanglement graph: %s", graph.getGraphName());
      MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
      dbToJung.addEntireGraph(graph);
      jungGraph = dbToJung.getGraph();
    }

    if (enableGui) {
      runGui(jungGraph);
    }

    if (enablePng || enableBmp || enableJpeg) {

      JungToBufferedImage graphToImage = new JungToBufferedImage(logger,
          layoutSizeX, layoutSizeY, exportAnimationSeconds, customVertexRenderers, jungGraph);
      logger.println("Exporting image...");
      BufferedImage image = graphToImage.createImage();
      if (enablePng) {
        File file = OutputFileUtil.createFile(new File("."), exportFilenameBase, ".png",
            layoutSizeX, layoutSizeY, exportAnimationSeconds);
        ImageUtil.writePng(image, file);
        logger.infoln("Written: %s", entFormat.format(file).toString());
      }
      if (enableJpeg) {
        File file = OutputFileUtil.createFile(new File("."), exportFilenameBase, ".jpeg",
            layoutSizeX, layoutSizeY, exportAnimationSeconds);
        ImageUtil.writeJpeg(image, file);
        logger.infoln("Written: %s", entFormat.format(file).toString());
      }
      if (enableBmp) {
        File file = OutputFileUtil.createFile(new File("."), exportFilenameBase, ".bmp",
            layoutSizeX, layoutSizeY, exportAnimationSeconds);
        ImageUtil.writeBmp(image, file);
        logger.infoln("Written: %s", entFormat.format(file).toString());
      }
    }
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
      frame = new JungGraphFrame(logger, trackingVis, trackingVis.getJungViewer());
      frame.getFrame().setVisible(true);
    }
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
