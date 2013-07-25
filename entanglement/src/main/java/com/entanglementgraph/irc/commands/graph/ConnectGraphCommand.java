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

package com.entanglementgraph.irc.commands.graph;

import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.data.GraphConnectionDetails;
import com.entanglementgraph.shell.EntanglementStatePropertyNames;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

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
    return "Registers a named set of graph connection details. These details may be used by other commands to create " +
        "graph connections when required";
  }

  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("conn", String.class, "A unique name to use for this connection object"));
    params.add(new RequiredParam("pool", String.class,
        "The name of a MongoDB connection pool name (as created by the 'connect MongoDB cluster' command. "));
    params.add(new RequiredParam("database", String.class, "A database located within a MongoDB pool."));
    params.add(new RequiredParam("graph", String.class, "Name of the Entanglement graph to use"));
    params.add(new OptionalParam("branch", String.class, "trunk",
        "Name of the branch to use (defaults to 'trunk', if not specified)"));
    return params;
  }


  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connectionName = parsedArgs.get("conn").getStringValue();
    String poolName = parsedArgs.get("pool").getStringValue();
    String database = parsedArgs.get("database").getStringValue();
    String graph = parsedArgs.get("graph").getStringValue();
    String branch = parsedArgs.get("branch").getStringValue();

    EntanglementRuntime runtime = state.getUserObject();

    try {
      GraphConnectionDetails details = new GraphConnectionDetails(poolName, database, graph, branch);
//      GraphConnection connection = connect(runtime, hostname, database, graph, branch);
      runtime.registerGraphConnectionDetails(connectionName, details);
      Message result = new Message(channel);
      result.println("Graph %s on %s is now available with connection name: %s", graph, poolName, connectionName);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

//  public GraphConnection connect(EntanglementRuntime runtime,
//                                 String hostname, String database, String graphName, String branchName)
//      throws RevisionLogException, MongoDbFactoryException, GraphConnectionFactoryException {
//    ClassLoader classLoader = runtime.getClassLoader();
//
//    GraphConnectionFactory factory = new GraphConnectionFactory(classLoader, hostname, database);
//    GraphConnection connection = factory.connect(graphName, branchName);
//    bot.debugln(channel, "Connected to: %s/%s!", graphName, branchName);
//    return connection;
//  }
}