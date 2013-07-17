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
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.*;

/**
 * This command opens a JFrame and tracks a specified cursor by displaying lists of incoming/outgoing edges of the
 * current node, as well as a JGraphX-rendered image of the immediate neighbourhood.
 *
 * Note; this command will only work where a graphical display is present!
 *
 * @author Keith Flanagan
 */
public class SwingCursorTracker extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Displays information about the cursor's immediate surroundings.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new OptionalParam("conn", String.class, "Graph connection to use. If no connection name is specified, the 'current' connection will be used."));
    params.add(new OptionalParam("cursor", String.class, "The name of the cursor to use. If not specified, the default cursor will be used"));
    params.add(new OptionalParam("display-edge-counts", Boolean.class, "true", "If set 'true', will display incoming/outgoing edge counts."));
    params.add(new OptionalParam("display-edge-types", Boolean.class, "true", "If set 'true', will display edge type information under the edge counts."));
    params.add(new OptionalParam("verbose", Boolean.class, "false", "If set 'true', will display all edge information as well as the summary."));
    params.add(new OptionalParam("maxUids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("maxNames", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));

    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connName = parsedArgs.get("conn").getStringValue();
    String cursorName = parsedArgs.get("cursor").getStringValue();
    boolean displayEdgeCounts = parsedArgs.get("display-edge-counts").parseValueAsBoolean();
    boolean displayEdgeTypes = parsedArgs.get("display-edge-types").parseValueAsBoolean();
    boolean verbose = parsedArgs.get("verbose").parseValueAsBoolean();
    int maxUids = parsedArgs.get("maxUids").parseValueAsInteger();
    int maxNames = parsedArgs.get("maxNames").parseValueAsInteger();

    BotState<EntanglementRuntime> state = channelState;
    EntanglementRuntime runtime = state.getUserObject();
    GraphConnection graphConn = EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault(runtime, connName);
    DbObjectMarshaller m = graphConn.getMarshaller();

    GraphCursor cursor = EntanglementIrcCommandUtils.getSpecifiedCursorOrDefault(runtime, cursorName);

    boolean isAtDeadEnd = cursor.isAtDeadEnd();
    int historyIdx = cursor.getCursorHistoryIdx();

    try {

      Message msg = new Message(channel);

      EntityKeys<? extends Node> currentPos = cursor.getCurrentNode();
      DBObject currentNodeObj = null;
      if (!cursor.isAtDeadEnd()) {
        currentNodeObj = cursor.resolve(graphConn);
        currentPos = MongoUtils.parseKeyset(m, (BasicDBObject) currentNodeObj);
      }

      msg.println("Cursor %s is currently located at: %s; Dead end? %s; Steps taken: %s",
          cursor.getName(), formatNodeKeyset(currentPos), formatBoolean(isAtDeadEnd), formatHistoryIndex(historyIdx));
      msg.println("Short version: %s", formatNodeKeysetShort(currentPos, maxUids, maxNames));

      if (displayEdgeCounts) {
        /*
         * Incoming edges
         */
        msg.println("* Incoming edges: %s", format(cursor.countIncomingEdges(graphConn)));
        if (displayEdgeTypes) {
          Map<String, Long> typeToCount = graphConn.getEdgeDao().countEdgesByTypeToNode(cursor.getCurrentNode());
          msg.println("* Incoming edge types: %s", format(typeToCount));
        }
        if (verbose) {
          for (DBObject edgeObj : cursor.iterateIncomingEdges(graphConn)) {
//            msg.println("  <= %s", formatEdge(m.deserialize(edgeObj, Edge.class)));
            Edge edge = m.deserialize(edgeObj, Edge.class);
            msg.println("  %sthis%s <= %s: %s", Colors.CYAN, Colors.OLIVE,
                edge.getKeys().getType(), formatNodeKeysetShort(edge.getFrom(), maxUids, maxNames));
          }
        }

        /*
         * Outgoing edges
         */
        msg.println("* Outgoing edges: %s", format(cursor.countOutgoingEdges(graphConn)));
        if (displayEdgeTypes) {
          Map<String, Long> typeToCount = graphConn.getEdgeDao().countEdgesByTypeFromNode(cursor.getCurrentNode());
          msg.println("* Outgoing edge types: %s", format(typeToCount));
        }
        if (verbose) {
          for (DBObject edgeObj : cursor.iterateOutgoingEdges(graphConn)) {
//            msg.println("  => %s", formatEdge(m.deserialize(edgeObj, Edge.class)));
            Edge edge = m.deserialize(edgeObj, Edge.class);
            msg.println("  %sthis%s => %s: %s", Colors.CYAN, Colors.OLIVE,
                edge.getKeys().getType(), formatNodeKeysetShort(edge.getTo(), 1, 2));
          }
        }
      }



      Integer foo = null;
      msg.println("Test null: %s", format(foo));

      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }


}
