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

package com.entanglementgraph.irc.commands.cursor;

import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.*;


/**
 * A command for printing information about the cursor's current position.
 *
 * @author Keith Flanagan
 */
public class CursorDescribe extends AbstractEntanglementCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Prints information about the cursor's immediate surroundings.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new OptionalParam("display-edge-counts", Boolean.class, "true", "If set 'true', will display incoming/outgoing edge counts."));
    params.add(new OptionalParam("display-edge-types", Boolean.class, "true", "If set 'true', will display edge type information under the edge counts."));
    params.add(new OptionalParam("verbose", Boolean.class, "false", "If set 'true', will display all edge information as well as the summary."));
    params.add(new OptionalParam("display-max-uids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("display-max-names", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));

    return params;
  }

  public CursorDescribe() {
    super(Requirements.GRAPH_CONN_NEEDED, Requirements.CURSOR_NEEDED);
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    boolean displayEdgeCounts = parsedArgs.get("display-edge-counts").parseValueAsBoolean();
    boolean displayEdgeTypes = parsedArgs.get("display-edge-types").parseValueAsBoolean();
    boolean verbose = parsedArgs.get("verbose").parseValueAsBoolean();
    int maxUids = parsedArgs.get("display-max-uids").parseValueAsInteger();
    int maxNames = parsedArgs.get("display-max-names").parseValueAsInteger();

//    EntanglementRuntime runtime = state.getUserObject();
    DbObjectMarshaller m = graphConn.getMarshaller();

    boolean isAtDeadEnd = cursor.isAtDeadEnd();
    int historyIdx = cursor.getCursorHistoryIdx();

    try {

      Message msg = new Message(channel);

      EntityKeys<? extends Node> currentPos = cursor.getPosition();
      DBObject currentNodeObj = null;
      if (!cursor.isAtDeadEnd()) {
        currentNodeObj = cursor.resolve(graphConn);
        currentPos = MongoUtils.parseKeyset(m, (BasicDBObject) currentNodeObj);
      }

      msg.println("Cursor %s is currently located at: %s; Dead end? %s; Steps taken: %s",
          cursor.getName(), entFormat.formatNodeKeyset(currentPos), entFormat.formatBoolean(isAtDeadEnd),
          entFormat.formatHistoryIndex(historyIdx));
      msg.println("Short version: %s", entFormat.formatNodeKeysetShort(currentPos, maxUids, maxNames));

      if (displayEdgeCounts) {
        /*
         * Incoming edges
         */
        msg.println("* Incoming edges: %s", entFormat.format(cursor.countIncomingEdges(graphConn)));
        if (displayEdgeTypes) {
          Map<String, Long> typeToCount = graphConn.getEdgeDao().countEdgesByTypeToNode(cursor.getPosition());
          msg.println("* Incoming edge types: %s", entFormat.format(typeToCount));
        }
        if (verbose) {
          for (DBObject edgeObj : cursor.iterateIncomingEdges(graphConn)) {
//            msg.println("  <= %s", formatEdge(m.deserialize(edgeObj, Edge.class)));
            Edge edge = m.deserialize(edgeObj, Edge.class);
            msg.println("  %sthis%s <= %s: %s", Colors.CYAN, Colors.OLIVE,
                edge.getKeys().getType(), entFormat.formatNodeKeysetShort(edge.getFrom(), maxUids, maxNames));
          }
        }

        /*
         * Outgoing edges
         */
        msg.println("* Outgoing edges: %s", entFormat.format(cursor.countOutgoingEdges(graphConn)));
        if (displayEdgeTypes) {
          Map<String, Long> typeToCount = graphConn.getEdgeDao().countEdgesByTypeFromNode(cursor.getPosition());
          msg.println("* Outgoing edge types: %s", entFormat.format(typeToCount));
        }
        if (verbose) {
          for (DBObject edgeObj : cursor.iterateOutgoingEdges(graphConn)) {
//            msg.println("  => %s", formatEdge(m.deserialize(edgeObj, Edge.class)));
            Edge edge = m.deserialize(edgeObj, Edge.class);
            msg.println("  %sthis%s => %s: %s", Colors.CYAN, Colors.OLIVE,
                edge.getKeys().getType(), entFormat.formatNodeKeysetShort(edge.getTo(), 1, 2));
          }
        }
      }


      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }


}
