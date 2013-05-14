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
import com.halfspinsoftware.uibot.ParamParser;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
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
  public String getHelpText() {
    StringBuilder txt = new StringBuilder();
    txt.append("USAGE:\n");
    txt.append("The following key=value parameter pairs are supported:");
    txt.append("  * type=<type name> - Specifies the type of graph entity to display (optional)\n");
    txt.append("  * offset=<Integer> - Specifies the number of entities to skip (optional)\n");
    txt.append("  * limit=<Integer> - Specifies the maximum number of entities to display (optional)\n");

    return txt.toString();
  }

  @Override
  public Message call() throws Exception {
    Message result = new Message(channel);
    GraphConnection graphConn = userObject.getCurrentConnection();
    try {
      if (graphConn == null) {
        bot.errln(errChannel, "No graph was set as the 'current' connection.");
        return result;
      }

      String type = ParamParser.findStringValueOf(args, "type");
      int offset = ParamParser.findIntegerValueOf(args, "offset", 0);
      int limit = ParamParser.findIntegerValueOf(args, "limit", Integer.MAX_VALUE);

      if (type == null) {
        for (EntityKeys keys : graphConn.getNodeDao().iterateKeys(offset, limit)) {
          bot.debugln(channel, "  * %s: names: %s; UIDs: %s", keys.getType(), keys.getNames(), keys.getUids());
        }
      } else {
        for (EntityKeys keys : graphConn.getNodeDao().iterateKeysByType(type, offset, limit)) {
          bot.debugln(channel, "  * %s: names: %s; UIDs: %s", keys.getType(), keys.getNames(), keys.getUids());
        }
      }
    } catch (Exception e) {
      bot.printException(errChannel, "WARNING: an Exception occurred while processing.", e);
    } finally {
      return result;
    }
  }


}
