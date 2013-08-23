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

package com.entanglementgraph.irc.commands.jungviz;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.export.DepthBasedSubgraphCreator;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.iteration.GraphIteratorException;
import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.specialistnodes.XYChartNode;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.jung.*;
import com.entanglementgraph.visualisation.jung.renderers.CategoryDatasetChartRenderer;
import com.entanglementgraph.visualisation.jung.renderers.CustomRendererRegistry;
import com.entanglementgraph.visualisation.jung.renderers.XYDatasetChartRenderer;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.mongodb.DBObject;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import com.scalesinformatics.util.UidGenerator;

import java.util.List;
import java.util.logging.Logger;

/**
 * This command opens a JFrame and tracks a specified cursor by displaying lists of incoming/outgoing edges of the
 * current node, as well as a Jung-rendered image of the immediate neighbourhood.
 *
 * Note; this command will only work where a graphical display is present!
 *
 * @author Keith Flanagan
 */
public class GuiNearestNeighboursCommand extends AbstractEntanglementCommand<EntanglementRuntime> {
  private static final Logger logger = Logger.getLogger(GuiNearestNeighboursCommand.class.getName());

  @Override
  public String getDescription() {
    return "Displays information about the cursor's immediate surroundings.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new OptionalParam("maxUids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("maxNames", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("track", Boolean.class, Boolean.TRUE.toString(), "Specifies whether the GUI should track cursor events and update itself when the cursor moves to a new position."));

    params.add(new OptionalParam("depth", Integer.class, "1", "Specifies the depth to search when generating the view. Default is to show directly connected neighbours only (depth=1)."));

    params.add(new RequiredParam("temp-cluster", String.class, "The name of a configured MongoDB cluster to use for storing temporary graphs."));

    params.add(new OptionalParam("layout-size-x", Integer.class, "800", "The width (in pixels) that the node layout engine will use for performing layouts."));
    params.add(new OptionalParam("layout-size-y", Integer.class, "800", "The height (in pixels) that the node layout engine will use for performing layouts."));
    params.add(new OptionalParam("display-size-x", Integer.class, "850", "The preferred width (in pixels) of the graph viewport."));
    params.add(new OptionalParam("display-size-y", Integer.class, "850", "The preferred height (in pixels) of the graph viewport."));

    return params;
  }

  public GuiNearestNeighboursCommand() {
    super(Requirements.GRAPH_CONN_NEEDED, Requirements.CURSOR_NEEDED);
  }

  private EntityDisplayNameRegistry displayNameFactories;
  private CustomRendererRegistry customVertexRenderers;
  private int maxUids;
  private int maxNames;
  private String tempCluster;

  private int depth;

  private int layoutSizeX;
  private int layoutSizeY;
  private int displaySizeX;
  private int displaySizeY;

  private TrackingVisualisation trackingVisualisation;
  private JungGraphFrame frame;


  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    depth = parsedArgs.get("depth").parseValueAsInteger();

    tempCluster = parsedArgs.get("temp-cluster").getStringValue();

    maxUids = parsedArgs.get("maxUids").parseValueAsInteger();
    maxNames = parsedArgs.get("maxNames").parseValueAsInteger();
    boolean track = parsedArgs.get("track").parseValueAsBoolean();

    layoutSizeX = parsedArgs.get("layout-size-x").parseValueAsInteger();
    layoutSizeY = parsedArgs.get("layout-size-y").parseValueAsInteger();
    displaySizeX = parsedArgs.get("display-size-x").parseValueAsInteger();
    displaySizeY = parsedArgs.get("display-size-y").parseValueAsInteger();

    // Here, we use generic Entanglement display name and Jung renderer registries
    // These could be replaced with project-specific classes, if necessary
    configureDefaultRenderers();
    trackingVisualisation = new TrackingVisualisation(
        customVertexRenderers,
        track
            ? TrackingVisualisation.UpdateType.APPEND_ON_CURSOR_MOVE
            : TrackingVisualisation.UpdateType.REPLACE_ON_CURSOR_MOVE,
        layoutSizeX, layoutSizeY, displaySizeX, displaySizeY);


    try {
      // Manually fire the first event
      notifyGraphCursorUpdated(cursor);

      // Register a listener to take care of future cursor movements
      CursorListener listener = new CursorListener();
      state.getUserObject().getCursorRegistry().getCurrentPositions().addEntryListener(listener, cursorName, true);

      Message msg = new Message(channel);

      EntityKeys<? extends Node> currentPos = cursor.getPosition();
      DBObject currentNodeObj = null;
      if (!cursor.isAtDeadEnd()) {
        currentNodeObj = cursor.resolve(graphConn);
        if (currentNodeObj == null) {
          throw new BotCommandException("A database document for the current cursor location could not be found: "
              +cursor.getPosition());
        }
        currentPos = MongoUtils.parseKeyset(graphConn.getMarshaller(), currentNodeObj);
      }


      boolean isAtDeadEnd = cursor.isAtDeadEnd();
      int historyIdx = cursor.getCursorHistoryIdx();
      msg.println("Cursor %s (%s) is currently located at: %s; Dead end? %s; Steps taken: %s",
          entFormat.formatCursorName(cursor.getName()).toString(), cursorName,
          entFormat.formatNodeKeyset(currentPos).toString(), entFormat.formatBoolean(isAtDeadEnd).toString(),
          entFormat.formatHistoryIndex(historyIdx).toString());
      msg.println("Short version: %s", entFormat.formatNodeKeysetShort(currentPos, maxUids, maxNames).toString());


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

  private class CursorListener implements EntryListener<String, GraphCursor> {

    @Override
    public void entryAdded(EntryEvent<String, GraphCursor> event) {
    }

    @Override
    public void entryRemoved(EntryEvent<String, GraphCursor> event) {
    }

    @Override
    public void entryUpdated(EntryEvent<String, GraphCursor> event) {
      bot.println("%s Received notification that cursor: %s has moved to %s",
          GuiNearestNeighboursCommand.class.getSimpleName(),
          entFormat.formatCursorName(event.getValue().getName()).toString(),
          entFormat.formatNodeKeysetShort(event.getValue().getPosition(), maxUids, maxNames).toString());
      notifyGraphCursorUpdated(event.getValue());
    }

    @Override
    public void entryEvicted(EntryEvent<String, GraphCursor> event) {
    }
  }

  private void notifyGraphCursorUpdated(GraphCursor newCursor) {
    try {
      bot.infoln(channel, "Received notification that cursor %s moved to %s via %s",
          newCursor.getName(), newCursor.getPosition(), newCursor.getArrivedVia());
      GraphConnection destGraph = exportSubgraph(newCursor);

      // Create an in-memory Jung graph representation of destGraph.
      MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
      dbToJung.addEntireGraph(destGraph);

      // Pass this graph connection to the TrackingVisualisation - it will decide what action to take.
      trackingVisualisation.update(dbToJung.getGraph());

      if (frame == null) {
        //This is the first refresh. We need a JFrame to display the visualisation.
        frame = new JungGraphFrame(trackingVisualisation.getJungViewer());
        frame.getFrame().setVisible(true);
      }


    } catch (Exception e) {
      bot.printException(channel, sender, "Failed to update GUI for new cursor position", e);
      e.printStackTrace();
    }
  }

  private GraphConnection exportSubgraph(GraphCursor graphCursor) throws GraphIteratorException, GraphConnectionFactoryException {
    GraphConnection sourceGraph = graphConn;
    GraphConnection destinationGraph = createTemporaryGraphConnection(tempCluster);
    DepthBasedSubgraphCreator exporter = new DepthBasedSubgraphCreator(
        sourceGraph, destinationGraph, state.getUserObject(), cursorContext, depth);

    //Use a throwaway cursor so as not to alter the cursor we're listening to, which may have unintended side effects
    GraphCursor tmpCursor = new GraphCursor(UidGenerator.generateUid(), graphCursor.getPosition());
    state.getUserObject().getCursorRegistry().addCursor(tmpCursor);
    exporter.execute(tmpCursor);
    state.getUserObject().getCursorRegistry().removeCursor(tmpCursor);
    return destinationGraph;
  }

//  private JungGraphFrame displayGraphInNewFrame(GraphConnection subgraph) throws GraphCursorException, DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {
//    // Export the temporary Entanglement graph to an in-memory Jung graph
//    MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
//    dbToJung.addEntireGraph(subgraph);
//    Graph<DBObject, DBObject> jungGraph = dbToJung.getGraph();
//
//    // Add a custom renderer for chart nodes
//    EntityDisplayNameRegistry displayNameFactories = new EntityDisplayNameRegistry();
//    CustomRendererRegistry customVertexRenderers = new CustomRendererRegistry(subgraph.getMarshaller(), displayNameFactories);
//    customVertexRenderers.addTypeToRendererMapping(CategoryChartNode.getTypeName(), CategoryDatasetChartRenderer.class);
//    customVertexRenderers.addTypeToRendererMapping(XYChartNode.getTypeName(), XYDatasetChartRenderer.class);
//
//
//    Visualiser visualiser = new Visualiser(jungGraph, customVertexRenderers, layoutSizeX, layoutSizeY, displaySizeX, displaySizeY);
//
//    JungGraphFrame frame = new JungGraphFrame(visualiser);
//    frame.getFrame().setVisible(true);
//    return frame;
//  }

//  private void updateDisplay(GraphFrame frame, GraphCursor graphCursor) throws JGraphXPopulationException, GraphCursorException {
//    EntanglementMxGraph mxGraph = new EntanglementMxGraph();
//    GraphCursorImmediateNeighbourhoodToJGraphX populator = new GraphCursorImmediateNeighbourhoodToJGraphX(mxGraph);
//    populator.populateImmediateNeighbourhood(graphConn, graphCursor);
////      doOrganicLayout(mxGraph);
//    doFastOrganicLayout(mxGraph);
//    frame.displayNewGraph(graphCursor.getName(), mxGraph);
//
//  }


  public CustomRendererRegistry getCustomVertexRenderers() {
    return customVertexRenderers;
  }

  public void setCustomVertexRenderers(CustomRendererRegistry customVertexRenderers) {
    this.customVertexRenderers = customVertexRenderers;
  }

  public EntityDisplayNameRegistry getDisplayNameFactories() {
    return displayNameFactories;
  }

  public void setDisplayNameFactories(EntityDisplayNameRegistry displayNameFactories) {
    this.displayNameFactories = displayNameFactories;
  }
}
