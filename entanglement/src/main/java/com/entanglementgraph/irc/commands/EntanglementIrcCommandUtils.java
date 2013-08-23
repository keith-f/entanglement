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

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

/**
 * @author Keith Flanagan
 */
public class EntanglementIrcCommandUtils {
  /**
   * A convenience method that creates and returns either a connection named <code>connName</code>, or
   * if <code>connName</code> is NULL, then creates and returns a connection for the bot's current "active" connection.
   *
   * @param runtime
   * @param connName
   * @return
   * @throws UserException
   * @throws BotCommandException
   */
  public static GraphConnection getSpecifiedGraphOrDefault(EntanglementRuntime runtime, String connName)
      throws UserException, BotCommandException {
    if (connName != null) {
      return runtime.createGraphConnectionFor(connName);
    }
    return runtime.createGraphConnectionForCurrentConnection();
  }

  /**
   * A convenience method that creates and returns a connection named <code>connName</code>, or NULL if
   * <code>connName</code> was also NULL.
   * Note that an exception will be thrown if <code>connName</code> is non-null, but no connection information
   * with that name could be found.
   *
   * @param runtime
   * @param connName
   * @return
   * @throws UserException
   * @throws BotCommandException
   */
  public static GraphConnection getSpecifiedGraph(EntanglementRuntime runtime, String connName)
      throws UserException, BotCommandException {
    GraphConnection conn = runtime.createGraphConnectionFor(connName);
    if (conn == null) {
      throw new UserException("Graph connection not found: %s", connName);
    }
    return conn;
  }

  /**
   * A convenience method that returns either a cursor named <code>cursorName</code>, or if <code>cursorName</code>
   * is NULL, then returns the current bot's current "active" cursor.
   *
   * @param runtime
   * @param cursorName
   * @return
   * @throws UserException
   * @throws BotCommandException
   */
  public static GraphCursor getSpecifiedCursorOrDefault(EntanglementRuntime runtime, String cursorName)
      throws UserException {

    GraphCursor graphCursor = null;
    if (cursorName != null) {
      graphCursor = runtime.getCursorRegistry().getCursorCurrentPosition(cursorName);
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
