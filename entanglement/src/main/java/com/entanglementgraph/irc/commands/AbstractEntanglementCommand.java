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
import com.scalesinformatics.uibot.BotState;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

/**
 * A partial command implementation that is useful for many Entanglement-based IRC commands. Includes support for
 * parsing graph connection names and cursor names from command lines. Graph connections can either be acquired
 * from the command line (if present), or whatever is currently the default specified in this channel's EntanglementRuntime.
 *
 * @author Keith Flanagan
 */
abstract public class AbstractEntanglementCommand extends AbstractCommand<EntanglementRuntime> {

  private boolean graphConnNeeded = false;
  private boolean graphCursorNeeded = false;


  protected String graphConnName;
  protected GraphConnection graphConn;
  protected String cursorName;
  protected GraphCursor cursor;



  protected static enum Requirements {
    GRAPH_CONN_NEEDED,
    CURSOR_NEEDED
  };


  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    if (graphConnNeeded) {
      params.add(new OptionalParam("conn", String.class, "Graph connection to use. If no connection name is specified, the 'current' connection will be used."));
    }
    if (graphCursorNeeded) {
      params.add(new OptionalParam("cursor", String.class, "The name of the cursor to use. If not specified, the default cursor will be used"));
    }
    return params;
  }


  protected AbstractEntanglementCommand(Requirements... requirements)
  {
    for (Requirements req : requirements) {
      switch (req) {
        case GRAPH_CONN_NEEDED:
          graphConnNeeded = true;
          break;
        case CURSOR_NEEDED:
          graphCursorNeeded = true;
          break;
      }
    }
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    if (graphConnNeeded) {
      graphConnName = parsedArgs.get("conn").getStringValue();
      graphConn = EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault(state.getUserObject(), graphConnName);
    }
    if (graphCursorNeeded) {
      cursorName = parsedArgs.get("cursor").getStringValue();
      cursor = EntanglementIrcCommandUtils.getSpecifiedCursorOrDefault(state.getUserObject(), cursorName);
    }
  }

}
