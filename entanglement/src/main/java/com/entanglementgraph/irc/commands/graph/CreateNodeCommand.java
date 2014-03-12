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

import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.commands.AbstractEntanglementGraphCommand;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.commands.NodeUpdate;
import com.entanglementgraph.specialistnodes.MapContent;
import com.entanglementgraph.util.TxnUtils;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.*;

/**
 * @author Keith Flanagan
 */
public class CreateNodeCommand extends AbstractEntanglementGraphCommand {
  private String type;
  private String entityName;

  private Map<String, String> attributes;

  @Override
  public String getDescription() {
    return "Creates or updates a node in the currently active graph.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("type", String.class, "The type name of the node to create/modify"));
    params.add(new RequiredParam("entityName", String.class, "A unique name for the node to create/modify"));
    params.add(new OptionalParam("{ key=value pairs }", null, "A set of key=value pairs that will be added to the node as attributes"));

    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    type = parsedArgs.get("type").getStringValue();
    entityName = parsedArgs.get("entityName").getStringValue();

    // Parse annotations
    attributes = parseAttributes(args);
    //FIXME do this properly - remove entries that are used as part of the command.
    attributes.remove("type");
    attributes.remove("entityName");
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      bot.infoln(channel, "Going to create a node: %s/%s with properties: %s", type, entityName, attributes);

      Node<MapContent> node = new Node();
      node.getKeys().setType(type);
      node.getKeys().addUid(entityName);

      // Add further custom properties
      MapContent content = new MapContent();
      node.setContent(content);
      for (Map.Entry<String, String> attr : attributes.entrySet()) {
        content.getMap().put(attr.getKey(), attr.getValue());
      }

      ; //FIXME policy should be user-configurable
      NodeUpdate nodeUpdateCommand = new NodeUpdate(node);
      TxnUtils.submitAsTxn(graphConn, nodeUpdateCommand);
      logger.println("Node created/updated: %s", entityName);
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

  private Map<String, String> parseAttributes(String[] args) {
    Map<String, String> attributes = new HashMap<>();
    for (String arg : args) {
      if (arg.contains("=")) {
        try {
          StringTokenizer st = new StringTokenizer(arg, "=");
          String name = st.nextToken();
          String value = st.nextToken();
          attributes.put(name, value);
        } catch (Exception e) {
          bot.errln(channel, "Incorrectly formatted key=value pair. Ignoring: %s", arg);
        }
      }
    }
    return attributes;
  }

}
