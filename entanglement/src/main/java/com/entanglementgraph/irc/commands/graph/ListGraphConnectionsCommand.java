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
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
public class ListGraphConnectionsCommand extends AbstractEntanglementCommand {

  @Override
  public String getDescription() {
    return "Lists known graph connections.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    return params;
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      logger.println("Graph connections [");
      for (Map.Entry<String, GraphConnectionDetails> entry : entRuntime.getGraphConnectionDetails().entrySet()) {
        GraphConnectionDetails details = entry.getValue();
        logger.println("  %s => %s/%s; %s/%s", entry.getKey(),
            details.getClusterName(),
            details.getDatabase(),
            details.getGraphName());
      }
      logger.println("]");
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
