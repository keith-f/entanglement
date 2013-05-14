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

import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import org.jibble.pircbot.Colors;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ListGraphConnectionsCommand extends AbstractCommand<EntanglementRuntime> {
  private static final String CURRENT_GRAPH_TXT = Colors.BROWN + "[Active]" + Colors.NORMAL;


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Lists known graph connections.");
    return txt.toString();
  }

  @Override
  public String getHelpText() {
    StringBuilder txt = new StringBuilder();
    txt.append("USAGE:\n");
    txt.append("No parameters required.");

    return txt.toString();
  }

  @Override
  public Message call() throws Exception {
    Message msg = new Message(channel);
    try {
      GraphConnection current = userObject.getCurrentConnection();
      msg.println("Graph connections [", channel);
      for (Map.Entry<String, GraphConnection> entry : userObject.getGraphConnections().entrySet()) {
        GraphConnection conn = entry.getValue();
        String currentText = current == conn ? CURRENT_GRAPH_TXT : "";
        msg.println("  %s => %s/%s; %s/%s %s", entry.getKey(),
            conn.getMongo().getAddress().getHost(),
            conn.getDb().getName(),
            conn.getGraphName(), conn.getGraphBranch(),
            currentText);
      }
      msg.println("]");
    } catch (Exception e) {
      bot.printException(errChannel, "WARNING: an Exception occurred while processing.", e);
    } finally {
      return msg;
    }
  }

}
