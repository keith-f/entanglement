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
import com.entanglementgraph.irc.commands.swing.JGraphXPopulationException;
import com.entanglementgraph.iteration.GraphIteratorException;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.jung.*;
import com.entanglementgraph.visualisation.jung.renderers.CategoryLineChartRenderer;
import com.mongodb.BasicDBObject;
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


  @Override
  public String getDescription() {
    return "Displays information about the cursor's immediate surroundings.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
//    params.add(new OptionalParam("display-edge-counts", Boolean.class, "true", "If set 'true', will display incoming/outgoing edge counts."));
//    params.add(new OptionalParam("display-edge-types", Boolean.class, "true", "If set 'true', will display edge type information under the edge counts."));
//    params.add(new OptionalParam("verbose", Boolean.class, "false", "If set 'true', will display all edge information as well as the summary."));
    params.add(new OptionalParam("maxUids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("maxNames", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("track", Boolean.class, Boolean.TRUE.toString(), "Specifies whether the GUI should track cursor events and update itself when the cursor moves to a new position."));

    params.add(new RequiredParam("temp-cluster", String.class, "The name of a configured MongoDB cluster to use for storing temporary graphs."));
    params.add(new OptionalParam("temp-database", String.class, "temp", "The name of a database to use for storing temporary graphs."));

    return params;
  }

  public CreateJungVizForCursorNearestNeighboursCommand() {
    super(Requirements.GRAPH_CONN_NEEDED, Requirements.CURSOR_NEEDED);
  }

  private String tempCluster;
  private String tempDatabase;

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    tempCluster = parsedArgs.get("temp-cluster").getStringValue();
    tempDatabase = parsedArgs.get("temp-database").getStringValue();

    int maxUids = parsedArgs.get("maxUids").parseValueAsInteger();
    int maxNames = parsedArgs.get("maxNames").parseValueAsInteger();
    boolean track = parsedArgs.get("track").parseValueAsBoolean();

    try {
      // Display initial graph around current cursor position
      GraphConnection destGraph = exportSubgraph(cursor);
      final JungGraphFrame frame = displayGraphInNewFrame(destGraph);

//
//      if (track) {
//        state.getUserObject().getCursorRegistry().getCurrentPositions().addEntryListener(new EntryListener<String, GraphCursor>() {
//          @Override
//          public void entryAdded(EntryEvent<String, GraphCursor> event) {
//          }
//
//          @Override
//          public void entryRemoved(EntryEvent<String, GraphCursor> event) {
//          }
//
//          @Override
//          public void entryUpdated(EntryEvent<String, GraphCursor> event) {
//            GraphCursor newCursor = event.getValue();
//            try {
//              updateDisplay(frame, newCursor);
//            } catch (Exception e) {
//              bot.printException(channel, sender, "Failed to update GUI for new cursor position", e);
//              e.printStackTrace();
//            }
//          }
//
//          @Override
//          public void entryEvicted(EntryEvent<String, GraphCursor> event) {
//          }
//        }, cursorName, true); //Receive updates for our cursor only.
//      }

      Message msg = new Message(channel);

      EntityKeys<? extends Node> currentPos = cursor.getPosition();
      DBObject currentNodeObj = null;
      if (!cursor.isAtDeadEnd()) {
        currentNodeObj = cursor.resolve(graphConn);
        currentPos = MongoUtils.parseKeyset(graphConn.getMarshaller(), (BasicDBObject) currentNodeObj);
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

  private GraphConnection exportSubgraph(GraphCursor graphCursor) throws GraphIteratorException, GraphConnectionFactoryException {
    GraphConnection sourceGraph = graphConn;
    GraphConnection destinationGraph = createTemporaryGraphConnection();
    DepthBasedSubgraphCreator exporter = new DepthBasedSubgraphCreator(
        sourceGraph, destinationGraph, state.getUserObject(), cursorContext, 1);
    exporter.execute(graphCursor);
    return destinationGraph;
  }

  private GraphConnection createTemporaryGraphConnection() throws GraphConnectionFactoryException {
    GraphConnectionFactory factory = new GraphConnectionFactory(tempCluster, tempDatabase);
    return factory.connect("tmp_"+ UidGenerator.generateUid(), "temp");
  }

  private JungGraphFrame displayGraphInNewFrame(GraphConnection subgraph) throws JGraphXPopulationException, GraphCursorException, DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {
    // Export the temporary Entanglement graph to an in-memory Jung graph
    MongoToJungGraphExporter dbToJung = new MongoToJungGraphExporter();
    dbToJung.addEntireGraph(subgraph);
    Graph<DBObject, DBObject> jungGraph = dbToJung.getGraph();

    // Add a custom renderer for chart nodes
    CustomRendererRegistry customVertexRenderers = new CustomRendererRegistry(subgraph.getMarshaller());
    customVertexRenderers.addTypeToRendererMapping(CategoryChartNode.getTypeName(), CategoryLineChartRenderer.class);

    Visualiser visualiser = new Visualiser(jungGraph, customVertexRenderers);

    JungGraphFrame frame = new JungGraphFrame(visualiser);
    frame.getFrame().setVisible(true);
    return frame;
  }


}
