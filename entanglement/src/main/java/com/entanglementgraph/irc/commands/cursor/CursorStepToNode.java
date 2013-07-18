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

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.*;

/**
 * Steps the graph cursor to a directly connected node.
 *
 * @author Keith Flanagan
 */
public class CursorStepToNode extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Steps the graph cursor to a directly connected node. The node may be at the end of either an incoming or " +
        "outgoing edge. You may specify a node UID, or type/name, or even just a type or a name as long as the they " +
        "are not ambiguous.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new OptionalParam("cursor", String.class, "The name of the cursor to use. If not specified, the default cursor will be used"));
    params.add(new OptionalParam("conn", String.class, "Graph connection to use. If no connection name is specified, "
        + "the 'current' connection will be used."));
    params.add(new OptionalParam("node-type", String.class, "The type name of the node to jump to."));
    params.add(new OptionalParam("node-name", String.class, "The unique name of the node to jump to."));
    params.add(new OptionalParam("node-uid", String.class, "The UID of the node to jump to."));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String cursorName = parsedArgs.get("cursor").getStringValue();
    String connName = parsedArgs.get("conn").getStringValue();
    String nodeType = parsedArgs.get("node-type").getStringValue();
    String nodeName = parsedArgs.get("node-name").getStringValue();
    String nodeUid = parsedArgs.get("node-uid").getStringValue();

    BotState<EntanglementRuntime> state = channelState;
    EntanglementRuntime runtime = state.getUserObject();
    GraphConnection graphConn = EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault(runtime, connName);
    DbObjectMarshaller m = graphConn.getMarshaller();

    GraphCursor cursor = EntanglementIrcCommandUtils.getSpecifiedCursorOrDefault(runtime, cursorName);
    EntityKeys<? extends Node> newLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);

    try {
      cursor = cursor.stepToNode(graphConn, newLocation);

      GraphCursor.HistoryItem current = cursor.getHistory().get(cursor.getCursorHistoryIdx());
      GraphCursor.HistoryItem previous = cursor.getHistory().get(cursor.getCursorHistoryIdx()-1);

      String outputText = String.format("Cursor %s moved from %s to %s. Movement type %s",
          formatCursorName(cursor.getName()),
          formatNodeKeyset(previous.getDestination()),
          formatNodeKeyset(current.getDestination()),
          formatMovementType(current.getMovementType()));

      Message result = new Message(channel);
      result.println(outputText);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
