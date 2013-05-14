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
import com.halfspinsoftware.uibot.BotCommand;
import com.halfspinsoftware.uibot.BotState;
import com.halfspinsoftware.uibot.GenericIrcBot;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.torrenttamer.mongodb.MongoDbFactoryException;
import com.torrenttamer.util.ExceptionUtils;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class ConnectGraphCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Creates a new named connection entry in the Entanglement runtime object.");
    return txt.toString();
  }

  @Override
  public String getHelpText() {
    StringBuilder txt = new StringBuilder();
    txt.append("USAGE:\n");
    txt.append("Either:\n");
    txt.append("  * Graph connection name\n");
    txt.append("  * Hostname\n");
    txt.append("  * Database name\n");
    txt.append("  * Graph name\n");
    txt.append("  * Graph branch\n");
    txt.append("Or:\n");
    txt.append("  * Graph connection name\n");
    txt.append("  * Graph name\n");
    txt.append("  * Graph branch\n");
    txt.append("  (in this case, MongoDB hostname and database values are assumed to be in environment properties.\n");

    return txt.toString();
  }

  @Override
  public Message call() throws Exception {
    Message result = new Message(channel);
    String connectionName = null;
    String hostname = null;
    String database = null;
    String graph = null;
    String branch = null;
    try {
      if (args.length == 3) {
        hostname = state.getEnvironment().get(EntanglementStatePropertyNames.PROP_HOSTNAME);
        database = state.getEnvironment().get(EntanglementStatePropertyNames.PROP_DB_NAME);
        if (hostname == null) {
          throw new EntanglementBotException("Environment variable was not set: "+EntanglementStatePropertyNames.PROP_HOSTNAME);
        }
        if (database == null) {
          throw new EntanglementBotException("Environment variable was not set: "+EntanglementStatePropertyNames.PROP_DB_NAME);
        }
        connectionName = args[0];
        graph = args[1];
        branch = args[2];
      } else {
        connectionName = args[0];
        hostname = args[1];
        database = args[2];
        graph = args[3];
        branch = args[4];
      }

      GraphConnection connection = connect(hostname, database, graph, branch);
      userObject.addGraphConnection(connectionName, connection);
      result.println("Graph %s on %s is now available with connection name: %s", graph, hostname, connectionName);
    } catch (Exception e) {
      bot.printException(errChannel, "WARNING: an Exception occurred while processing.", e);
    } finally {
      return result;
    }
  }

  public GraphConnection connect(String hostname, String database, String graphName, String branchName)
      throws RevisionLogException, MongoDbFactoryException, GraphConnectionFactoryException {
    ClassLoader classLoader = userObject.getClassLoader();

    GraphConnectionFactory factory = new GraphConnectionFactory(classLoader, hostname, database);
    GraphConnection connection = factory.connect(graphName, branchName);
    bot.debugln(channel, "Connected to: %s/%s!", graphName, branchName);
    return connection;
  }
}
