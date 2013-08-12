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
import com.entanglementgraph.cursor.GraphCursorException;
import com.entanglementgraph.export.DepthBasedSubgraphCreator;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.iteration.GraphIteratorException;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.specialistnodes.XYChartNode;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.jung.*;
import com.entanglementgraph.visualisation.jung.renderers.CategoryDatasetChartRenderer;
import com.entanglementgraph.visualisation.jung.renderers.XYDatasetChartRenderer;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import com.scalesinformatics.util.UidGenerator;
import edu.uci.ics.jung.graph.Graph;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.*;

/**
 * This command opens a JFrame and tracks a specified cursor by displaying lists of incoming/outgoing edges of the
 * current node, as well as a Jung-rendered image of the immediate neighbourhood.
 *
 * Note; this command will only work where a graphical display is present!
 *
 * @author Keith Flanagan
 */
public class CreateJungVizForCursorNearestNeighboursCommand extends AbstractEntanglementCommand<EntanglementRuntime> {
  private static final Logger logger = Logger.getLogger(CreateJungVizForCursorNearestNeighboursCommand.class.getName());

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

    params.add(new RequiredParam("temp-cluster", String.class, "The name of a configured MongoDB cluster to use for storing temporary graphs."));
    params.add(new OptionalParam("temp-database", String.class, "temp", "The name of a database to use for storing temporary graphs."));

    params.add(new OptionalParam("layout-size-x", Integer.class, "800", "The width (in pixels) that the node layout engine will use for performing layouts."));
    params.add(new OptionalParam("layout-size-y", Integer.class, "800", "The height (in pixels) that the node layout engine will use for performing layouts."));
    params.add(new OptionalParam("display-size-x", Integer.class, "850", "The preferred width (in pixels) of the graph viewport."));
    params.add(new OptionalParam("display-size-y", Integer.class, "850", "The preferred height (in pixels) of the graph viewport."));

    return params;
  }

  public CreateJungVizForCursorNearestNeighboursCommand() {
    super(Requirements.GRAPH_CONN_NEEDED, Requirements.CURSOR_NEEDED);
  }

  private String tempCluster;
  private String tempDatabase;

  private int layoutSizeX;
  private int layoutSizeY;
  private int displaySizeX;
  private int displaySizeY;

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    tempCluster = parsedArgs.get("temp-cluster").getStringValue();
    tempDatabase = parsedArgs.get("temp-database").getStringValue();

    int maxUids = parsedArgs.get("maxUids").parseValueAsInteger();
    int maxNames = parsedArgs.get("maxNames").parseValueAsInteger();
    boolean track = parsedArgs.get("track").parseValueAsBoolean();

    layoutSizeX = parsedArgs.get("layout-size-x").parseValueAsInteger();
    layoutSizeY = parsedArgs.get("layout-size-y").parseValueAsInteger();
    displaySizeX = parsedArgs.get("display-size-x").parseValueAsInteger();
    displaySizeY = parsedArgs.get("display-size-y").parseValueAsInteger();

    try {
      // Manually fire the first event just in case we happen to be on a Gene node at the moment
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
      msg.println("Cursor %s is currently located at: %s; Dead end? %s; Steps taken: %s",
          cursor.getName(), formatNodeKeyset(currentPos), formatBoolean(isAtDeadEnd), formatHistoryIndex(historyIdx));
      msg.println("Short version: %s", formatNodeKeysetShort(currentPos, maxUids, maxNames));


      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
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
//      GraphCursor eventCursor = event.getValue();
//      if (!eventCursor.getName().equals(cursorName)) {
//        logger.info(String.format("Received an event of a cursor that we're not interested in: %s != %s. Ignoring.",
//            eventCursor.getName(), cursorName));
//        return;
//      }
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
//              updateDisplay(frame, newCursor);
      GraphConnection destGraph = exportSubgraph(newCursor);
      JungGraphFrame newFrame = displayGraphInNewFrame(destGraph);
    } catch (Exception e) {
      bot.printException(channel, sender, "Failed to update GUI for new cursor position", e);
      e.printStackTrace();
    }
  }

  private GraphConnection exportSubgraph(GraphCursor graphCursor) throws GraphIteratorException, GraphConnectionFactoryException {
    GraphConnection sourceGraph = graphConn;
    GraphConnection destinationGraph = createTemporaryGraphConnection();
    DepthBasedSubgraphCreator exporter = new DepthBasedSubgraphCreator(
        sourceGraph, destinationGraph, state.getUserObject(), cursorContext, 1);

    //Use a throwaway cursor so as not to alter the cursor we're listening to, which may have unintended side effects
    GraphCursor tmpCursor = new GraphCursor(UidGenerator.generateUid(), graphCursor.getPosition());
    state.getUserObject().getCursorRegistry().addCursor(tmpCursor);
    exporter.execute(tmpCursor);
    state.getUserObject().getCursorRegistry().removeCursor(tmpCursor);
    return destinationGraph;
  }

  private GraphConnection createTemporaryGraphConnection() throws GraphConnectionFactoryException {
    GraphConnectionFactory factory = new GraphConnectionFactory(tempCluster, tempDatabase);
    return factory.connect("tmp_"+UidGenerator.generateUid(), "temp");
  }

  private JungGraphFrame displayGraphInNewFrame(GraphConnection subgraph) throws GraphCursorException, DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {
    // Export the temporary Entanglement graph to an in-memory Jung graph
    MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
    dbToJung.addEntireGraph(subgraph);
    Graph<DBObject, DBObject> jungGraph = dbToJung.getGraph();

    // Add a custom renderer for chart nodes
    CustomRendererRegistry customVertexRenderers = new CustomRendererRegistry(subgraph.getMarshaller());
    customVertexRenderers.addTypeToRendererMapping(CategoryChartNode.getTypeName(), CategoryDatasetChartRenderer.class);
    customVertexRenderers.addTypeToRendererMapping(XYChartNode.getTypeName(), XYDatasetChartRenderer.class);


    Visualiser visualiser = new Visualiser(jungGraph, customVertexRenderers, layoutSizeX, layoutSizeY, displaySizeX, displaySizeY);

    JungGraphFrame frame = new JungGraphFrame(visualiser);
    frame.getFrame().setVisible(true);
    return frame;
  }

//  private void updateDisplay(GraphFrame frame, GraphCursor graphCursor) throws JGraphXPopulationException, GraphCursorException {
//    EntanglementMxGraph mxGraph = new EntanglementMxGraph();
//    GraphCursorImmediateNeighbourhoodToJGraphX populator = new GraphCursorImmediateNeighbourhoodToJGraphX(mxGraph);
//    populator.populateImmediateNeighbourhood(graphConn, graphCursor);
////      doOrganicLayout(mxGraph);
//    doFastOrganicLayout(mxGraph);
//    frame.displayNewGraph(graphCursor.getName(), mxGraph);
//
//  }



}
