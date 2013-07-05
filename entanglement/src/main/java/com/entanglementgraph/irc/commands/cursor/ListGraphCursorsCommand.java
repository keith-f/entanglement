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
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.Param;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ListGraphCursorsCommand extends AbstractCommand<EntanglementRuntime> {
  private static final String CURRENT_CURSOR_TXT = Colors.BROWN + "[Active]" + Colors.NORMAL;


  @Override
  public String getDescription() {
    return "Lists known graph cursors.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    Message msg = new Message(channel);
    try {
      GraphCursor current = userObject.getCurrentCursor();
      msg.println("Graph cursors [");
      for (Map.Entry<String, GraphCursor> entry : userObject.getGraphCursors().entrySet()) {
        GraphCursor cursor = entry.getValue();
        String currentText = current == current ? CURRENT_CURSOR_TXT : "";
        msg.println("  %s => %s %s",
            entry.getKey(),
            cursor.getCurrentNode().toString(),
            currentText);
      }
      msg.println("]");
      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
