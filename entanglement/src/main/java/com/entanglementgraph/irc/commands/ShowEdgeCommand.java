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
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.Param;
import com.halfspinsoftware.uibot.ParamParser;
import com.halfspinsoftware.uibot.RequiredParam;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import com.mongodb.BasicDBObject;
import org.jibble.pircbot.Colors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ShowEdgeCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Pretty-prints a specified edge in the currently active graph.");
    return txt.toString();
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new RequiredParam("type", String.class, "The type name of the edge to display"));
    params.add(new RequiredParam("entityName", Integer.class, "A unique name of the edge to display"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String type = parsedArgs.get("type").getStringValue();
    String entityName = parsedArgs.get("entityName").getStringValue();

    GraphConnection graphConn = userObject.getCurrentConnection();
    if (graphConn == null) throw new UserException(sender, "No graph was set as the 'current' connection.");

    try {
      // Create a keyset in order to query the database
      EntityKeys keyset = new EntityKeys(type, entityName);
      // This method returns a raw MongoDB object. For real-world applications, you would often parse this into a Java bean
      BasicDBObject edge = graphConn.getEdgeDao().getByKey(keyset);

      Message result = new Message(channel);
      if (edge == null) {
        result.println("A graph entity of type %s with name %s could not be found!", type, entityName);
      } else {
        result.println("Found the following entity with properties:");
        result.println("[");
        Map edgeAsMap = edge.toMap();
        for (Object key : edgeAsMap.keySet()) {
          Object val = edgeAsMap.get(key);
          String keyStr = String.format("%s%s%s%s", Colors.BOLD, Colors.RED, key.toString(), Colors.NORMAL);
          String valStr = val.toString();
          result.println("  %s => %s", keyStr, valStr);
        }
        result.println("]");

      }


      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
