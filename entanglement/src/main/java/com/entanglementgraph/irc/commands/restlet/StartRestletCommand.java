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

package com.entanglementgraph.irc.commands.restlet;

import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.restlet.EntanglementRestServer;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;
import org.restlet.routing.Route;

import java.net.InetAddress;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class StartRestletCommand extends AbstractEntanglementCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Starts a RESTlet server for exposing the specified graph via a Web-based interface.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
//    params.add(new RequiredParam("type", String.class, "The type name of the node to create/modify"));
//    params.add(new RequiredParam("entityName", String.class, "A unique name for the node to create/modify"));
//    params.add(new OptionalParam("{ key=value pairs }", null, "A set of key=value pairs that will be added to the node as attributes"));
    params.add(new OptionalParam("port", Integer.class, "4000", "Port for the server"));
    return params;
  }

  public StartRestletCommand() {
    super(Requirements.GRAPH_CONN_NEEDED);
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    int port = parsedArgs.get("port").parseValueAsInteger();

    EntanglementRuntime runtime = state.getUserObject();


    try {
      logger.println("Going to start a REST interface for the graph: %s", graphConn.getGraphName());

      EntanglementRestServer server = new EntanglementRestServer(port, runtime, graphConn);

      String hostname = InetAddress.getLocalHost().getHostName();

      logger.println("Server started. Available routes are:");
      for (Route route : server.getComponent().getDefaultHost().getRoutes()) {
        entFormat.bullet().append(" ").append("http://").append(hostname).append(":").append(port);
        entFormat.pushFormat(Colors.CYAN).append(route.toString()).popFormat();
        logger.println(entFormat.toString());
      }

      server.getComponent().start();
      logger.println("Done.");
      return null;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
