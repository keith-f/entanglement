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
import com.entanglementgraph.util.GraphConnection;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.OptionalParam;
import com.halfspinsoftware.uibot.Param;
import com.halfspinsoftware.uibot.RequiredParam;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
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
    params.add(new OptionalParam("node-type", String.class, "The type name of the node to jump to."));
    params.add(new OptionalParam("node-name", String.class, "The unique name of the node to jump to."));
    params.add(new OptionalParam("node-uid", String.class, "The UID of the node to jump to."));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String cursorName = parsedArgs.get("cursor").getStringValue();
    String nodeType = parsedArgs.get("node-type").getStringValue();
    String nodeName = parsedArgs.get("node-name").getStringValue();
    String nodeUid = parsedArgs.get("node-uid").getStringValue();

    GraphCursor cursor = getSpecifiedCursorOrDefault(userObject, cursorName);
    EntityKeys<? extends Node> newLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);

    try {
      cursor = cursor.jump(newLocation);

      GraphCursor.HistoryItem current = cursor.getHistory().get(cursor.getCursorHistoryIdx());
      GraphCursor.HistoryItem previous = cursor.getHistory().get(cursor.getCursorHistoryIdx()-1);

      String outputText = String.format("Cursor %s moved from %s to %s. Movement type %s",
          formatCursorName(cursorName),
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
