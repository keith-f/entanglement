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

import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

/**
 * An Entanglement command implementation that requires that a graph connection name is specified.
 *
 * @author Keith Flanagan
 */
abstract public class AbstractEntanglementGraphCommand extends AbstractEntanglementCommand {
  protected String connName;
  protected GraphConnection graphConn;

  protected String tempClusterName; // The name of a database pool to use for short-lived graphs.

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("conn", String.class, "Name of the graph connection to use"));
    params.add(new OptionalParam("temp-cluster", String.class,
        "The name of a configured database cluster to use for storing temporary graphs. Some commands require a" +
            "short-lived graph (usually deleted when the command terminates) for intermediate results. " +
            "If you don't specify a temporary cluster name, then the cluster used by 'conn' will be used instead."));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();

    connName = parsedArgs.get("conn").getStringValue();
    graphConn = entRuntime.createGraphConnectionFor(connName);

    if (parsedArgs.containsKey("temp-cluster")) {
      tempClusterName = parsedArgs.get("temp-cluster").getStringValue();
    } else {
      tempClusterName = graphConn.getPoolName();
    }
  }

}
