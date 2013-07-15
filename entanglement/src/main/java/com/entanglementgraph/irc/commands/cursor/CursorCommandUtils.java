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

package com.entanglementgraph.irc.commands.cursor;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 04/07/2013
 * Time: 22:10
 * To change this template use File | Settings | File Templates.
 */
public class CursorCommandUtils {

//  public static String COLOR_NULL = Colors.RED+Colors.BLACK+Colors.REVERSE;
  public static String COLOR_NULL = Colors.RED+Colors.CYAN+",02";
  public static String COLOR_BOOLEAN_TRUE = Colors.NORMAL+Colors.GREEN;
  public static String COLOR_BOOLEAN_FALSE = Colors.NORMAL+Colors.RED;
  public static String COLOR_LONG = Colors.NORMAL+Colors.CYAN;
  public static String COLOR_INT = Colors.NORMAL+Colors.CYAN;
  public static String COLOR_FLOAT = Colors.NORMAL+Colors.YELLOW;
  public static String COLOR_DOUBLE = Colors.NORMAL+Colors.YELLOW;

  public static String COLOR_LIST_BRACKET = Colors.NORMAL+Colors.YELLOW;
  public static String COLOR_LIST_ITEM_DEFAULT = Colors.NORMAL;


  public static String COLOR_HISTORY_GENERIC = Colors.NORMAL+Colors.OLIVE;

  public static String COLOR_MOVEMENT_TYPE = Colors.NORMAL+Colors.CYAN;

  public static String COLOR_NODE_ANNOUNCE = Colors.NORMAL+Colors.GREEN+Colors.BOLD;
  public static String COLOR_NODE_KEY = Colors.NORMAL+Colors.GREEN +Colors.BOLD;
  public static String COLOR_NODE_VALUE = Colors.NORMAL+Colors.GREEN;

  public static String COLOR_EDGE_ANNOUNCE = Colors.NORMAL+Colors.REVERSE+Colors.BOLD;
  public static String COLOR_EDGE_KEY = Colors.NORMAL+Colors.OLIVE +Colors.BOLD;
  public static String COLOR_EDGE_VALUE = Colors.NORMAL+Colors.OLIVE;

  public static String COLOR_EDGE_ARROW = Colors.NORMAL+Colors.OLIVE;
  public static String COLOR_EDGE_ARROW_TEXT = Colors.NORMAL+Colors.OLIVE;

  /**
   * Pretty-prints an EntityKeys representing a node using IRC-compatible colours/formatting
   * @param node
   * @return
   */
  public static String formatNodeKeyset(EntityKeys<? extends Node> node) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_NODE_ANNOUNCE).append("[Node:");
    sb.append(Colors.NORMAL).append(" ");
    if (node.getType() != null) {
      sb.append(COLOR_NODE_KEY).append("Type: ").append(COLOR_NODE_VALUE).append(node.getType());
      sb.append("; ");
    }
    if (!node.getUids().isEmpty()) {
      sb.append(COLOR_NODE_KEY).append("UIDs: ").append(COLOR_NODE_VALUE).append(node.getUids());
      sb.append("; ");
    }
    if (!node.getNames().isEmpty()) {
      sb.append(COLOR_NODE_KEY).append("Names: ").append(COLOR_NODE_VALUE).append(node.getNames());
    }
    sb.append(COLOR_NODE_ANNOUNCE).append("]");
    sb.append(Colors.NORMAL);
    return sb.toString();
  }

  public static String formatNodeKeysetShort(EntityKeys<? extends Node> node, int maxUids, int maxNames) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_NODE_ANNOUNCE).append("[");
    if (node.getType() != null) {
      sb.append(COLOR_NODE_VALUE).append(node.getType());
      sb.append("; ");
    }
    if (!node.getUids().isEmpty()) {
      sb.append(COLOR_NODE_KEY).append("ID:").
          append(COLOR_NODE_VALUE).append(format(node.getUids(), maxUids));
      sb.append("; ");
    }
    if (!node.getNames().isEmpty()) {
      sb.append(COLOR_NODE_KEY).append("ns:").
          append(COLOR_NODE_VALUE).append(format(node.getNames(), maxNames));
    }
    sb.append(COLOR_NODE_ANNOUNCE).append("]");
    sb.append(Colors.NORMAL);
    return sb.toString();
  }

  /**
   * Pretty-prints an EntityKeys representing an edge using IRC-compatible colours/formatting
   * @param node
   * @return
   */
  public static String formatEdgeKeyset(EntityKeys<? extends Node> node) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_EDGE_ANNOUNCE).append("[Edge:");
    sb.append(Colors.NORMAL).append(" ");
    if (node.getType() != null) {
      sb.append(COLOR_EDGE_KEY).append("Type: ").append(COLOR_EDGE_VALUE).append(node.getType());
      sb.append("; ");
    }
    if (!node.getUids().isEmpty()) {
      sb.append(COLOR_EDGE_KEY).append("UIDs: ").append(COLOR_EDGE_VALUE).append(node.getUids());
      sb.append("; ");
    }
    if (!node.getNames().isEmpty()) {
      sb.append(COLOR_EDGE_KEY).append("Names: ").append(COLOR_EDGE_VALUE).append(node.getNames());
    }
    sb.append(COLOR_EDGE_ANNOUNCE).append("]");
    sb.append(Colors.NORMAL);
    return sb.toString();
  }

  /**
   * Pretty-prints an edge IRC-compatible colours/formatting. Both connecting nodes are printed with an ASCII-art
   * link.
   * @param edge
   * @return
   */
  public static String formatEdge(Edge edge) {
    StringBuilder sb = new StringBuilder();

    sb.append("[  ");

    sb.append(formatNodeKeysetShort(edge.getFrom(), 0, 1));
    sb.append(String.format("  %s---%s%s%s--->  ",
        COLOR_EDGE_ARROW, COLOR_EDGE_ARROW_TEXT, edge.getKeys().getType(), COLOR_EDGE_ARROW));
    sb.append(formatNodeKeysetShort(edge.getTo(), 0, 1));

    sb.append("  ]");
    sb.append(Colors.NORMAL);
    return sb.toString();
  }

  public static String formatEdgeArrow(String arrowText, boolean leftArrow, boolean rightArrow) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_EDGE_ARROW);
    if (leftArrow) sb.append("<");
    sb.append(String.format("---%s%s%s---", COLOR_EDGE_ARROW_TEXT, arrowText, COLOR_EDGE_ARROW));
    if (rightArrow) sb.append(">");
    return sb.toString();
  }

  public static String formatCursorName(String cursorName) {
    return String.format("%s%s%s", COLOR_MOVEMENT_TYPE, cursorName, Colors.NORMAL);
  }

  public static String formatMovementType(GraphCursor.MovementTypes type) {
    return String.format("%s%s%s", COLOR_MOVEMENT_TYPE, type.name(), Colors.NORMAL);
  }

  public static String formatHistoryIndex(int index) {
    return String.format("%s%s%s", COLOR_HISTORY_GENERIC, index, Colors.NORMAL);
  }

  public static String formatBoolean(boolean bool) {
    return String.format("%s%s%s", bool ? COLOR_BOOLEAN_TRUE : COLOR_BOOLEAN_FALSE, bool, Colors.NORMAL);
  }
  public static String format(long number) {
    return String.format("%s%s%s", COLOR_LONG, number, Colors.NORMAL);
  }
  public static String format(int number) {
    return String.format("%s%s%s", COLOR_INT, number, Colors.NORMAL);
  }

  public static String format(float number) {
    return String.format("%s%s%s", COLOR_FLOAT, number, Colors.NORMAL);
  }
  public static String format(double number) {
    return String.format("%s%s%s", COLOR_DOUBLE, number, Colors.NORMAL);
  }
  public static String format(Object object) {
    if (object == null) {
      return String.format("%sNULL%s", COLOR_NULL, Colors.NORMAL);
    }
    return String.format("%s%s", Colors.NORMAL, object.toString());
  }

  public static <T> String format(List<T> list) {
    return format(list, Integer.MAX_VALUE);
  }

  public static <T> String format(List<T> list, final int maxItems) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_LIST_BRACKET).append("[");

    int count = 0;
    for (Iterator<T> itr = list.iterator(); itr.hasNext(); ) {
      T item = itr.next();
      if (count >= maxItems) {
        sb.append("..."); //Indicate that there are more items, even when we can't display them
        break;
      }
      sb.append(format(item));
      if (itr.hasNext()) {
        sb.append(COLOR_LIST_BRACKET).append(", ");
      }
      count++;
    }
    sb.append(COLOR_LIST_BRACKET).append("]");
    return sb.toString();
  }

  public static <T> String format(Set<T> set) {
    return format(set, Integer.MAX_VALUE);
  }

  public static <T> String format(Set<T> set, final int maxItems) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_LIST_BRACKET).append("[");

    int count = 0;
    for (Iterator<T> itr = set.iterator(); itr.hasNext(); ) {
      T item = itr.next();
      if (count >= maxItems) {
        sb.append("..."); //Indicate that there are more items, even when we can't display them
        break;
      }
      sb.append(format(item));
      if (itr.hasNext()) {
        sb.append(COLOR_LIST_BRACKET).append(", ");
      }
      count++;
    }
    sb.append(COLOR_LIST_BRACKET).append("]");
    return sb.toString();
  }

  public static <X, Y> String format(Map<X, Y> map) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_LIST_BRACKET).append("[");

    for (Iterator<Map.Entry<X, Y>> itr = map.entrySet().iterator(); itr.hasNext(); ) {
      Map.Entry<X, Y> entry = itr.next();

      sb.append(format(entry.getKey()));
      sb.append(COLOR_LIST_BRACKET).append(" ==> ");
      sb.append(format(entry.getValue()));
      if (itr.hasNext()) {
        sb.append(COLOR_LIST_BRACKET).append(", ");
      }
    }
    sb.append(COLOR_LIST_BRACKET).append("]");
    return sb.toString();
  }





  public static GraphConnection getSpecifiedGraphOrDefault(EntanglementRuntime runtime, String connName)
      throws UserException {

    GraphConnection graphConn = null;
    if (connName != null) {
      graphConn = runtime.getGraphConnections().get(connName);
    }
    if (graphConn == null) {
      graphConn = runtime.getCurrentConnection();
    }
    if (graphConn == null) {
      throw new UserException("Either (no graph connection name was specified, or the specified graph " +
          "connection didn't exist), and no graph was set as the 'current' default connection.");
    }
    return graphConn;
  }

  public static GraphCursor getSpecifiedCursorOrDefault(EntanglementRuntime runtime, String cursorName)
      throws UserException {

    GraphCursor graphCursor = null;
    if (cursorName != null) {
      graphCursor = runtime.getGraphCursors().get(cursorName);
    }
    if (graphCursor == null) {
      graphCursor = runtime.getCurrentCursor();
    }
    if (graphCursor == null) {
      throw new UserException("Either (no graph cursor name was specified, or the specified graph " +
          "cursor didn't exist), and no cursor was set as the 'current' default connection.");
    }
    return graphCursor;
  }


}
