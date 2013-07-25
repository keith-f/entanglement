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

package com.entanglementgraph.irc.commands.swing;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.cursor.GraphCursorException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.jgraphx.EntanglementMxGraph;
import com.entanglementgraph.visualisation.jgraphx.GraphFrame;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;
import com.mxgraph.view.mxGraph;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.*;

/**
 * This command opens a JFrame and tracks a specified cursor by displaying lists of incoming/outgoing edges of the
 * current node, as well as a JGraphX-rendered image of the immediate neighbourhood.
 *
 * Note; this command will only work where a graphical display is present!
 *
 * @author Keith Flanagan
 */
public class CreateSwingCursorNearestNeighboursCommand extends AbstractEntanglementCommand<EntanglementRuntime> {


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

    params.add(new OptionalParam("layout-force-constant", Double.class, "500", "The higher this value is, the more separated the nodes become."));
    params.add(new OptionalParam("layout-min-distance-limit", Double.class, "10", "Unknown..."));
    params.add(new OptionalParam("layout-initial-temp", Double.class, "10", "Unknown..."));

    return params;
  }

  public CreateSwingCursorNearestNeighboursCommand() {
    super(Requirements.GRAPH_CONN_NEEDED, Requirements.CURSOR_NEEDED);
  }

  private double layoutForceConstant;
  private double layoutMinDistanceLimit;
  private double layoutInitialTemp;

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
//    boolean displayEdgeCounts = parsedArgs.get("display-edge-counts").parseValueAsBoolean();
//    boolean displayEdgeTypes = parsedArgs.get("display-edge-types").parseValueAsBoolean();
//    boolean verbose = parsedArgs.get("verbose").parseValueAsBoolean();
    int maxUids = parsedArgs.get("maxUids").parseValueAsInteger();
    int maxNames = parsedArgs.get("maxNames").parseValueAsInteger();
    boolean track = parsedArgs.get("track").parseValueAsBoolean();

    layoutForceConstant = parsedArgs.get("layout-force-constant").parseValueAsDouble();
    layoutMinDistanceLimit = parsedArgs.get("layout-min-distance-limit").parseValueAsDouble();
    layoutInitialTemp = parsedArgs.get("layout-initial-temp").parseValueAsDouble();



    try {
      final GraphFrame frame = displayGraphInNewFrame(cursor);

      if (track) {
        state.getUserObject().getCursorRegistry().getCurrentPositions().addEntryListener(new EntryListener<String, GraphCursor>() {
          @Override
          public void entryAdded(EntryEvent<String, GraphCursor> event) {
          }

          @Override
          public void entryRemoved(EntryEvent<String, GraphCursor> event) {
          }

          @Override
          public void entryUpdated(EntryEvent<String, GraphCursor> event) {
            GraphCursor newCursor = event.getValue();
            try {
              updateDisplay(frame, newCursor);
            } catch (Exception e) {
              bot.printException(channel, sender, "Failed to update GUI for new cursor position", e);
              e.printStackTrace();
            }
          }

          @Override
          public void entryEvicted(EntryEvent<String, GraphCursor> event) {
          }
        }, cursorName, true); //Receive updates for our cursor only.
      }

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

  private GraphFrame displayGraphInNewFrame(GraphCursor graphCursor) throws JGraphXPopulationException, GraphCursorException {
    EntanglementMxGraph mxGraph = new EntanglementMxGraph();
    GraphCursorImmediateNeighbourhoodToJGraphX populator = new GraphCursorImmediateNeighbourhoodToJGraphX(mxGraph);
    populator.populateImmediateNeighbourhood(graphConn, graphCursor);
//      doOrganicLayout(mxGraph);
    doFastOrganicLayout(mxGraph);
    GraphFrame frame = new GraphFrame(cursorName, mxGraph);
    frame.getFrame().setVisible(true);
    return frame;
  }

  private void updateDisplay(GraphFrame frame, GraphCursor graphCursor) throws JGraphXPopulationException, GraphCursorException {
    EntanglementMxGraph mxGraph = new EntanglementMxGraph();
    GraphCursorImmediateNeighbourhoodToJGraphX populator = new GraphCursorImmediateNeighbourhoodToJGraphX(mxGraph);
    populator.populateImmediateNeighbourhood(graphConn, graphCursor);
//      doOrganicLayout(mxGraph);
    doFastOrganicLayout(mxGraph);
    frame.displayNewGraph(graphCursor.getName(), mxGraph);

  }

  private void doOrganicLayout(mxGraph graph) {
    // Define layout
    mxOrganicLayout layout = new mxOrganicLayout(graph);


    // layout graph
    layout.execute(graph.getDefaultParent());
  }

  @SuppressWarnings("UnusedDeclaration")
  private void doFastOrganicLayout(mxGraph graph) {
    // Define layout
    mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);

    //Layout with single layout
    // set some properties
    layout.setForceConstant(layoutForceConstant); // the higher, the more separated
    layout.setDisableEdgeStyle(false); // true transforms the edges and makes them direct lines

    layout.setMinDistanceLimit(layoutMinDistanceLimit);
    layout.setInitialTemp(layoutInitialTemp);
    layout.setDisableEdgeStyle(true);

    // layout graph
    layout.execute(graph.getDefaultParent());
  }

  @SuppressWarnings("UnusedDeclaration")
  private void doOrthogonalLayout(mxGraph graph) {
    // Define layout
    mxOrthogonalLayout layout = new mxOrthogonalLayout(graph);

    // layout graph
    layout.execute(graph.getDefaultParent());
  }




}
