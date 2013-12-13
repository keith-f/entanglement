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

import com.entanglementgraph.irc.commands.AbstractEntanglementGraphCommand;
import com.entanglementgraph.visualisation.gephi.MongoToGephiExporter;
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
public class ExportGephiCommand extends AbstractEntanglementGraphCommand {

  private static final Color DEFAULT_COLOR = Color.BLACK;
  private static final String NODE_COLOR_PREFIX = "node.color.";
  private static final String EDGE_COLOR_PREFIX = "edge.color.";

  private Map<String, Color> colorMappings;

  @Override
  public String getDescription() {
    return "Exports an Entanglement graph to Gephi format.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    colorMappings = parseColoursFromEnvironment(state);
    bot.debugln(channel, "Found the following colour mappings: %s", colorMappings);
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      File outputFile = new File(graphConn.getGraphName()+".gexf");

      MongoToGephiExporter exporter = new MongoToGephiExporter();
      exporter.addColourMappings(colorMappings);
      exporter.addEntireGraph(graphConn);
      exporter.writeToFile(outputFile);
      exporter.close();

      logger.println("Graph %s has been exported to a Gephi file: %s", graphConn.getGraphName(), outputFile.getAbsolutePath());
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

  public static Map<String, Color> parseColoursFromEnvironment(BotState state) {
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
