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

import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.ParamParser;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import com.mongodb.BasicDBObject;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ListEdgesCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Lists edge(s) in the currently active graph.");
    return txt.toString();
  }

  @Override
  public String getHelpText() {
    StringBuilder txt = new StringBuilder();
    txt.append("USAGE:\n");
    txt.append("The following key=value parameter pairs are supported:\n");
    txt.append("  * type=<type name> - Specifies the type of graph entity to display (optional)\n");
    txt.append("  * offset=<Integer> - Specifies the number of entities to skip (optional)\n");
    txt.append("  * limit=<Integer> - Specifies the maximum number of entities to display (optional)\n");

    return txt.toString();
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String type = ParamParser.findStringValueOf(args, "type", null);
    Integer offset = ParamParser.findIntegerValueOf(args, "offset", 0);
    Integer limit = ParamParser.findIntegerValueOf(args, "limit", Integer.MAX_VALUE);

    // The parameters parsed (above) are all optional, so no need to throw a UserException(s) if one or more are null.

    GraphConnection graphConn = userObject.getCurrentConnection();
    if (graphConn == null) throw new UserException("No graph was set as the 'current' connection.");

    int count = 0;
    try {
      if (type == null) {
        for (EntityKeys keys : graphConn.getEdgeDao().iterateKeys(offset, limit)) {
          count++;
          BasicDBObject edgeObj = graphConn.getEdgeDao().getByKey(keys);
          Edge edge = graphConn.getMarshaller().deserialize(edgeObj, Edge.class);

          String fromStr = edge.getFrom().getType()+"/"+edge.getFrom().getNames();
          String toStr = edge.getTo().getType()+"/"+edge.getTo().getNames();

          bot.debugln(channel, "  * "+fromStr+"   ------ "+edge.getKeys().getType() + " ----->> " + toStr);
        }
      } else {
        for (EntityKeys keys : graphConn.getNodeDao().iterateKeysByType(type, offset, limit)) {
          count++;
          BasicDBObject edgeObj = graphConn.getEdgeDao().getByKey(keys);
          Edge edge = graphConn.getMarshaller().deserialize(edgeObj, Edge.class);

          String fromStr = edge.getFrom().getType()+"/"+edge.getFrom().getNames();
          String toStr = edge.getTo().getType()+"/"+edge.getTo().getNames();

          bot.debugln(channel, "  * "+fromStr+"   ------ "+edge.getKeys().getType() + " ----->> " + toStr);
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
