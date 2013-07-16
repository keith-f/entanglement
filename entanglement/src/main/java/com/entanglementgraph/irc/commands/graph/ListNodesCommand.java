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

package com.entanglementgraph.irc.commands.graph;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.*;

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.getSpecifiedGraphOrDefault;

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
    params.add(new OptionalParam("cursor", String.class, "The name of the cursor to use. If not specified, the default cursor will be used"));
    params.add(new OptionalParam("type", String.class, "Specifies the type of graph entity to display"));
    params.add(new OptionalParam("offset", Integer.class, "0", "Specifies the number of entities to skip"));
    params.add(new OptionalParam("limit", Integer.class, String.valueOf(Integer.MAX_VALUE), "Specifies the maximum number of entities to display"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connName = parsedArgs.get("conn").getStringValue();
    String type = parsedArgs.get("type").getStringValue();
    int offset = Integer.parseInt(parsedArgs.get("offset").getStringValue());
    int limit = Integer.parseInt(parsedArgs.get("limit").getStringValue());

    BotState<EntanglementRuntime> state = channelState;
    EntanglementRuntime runtime = state.getUserObject();
    GraphConnection graphConn = getSpecifiedGraphOrDefault(runtime, connName);

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
