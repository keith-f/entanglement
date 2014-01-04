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
import com.entanglementgraph.util.GraphConnectionFactory;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

/**
 * A utility for creating database connection pools.
 *
 * @author Keith Flanagan
 */
public class ConnectMongoDbClusterCommand extends AbstractEntanglementCommand {

  private static final int DEFAULT_MONGODB_PORT = 27017;

  private String poolName;
  private String rawHosts;

  @Override
  public String getDescription() {
    return "Creates a connection pool for a specified MongoDB cluster replica set. " +
        "This pool can be named and subsequently used to create graph connections. " +
        "The 'master' node is determined automatically by MongoDB.";
  }

  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("pool", String.class, "A unique name to use for this connection pool."));
    params.add(new RequiredParam("hosts", String.class, "A comma-separated list of hostname:port of the members of the cluster's replica set"));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    poolName = parsedArgs.get("pool").getStringValue();
    rawHosts = parsedArgs.get("hosts").getStringValue();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      List<ServerAddress> servers = new LinkedList<>();
      for (String hostPort : rawHosts.split(",")) {
        String hostname = hostPort;
        int port = DEFAULT_MONGODB_PORT;
        if (hostPort.contains(":")) {
          String[] hostPortTokens = hostPort.split(":");
          hostname = hostPortTokens[0];
          port = Integer.parseInt(hostPortTokens[1]);
        }

        ServerAddress server = new ServerAddress(hostname, port);
        servers.add(server);
      }

      MongoClient pool = GraphConnectionFactory.registerNamedPool(
          poolName, servers.toArray(new ServerAddress[servers.size()]));

      logger.println("Connection pool '%s' is now available, containing %d servers and %d databases.",
          poolName, pool.getAllAddress().size(), pool.getDatabaseNames().size());
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
