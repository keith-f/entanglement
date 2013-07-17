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

package com.entanglementgraph.irc.commands.imageexport;

import com.entanglementgraph.export.jgraphx.MongoToJGraphExporter;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ExportJGraphXCommand extends AbstractCommand<EntanglementRuntime> {

  private static final Color DEFAULT_COLOR = Color.BLACK;
  private static final String NODE_COLOR_PREFIX = "node.color.";
  private static final String EDGE_COLOR_PREFIX = "edge.color.";

  @Override
  public String getDescription() {
    return "Exports the current graph to JGraphX XML format.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new OptionalParam("conn", String.class, "Graph connection to use. If no connection name is specified, the 'current' connection will be used."));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connName = parsedArgs.get("conn").getStringValue();

    BotState<EntanglementRuntime> state = channelState;
    EntanglementRuntime runtime = state.getUserObject();
    GraphConnection graphConn = getSpecifiedGraphOrDefault(runtime, connName);

    Map<String, Color> colorMappings = parseColoursFromEnvironment(state);
    bot.debugln(channel, "Found the following colour mappings: %s", colorMappings);


    try {
      File outputFile = new File(graphConn.getGraphName()+".xml");

      MongoToJGraphExporter exporter = new MongoToJGraphExporter();
      exporter.addEntireGraph(graphConn);
      exporter.writeToJGraphXmlFile(outputFile);
      exporter.clearGraph();

      Message result = new Message(channel);
      result.println("Graph %s has been exported to a JGraphX file: %s", graphConn.getGraphName(), outputFile.getAbsolutePath());
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

  public static Map<String, Color> parseColoursFromEnvironment(BotState<EntanglementRuntime> state) {
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
