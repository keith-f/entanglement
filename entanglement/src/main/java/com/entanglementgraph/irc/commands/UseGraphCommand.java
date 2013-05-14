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

import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.shell.EntanglementStatePropertyNames;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.torrenttamer.mongodb.MongoDbFactoryException;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class UseGraphCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Sets the currently active graph connection for shell commands.");
    return txt.toString();
  }

  @Override
  public String getHelpText() {
    StringBuilder txt = new StringBuilder();
    txt.append("USAGE:\n");
    txt.append("  * Graph connection name\n");

    return txt.toString();
  }

  @Override
  public Message call() throws Exception {
    Message result = new Message(channel);
    String connectionName = null;
    try {
      connectionName = args[0];

      GraphConnection conn = userObject.getGraphConnections().get(connectionName);
      if (conn == null) {
        bot.errln(errChannel, "No such graph connection: %s", connectionName);
        return result;
      }

      userObject.setCurrentConnection(conn);
      result.println("Current graph set to: %s", connectionName);
    } catch (Exception e) {
      bot.printException(errChannel, "WARNING: an Exception occurred while processing.", e);
    } finally {
      return result;
    }
  }

}
