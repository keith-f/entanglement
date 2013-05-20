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

package com.entanglementgraph.irc.commands;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.OptionalParam;
import com.halfspinsoftware.uibot.Param;
import com.halfspinsoftware.uibot.ParamParser;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import com.mongodb.BasicDBObject;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ListNodesCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Lists node(s) in the currently active graph.");
    return txt.toString();
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new OptionalParam("type", String.class, "Specifies the type of graph entity to display"));
    params.add(new OptionalParam("offset", Integer.class, "Specifies the number of entities to skip"));
    params.add(new OptionalParam("limit", Integer.class, "Specifies the maximum number of entities to display"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String type = parsedArgs.get("type").getStringValue();
    int offset = Integer.parseInt(parsedArgs.get("offset").getStringValue());
    int limit = Integer.parseInt(parsedArgs.get("limit").getStringValue());

    GraphConnection graphConn = userObject.getCurrentConnection();
    if (graphConn == null) throw new UserException(sender, "No graph was set as the 'current' connection.");

    int count = 0;
    try {
      if (type == null) {
        for (EntityKeys keys : graphConn.getNodeDao().iterateKeys(offset, limit)) {
          count++;
          bot.debugln(channel, "  * %s: names: %s; UIDs: %s", keys.getType(), keys.getNames(), keys.getUids());
        }
      } else {
        for (EntityKeys keys : graphConn.getNodeDao().iterateKeysByType(type, offset, limit)) {
          count++;
          bot.debugln(channel, "  * %s: names: %s; UIDs: %s", keys.getType(), keys.getNames(), keys.getUids());
        }
      }

      Message result = new Message(channel);
      result.println("Printed %d nodes.", count);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }


}
