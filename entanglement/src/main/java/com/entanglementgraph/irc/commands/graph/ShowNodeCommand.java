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
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import com.mongodb.BasicDBObject;
import org.jibble.pircbot.Colors;

import java.util.*;

import static com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ShowNodeCommand extends AbstractEntanglementCommand {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Pretty-prints a specified node in the currently active graph.");
    return txt.toString();
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("type", String.class, "The type name of the node to display"));
    params.add(new RequiredParam("entityName", String.class, "A unique name of the node to display"));
    return params;
  }

  public ShowNodeCommand() {
    super(AbstractEntanglementCommand.Requirements.GRAPH_CONN_NEEDED);
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connName = parsedArgs.get("conn").getStringValue();
    String type = parsedArgs.get("type").getStringValue();
    String entityName = parsedArgs.get("entityName").getStringValue();

    try {
      // Create a keyset in order to query the database
      EntityKeys keyset = new EntityKeys(type, entityName);
      // This method returns a raw MongoDB object. For real-world applications, you would often parse this into a Java bean
      BasicDBObject node = graphConn.getNodeDao().getByKey(keyset);

      Message result = new Message(channel);
      if (node == null) {
        result.println("A graph entity of type %s with name %s could not be found!", type, entityName);
      } else {
        result.println("Found the following entity with properties:");
        result.println("[");
        Map nodeAsMap = node.toMap();
        for (Object key : nodeAsMap.keySet()) {
          Object val = nodeAsMap.get(key);
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
