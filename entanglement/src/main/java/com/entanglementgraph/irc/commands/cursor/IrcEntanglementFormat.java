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
import com.scalesinformatics.uibot.IrcFormat;
import org.jibble.pircbot.Colors;




/**
 *
 * @author Keith Flanagan
 */
public class IrcEntanglementFormat extends IrcFormat {

  public static String COLOR_HISTORY_GENERIC = Colors.OLIVE;

  public static String COLOR_MOVEMENT_TYPE = Colors.CYAN;

  public static String COLOR_NODE_ANNOUNCE = Colors.GREEN+Colors.BOLD;
  public static String COLOR_NODE_KEY = Colors.GREEN +Colors.BOLD;
  public static String COLOR_NODE_VALUE = Colors.GREEN;

  public static String COLOR_EDGE_ANNOUNCE = Colors.REVERSE+Colors.BOLD;
  public static String COLOR_EDGE_KEY = Colors.OLIVE +Colors.BOLD;
  public static String COLOR_EDGE_VALUE = Colors.OLIVE;

  public static String COLOR_EDGE_ARROW = Colors.OLIVE;
  public static String COLOR_EDGE_ARROW_TEXT = Colors.OLIVE;

  public static String COLOR_HOSTNAME = Colors.RED;
//
//  public IrcFormat nodeKey() {
//    text.append(String.format("%s<%s", pushFormat(COLOR_NODE_KEY), popFormat()));
//    return this;
//  }
//
//  public IrcFormat nodeVal() {
//    text.append(String.format("%s<%s", pushFormat(COLOR_LIST_BRACKET), popFormat()));
//    return this;
//  }
  /**
   * Pretty-prints an EntityKeys representing a node using IRC-compatible colours/formatting
   * @param node
   * @return
   */
  public IrcEntanglementFormat formatNodeKeyset(EntityKeys<? extends Node> node) {
    openSquareBracket().customFormat("Node", COLOR_NODE_ANNOUNCE).append(" ");
    if (node.getType() != null) {
      pushFormat(COLOR_NODE_KEY).append("Type: ").popFormat();
      pushFormat(COLOR_NODE_VALUE).append(node.getType()).append("; ").popFormat();
    }
    if (!node.getUids().isEmpty()) {
      pushFormat(COLOR_NODE_KEY).append("UIDs: ").popFormat();
      pushFormat(COLOR_NODE_VALUE).format(node.getUids()).popFormat();
      append("; ");
    }
    if (!node.getNames().isEmpty()) {
      pushFormat(COLOR_NODE_KEY).append("Names: ").popFormat();
      pushFormat(COLOR_NODE_VALUE).format(node.getNames()).popFormat();
    }
    closeSquareBracket();
    return this;
  }

  public IrcEntanglementFormat formatNodeKeysetShort(EntityKeys<? extends Node> node, int maxUids, int maxNames) {

    openSquareBracket().customFormat(node.getType(), COLOR_NODE_ANNOUNCE).append("; ");

    if (!node.getUids().isEmpty()) {
      pushFormat(COLOR_NODE_KEY).append("ID:").popFormat();
      pushFormat(COLOR_NODE_VALUE).format(node.getUids(), maxUids).popFormat().append("; ");
    }
    if (!node.getNames().isEmpty()) {
      pushFormat(COLOR_NODE_KEY).append("ns:").popFormat();
      pushFormat(COLOR_NODE_VALUE).format(node.getNames(), maxNames).popFormat();
    }
    closeSquareBracket();

    return this;
  }

  /**
   * Pretty-prints an EntityKeys representing an edge using IRC-compatible colours/formatting
   * @param edge
   * @return
   */
  public IrcEntanglementFormat formatEdgeKeyset(EntityKeys<? extends Edge> edge) {
    openSquareBracket().customFormat("Edge", COLOR_EDGE_ANNOUNCE).append(" ");
    if (edge.getType() != null) {
      pushFormat(COLOR_EDGE_KEY).append("Type: ").popFormat();
      pushFormat(COLOR_EDGE_VALUE).append(edge.getType()).append("; ").popFormat();
    }
    if (!edge.getUids().isEmpty()) {
      pushFormat(COLOR_EDGE_KEY).append("UIDs: ").popFormat();
      pushFormat(COLOR_EDGE_VALUE).format(edge.getUids()).popFormat();
      append("; ");
    }
    if (!edge.getNames().isEmpty()) {
      pushFormat(COLOR_EDGE_KEY).append("Names: ").popFormat();
      pushFormat(COLOR_EDGE_VALUE).format(edge.getNames()).popFormat();
    }
    closeSquareBracket();
    return this;
  }

  /**
   * Pretty-prints an edge IRC-compatible colours/formatting. Both connecting nodes are printed with an ASCII-art
   * link.
   * @param edge
   * @return
   */
  public IrcEntanglementFormat formatEdge(Edge edge) {
    openSquareBracket();
    formatNodeKeysetShort(edge.getFrom(), 0, 1);
    pushFormat(COLOR_EDGE_ARROW).append("---")
        .customFormat(edge.getKeys().getType(), COLOR_EDGE_ARROW_TEXT)
        .append("--->").popFormat();
//    IrcEntanglementFormat tmpFormat = new IrcEntanglementFormat();
//    append(String.format("  ---%s--->  ",
//        tmpFormat.customFormat(edge.getKeys().getType(), COLOR_EDGE_ARROW_TEXT).toString()));
//    popFormat();
    formatNodeKeysetShort(edge.getTo(), 0, 1);

    closeSquareBracket();
    return this;
  }

//  public IrcEntanglementFormat formatEdgeArrow(String arrowText, boolean leftArrow, boolean rightArrow) {
//
//    pushFormat(COLOR_EDGE_ARROW);
//    if (leftArrow) append("<");
//    append(String.format("---%s---", customFormat(arrowText, COLOR_EDGE_ARROW_TEXT)));
//    if (rightArrow) append(">");
//    popFormat();
//    return this;
//  }

  public IrcEntanglementFormat formatCursorName(String cursorName) {
    return (IrcEntanglementFormat) customFormat(cursorName, COLOR_MOVEMENT_TYPE);
  }

  public IrcEntanglementFormat formatMovementType(GraphCursor.MovementTypes type) {
    return (IrcEntanglementFormat) customFormat(type.name(), COLOR_MOVEMENT_TYPE);
  }

  public IrcEntanglementFormat formatHistoryIndex(int index) {
    return (IrcEntanglementFormat) pushFormat(COLOR_HISTORY_GENERIC).append(index).popFormat();
  }

  public IrcEntanglementFormat formatHost(String hostname) {
    return (IrcEntanglementFormat) customFormat(hostname, COLOR_HOSTNAME);
  }

//  public IrcEntanglementFormat printCursorMovementText(GraphCursor previous, GraphCursor current) {
//    String outputText = String.format("Cursor %s moved from %s to %s. Movement type %s",
//        new IrcEntanglementFormat().formatCursorName(current.getName()),
//        new IrcEntanglementFormat().formatNodeKeysetShort(previous.getPosition(), maxUids, maxNames),
//        new IrcEntanglementFormat().formatNodeKeysetShort(current.getPosition(), maxUids, maxNames),
//        new IrcEntanglementFormat().formatMovementType(current.getMovementType()));
//  }

}
