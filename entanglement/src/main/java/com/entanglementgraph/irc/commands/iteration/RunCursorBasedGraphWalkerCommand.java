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
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

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

    return params;
  }

  public RunCursorBasedGraphWalkerCommand() {
    super(AbstractEntanglementCommand.Requirements.GRAPH_CONN_NEEDED, AbstractEntanglementCommand.Requirements.CURSOR_NEEDED);
  }

  private String tempCluster;

  private EntityDisplayNameRegistry displayNameFactories;
  private CustomRendererRegistry customVertexRenderers;
  private boolean enableGui;
  boolean track;
  private int layoutSizeX;
  private int layoutSizeY;
  private int displaySizeX;
  private int displaySizeY;

  private TrackingVisualisation trackingVisualisation;
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

      if (enableGui) {
        logger.println("GUI visualisation requested. Populating Jung graph.");
        configureDefaultRenderers();
        trackingVisualisation = new TrackingVisualisation(
            customVertexRenderers,
            track
                ? TrackingVisualisation.UpdateType.APPEND_ON_CURSOR_MOVE
                : TrackingVisualisation.UpdateType.REPLACE_ON_CURSOR_MOVE,
            layoutSizeX, layoutSizeY, displaySizeX, displaySizeY);

        display(destination);
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

  private void display(GraphConnection destGraph) throws DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {

    // Create an in-memory Jung graph representation of destGraph.
    MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
    dbToJung.addEntireGraph(destGraph);

    // Pass this graph to the TrackingVisualisation
    trackingVisualisation.update(dbToJung.getGraph());

    if (frame == null) {
      //This is the first refresh. We need a JFrame to display the visualisation.
      frame = new JungGraphFrame(trackingVisualisation.getJungViewer());
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
