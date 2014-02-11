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

import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.irc.data.GraphConnectionDetails;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

/**
 * @author Keith Flanagan
 */
public class ConnectGraphCommand extends AbstractEntanglementCommand {
  private String connectionName;
  private String clusterName;
  private String database;
  private String graph;

  @Override
  public String getDescription() {
    return "Registers a named set of graph connection details. These details may be used by other commands to create " +
        "graph connections when required";
  }

  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("conn", String.class, "A unique name to use for this connection object"));
    params.add(new RequiredParam("cluster", String.class,
        "The name of a MongoDB connection pool/cluster name (as created by the 'connect MongoDB cluster' command. "));
    params.add(new RequiredParam("database", String.class, "A database located within a MongoDB pool."));
    params.add(new RequiredParam("graph", String.class, "Name of the Entanglement graph to use"));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    connectionName = parsedArgs.get("conn").getStringValue();
    clusterName = parsedArgs.get("cluster").getStringValue();
    database = parsedArgs.get("database").getStringValue();
    graph = parsedArgs.get("graph").getStringValue();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      GraphConnectionDetails details = new GraphConnectionDetails(clusterName, database, graph);
      entRuntime.registerGraphConnectionDetails(connectionName, details);
      logger.println("Graph %s on %s is now available with connection name: %s", graph, clusterName, connectionName);
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
