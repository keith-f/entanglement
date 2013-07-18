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
  public static GraphConnection getSpecifiedGraphOrDefault(EntanglementRuntime runtime, String connName)
      throws UserException, BotCommandException {
    if (connName != null) {
      return runtime.createGraphConnectionFor(connName);
    }
    return runtime.createGraphConnectionForCurrentConnection();
  }

  public static GraphCursor getSpecifiedCursorOrDefault(EntanglementRuntime runtime, String cursorName)
      throws UserException {

    GraphCursor graphCursor = null;
    if (cursorName != null) {
      graphCursor = runtime.getGraphCursor(cursorName);
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
