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
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 04/07/2013
 * Time: 22:10
 * To change this template use File | Settings | File Templates.
 */
public class CursorCommandUtils {

  public static String COLOR_MOVEMENT_TYPE = Colors.CYAN;
  public static String COLOR_NODE = Colors.GREEN;

  public static String formatNodeKeyset(EntityKeys<? extends Node> node) {
    StringBuilder sb = new StringBuilder();
    sb.append(COLOR_NODE);
    if (node.getType() != null) {
      sb.append("Type: ").append(node.getType());
    }
    if (node.getUids().isEmpty()) {
      sb.append("UIDs: ").append(node.getUids());
    }
    if (node.getNames().isEmpty()) {
      sb.append("names: ").append(node.getNames());
    }
    sb.append(Colors.NORMAL);
    return sb.toString();
  }

  public static String formatCursorName(String cursorName) {
    return String.format("%s%s%s", COLOR_MOVEMENT_TYPE, cursorName, Colors.NORMAL);
  }

  public static String formatMovementType(GraphCursor.MovementTypes type) {
    return String.format("%s%s%s", COLOR_MOVEMENT_TYPE, type.name(), Colors.NORMAL);
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
