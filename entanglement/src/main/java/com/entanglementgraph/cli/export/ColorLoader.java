package com.entanglementgraph.cli.export;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 12/06/13
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
public class ColorLoader {

  private static final Color DEFAULT_COLOR = Color.BLACK;

  @SuppressWarnings("UnusedDeclaration")
  public static Map<String, Color> loadColorMappings(File propFile)
      throws IOException {
    Properties props;
    try (FileInputStream is = new FileInputStream(propFile)) {
      props = new Properties();
      props.load(is);
    }

    Map<String, Color> nodeTypeToColour = new HashMap<>();
    for (String nodeType : props.stringPropertyNames()) {
      String colorString = props.getProperty(nodeType);
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


      nodeTypeToColour.put(nodeType, color);
    }
    return nodeTypeToColour;
  }

}
