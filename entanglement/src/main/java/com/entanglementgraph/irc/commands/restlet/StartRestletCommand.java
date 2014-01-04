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

import com.entanglementgraph.irc.commands.AbstractEntanglementGraphCommand;
import com.entanglementgraph.restlet.EntanglementRestServer;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;
import org.restlet.routing.Route;

import java.net.InetAddress;
import java.util.List;

/**
 * @author Keith Flanagan
 */
public class StartRestletCommand extends AbstractEntanglementGraphCommand {
  private int port;

  @Override
  public String getDescription() {
    return "Starts a RESTlet server for exposing the specified graph via a Web-based interface.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new OptionalParam("port", Integer.class, "4000", "Port for the server"));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    port = parsedArgs.get("port").parseValueAsInteger();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      logger.println("Going to start a REST interface for the graph: %s", graphConn.getGraphName());

      EntanglementRestServer server = new EntanglementRestServer(port, entRuntime, graphConn);

      String hostname = InetAddress.getLocalHost().getHostName();

      logger.println("Server started. Available routes are:");
      for (Route route : server.getComponent().getDefaultHost().getRoutes()) {
        entFormat.bullet().append(" ").append("http://").append(hostname).append(":").append(port);
        entFormat.pushFormat(Colors.CYAN).append(route.toString()).popFormat();
        logger.println(entFormat.toString());
      }

      server.getComponent().start();
      logger.println("Done.");
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
