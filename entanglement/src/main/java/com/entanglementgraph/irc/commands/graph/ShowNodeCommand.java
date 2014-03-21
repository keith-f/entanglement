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

import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.commands.AbstractEntanglementGraphCommand;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.List;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
public class ShowNodeCommand extends AbstractEntanglementGraphCommand {
  private String type;
  private String entityName;

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

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    type = parsedArgs.get("type").getStringValue();
    entityName = parsedArgs.get("entityName").getStringValue();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      // Create a keyset in order to query the database
      EntityKeys keyset = new EntityKeys(type, entityName);
      // This method returns a raw MongoDB object. For real-world applications, you would often parse this into a Java bean
      Node node = graphConn.getNodeDao().getByKey(keyset);

      if (node == null) {
        logger.println("A graph entity of type %s with name %s could not be found!", type, entityName);
      } else {
        logger.println("Found the following entity with properties:");
        logger.println("[");
        logger.println(String.format("%s%s%s%s", Colors.BOLD, Colors.RED, node.toString(), Colors.NORMAL));
        logger.println("]");
      }
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
