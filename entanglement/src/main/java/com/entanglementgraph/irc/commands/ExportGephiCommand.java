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

import com.entanglementgraph.cli.export.MongoToGephiExporter;
import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.shell.EntanglementStatePropertyNames;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.OptionalParam;
import com.halfspinsoftware.uibot.Param;
import com.halfspinsoftware.uibot.RequiredParam;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import com.torrenttamer.mongodb.MongoDbFactoryException;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
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
public class ExportGephiCommand extends AbstractCommand<EntanglementRuntime> {

  private static final Color DEFAULT_COLOR = Color.BLACK;
  private static final String NODE_COLOR_PREFIX = "node.color.";
  private static final String EDGE_COLOR_PREFIX = "edge.color.";

  @Override
  public String getDescription() {
    return "Exports the current graph to Gephi format.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
//    params.add(new RequiredParam("type", String.class, "The type name of the edge to create/modify"));
//    params.add(new RequiredParam("entityName", String.class, "A unique name for the edge to create/modify"));
//    params.add(new OptionalParam("{ key=value pairs }", null, "A set of key=value pairs that will be added to the edge as attributes"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    GraphConnection graphConn = userObject.getCurrentConnection();
    if (graphConn == null) throw new UserException(sender, "No graph was set as the 'current' connection.");

    Map<String, Color> colorMappings = parseColoursFromEnvironment();
    bot.debugln(channel, "Found the following colour mappings: %s", colorMappings);


    try {
      File outputFile = new File(graphConn.getGraphName()+".gexf");

      MongoToGephiExporter exporter = new MongoToGephiExporter();
      exporter.addColourMappings(colorMappings);
      exporter.addEntireGraph(graphConn);
      exporter.writeToFile(outputFile);
      exporter.close();

      Message result = new Message(channel);
      result.println("Graph %s has been exported to a Gephi file: %s", graphConn.getGraphName(), outputFile.getAbsolutePath());
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

  public Map<String, Color> parseColoursFromEnvironment() {
    Map<String, Color> entityTypeToColor = new HashMap<>();
    for (Map.Entry<String, String> entry : state.getEnvironment().entrySet()) {
      String key = entry.getKey();
      String val = entry.getValue();
      if (key.startsWith(NODE_COLOR_PREFIX)) {
        entityTypeToColor.put(key.substring(NODE_COLOR_PREFIX.length()), stringToColor(val));
      } else if(key.startsWith(EDGE_COLOR_PREFIX)) {
        entityTypeToColor.put(key.substring(EDGE_COLOR_PREFIX.length()), stringToColor(val));
      }
    }

    return entityTypeToColor;
  }

  private static Color stringToColor(String colorString) {
    Color color;
    switch (colorString) {
      case "BLACK":
        color = Color.BLACK;
        break;
      case "BLUE":
        color = Color.BLUE;
        break;
      case "CYAN":
        color = Color.CYAN;
        break;
      case "DARK_GRAY":
        color = Color.DARK_GRAY;
        break;
      case "GRAY":
        color = Color.GRAY;
        break;
      case "GREEN":
        color = Color.GREEN;
        break;
      case "LIGHT_GRAY":
        color = Color.LIGHT_GRAY;
        break;
      case "MAGENTA":
        color = Color.MAGENTA;
        break;
      case "ORANGE":
        color = Color.ORANGE;
        break;
      case "PINK":
        color = Color.PINK;
        break;
      case "RED":
        color = Color.RED;
        break;
      case "WHITE":
        color = Color.WHITE;
        break;
      case "YELLOW":
        color = Color.YELLOW;
        break;
      default:
        color = DEFAULT_COLOR;
    }

    return color;
  }
}
