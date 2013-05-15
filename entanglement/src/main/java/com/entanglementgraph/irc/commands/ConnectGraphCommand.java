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
import com.halfspinsoftware.uibot.*;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import com.torrenttamer.mongodb.MongoDbFactoryException;
import com.torrenttamer.util.ExceptionUtils;

import java.util.LinkedList;
import java.util.List;

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
    return "Creates a new named connection entry in the Entanglement runtime object.";
  }

  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new RequiredParam("conn", String.class, "A unique name to use for this connection object"));
    params.add(new OptionalParam("hostname", String.class, "The hostname of a MongoDB server. Optional if you have " +
        "already specified this in the environment variable: "+EntanglementStatePropertyNames.PROP_HOSTNAME));
    params.add(new OptionalParam("database", String.class, "A database located on a MongoDB server. Optional if you have " +
        "already specified this in the environment variable: "+EntanglementStatePropertyNames.PROP_DB_NAME));
    params.add(new RequiredParam("graph", String.class, "Name of the Entanglement graph to use"));
    params.add(new OptionalParam("branch", String.class, "Name of the branch to use (defaults to 'trunk', if not specified)"));
    return params;
  }


  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connectionName = ParamParser.findStringValueOf(args, "conn");
    String hostname = ParamParser.findStringValueOf(args, "hostname", state.getEnvironment().get(EntanglementStatePropertyNames.PROP_HOSTNAME));
    String database = ParamParser.findStringValueOf(args, "database", state.getEnvironment().get(EntanglementStatePropertyNames.PROP_DB_NAME));
    String graph = ParamParser.findStringValueOf(args, "graph");
    String branch = ParamParser.findStringValueOf(args, "branch", "trunk");

    if (connectionName == null) throw new UserException(sender, "You forgot to specify a name for this connection.");
    if (hostname == null) throw new UserException(sender, "You forgot to specify a hostname to your MongoDB server.");
    if (database == null) throw new UserException(sender, "You forgot to specify a database name on your MongoDB server.");
    if (graph == null) throw new UserException(sender, "You forgot to specify an Entanglement graph to connect to.");
    if (branch == null) throw new UserException(sender, "You forgot to specify an Entanglement graph branch name.");

    try {
      GraphConnection connection = connect(hostname, database, graph, branch);
      userObject.addGraphConnection(connectionName, connection);
      Message result = new Message(channel);
      result.println("Graph %s on %s is now available with connection name: %s", graph, hostname, connectionName);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
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
