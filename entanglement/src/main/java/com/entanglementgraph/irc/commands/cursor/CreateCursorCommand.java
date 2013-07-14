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
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import com.mongodb.BasicDBObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class CreateCursorCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Creates a new graph cursor of the specified name. You must choose a start node for this cursor; this may " +
        "EITHER specified by a node UID, OR by a node's type and name properties. You should not normally specify both " +
        "a UID *and* a type+name.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new RequiredParam("cursor", String.class, "Name of the new cursor"));
    params.add(new OptionalParam("conn", String.class, "Graph connection to use. If no connection name is specified, " +
        "the 'current' connection will use."));
    params.add(new OptionalParam("node-type", String.class, "Type of initial node"));
    params.add(new OptionalParam("node-name", String.class, "Name of initial node"));
    params.add(new OptionalParam("node-uid", String.class, "UID of initial node"));
    return params;
  }



  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String cursorName = parsedArgs.get("cursor").getStringValue();
    String connName = parsedArgs.get("conn").getStringValue();
    String nodeType = parsedArgs.get("node-type").getStringValue();
    String nodeName = parsedArgs.get("node-name").getStringValue();
    String nodeUid = parsedArgs.get("node-uid").getStringValue();

    GraphConnection graphConn = getSpecifiedGraphOrDefault(userObject, connName);

    EntityKeys<? extends Node> nodeLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);

    BotLogger logger = new BotLogger(bot, channel, cursorName, cursorName);
    logger.infoln("Created new graph cursor: %s at location: %s",cursorName, nodeLocation);

    try {
      GraphCursor newCursor = new GraphCursor(logger, graphConn, nodeLocation);
      userObject.getGraphCursors().put(cursorName, newCursor);

      String outputText = String.format("New cursor %s created at node: %s",
          formatCursorName(cursorName),
          formatNodeKeyset(nodeLocation));

      Message result = new Message(channel);
      result.println(outputText);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }


  }


}
