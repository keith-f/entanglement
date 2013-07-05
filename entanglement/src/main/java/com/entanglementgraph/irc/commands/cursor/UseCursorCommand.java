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
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.Param;
import com.halfspinsoftware.uibot.RequiredParam;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class UseCursorCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Sets the currently active graph cursor for shell commands.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new RequiredParam("cursor", String.class, "The name of the cursor that is to be set 'active'"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String cursorName = parsedArgs.get("cursor").getStringValue();

    try {
      GraphCursor cursor = userObject.getGraphCursors().get(cursorName);
      if (cursor == null) {
        throw new UserException(sender, "No graph cursor exists with the name: "+cursorName);
      }

      userObject.setCurrentCursor(cursor);

      Message result = new Message(channel);
      result.println("Current cursor set to: %s", cursorName);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}