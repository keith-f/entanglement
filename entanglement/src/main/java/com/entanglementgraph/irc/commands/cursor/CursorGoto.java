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

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.*;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

/**
 * A command that sets a new graph cursor to a specified node. The new position can be completely unrelated to the
 * existing position. This command is effectively a 'goto' statement for a cursor.
 *
 * @author Keith Flanagan
 */
public class CursorGoto extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Sets a new position for a graph cursor. You can either specify UID or a type/name pair of the node to jump to.";
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

    params.add(new OptionalParam("display-max-uids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("display-max-names", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String cursorName = parsedArgs.get("cursor").getStringValue();
    String connName = parsedArgs.get("conn").getStringValue();
    String nodeType = parsedArgs.get("node-type").getStringValue();
    String nodeName = parsedArgs.get("node-name").getStringValue();
    String nodeUid = parsedArgs.get("node-uid").getStringValue();
    int maxUids = parsedArgs.get("display-max-uids").parseValueAsInteger();
    int maxNames = parsedArgs.get("display-max-names").parseValueAsInteger();

    BotState<EntanglementRuntime> state = channelState;
    EntanglementRuntime runtime = state.getUserObject();
    GraphConnection graphConn = EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault(runtime, connName);
    DbObjectMarshaller m = graphConn.getMarshaller();

    GraphCursor cursor = EntanglementIrcCommandUtils.getSpecifiedCursorOrDefault(runtime, cursorName);
    EntityKeys<? extends Node> newLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);

    try {
      cursor = cursor.jump(newLocation);

      GraphCursor.HistoryItem current = cursor.getHistory().get(cursor.getCursorHistoryIdx());
      GraphCursor.HistoryItem previous = cursor.getHistory().get(cursor.getCursorHistoryIdx()-1);

      String outputText = String.format("Cursor %s moved from %s to %s. Movement type %s",
          formatCursorName(cursor.getName()),
          formatNodeKeysetShort(previous.getDestination(), maxUids, maxNames),
          formatNodeKeysetShort(current.getDestination(), maxUids, maxNames),
          formatMovementType(current.getMovementType()));

      Message result = new Message(channel);
      result.println(outputText);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
