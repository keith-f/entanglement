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
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

import static com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault;

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
    return "Sets the currently active graph connection for shell commands.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("conn", String.class, "The name of the connection that is to be set 'active'"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String connName = parsedArgs.get("conn").getStringValue();

    EntanglementRuntime runtime = state.getUserObject();

    try {
      GraphConnectionDetails details = runtime.getGraphConnectionDetails().get(connName);
      if (details == null) {
        throw new UserException(sender, "No graph connection information exists with the name: "+connName);
      }

      runtime.setCurrentConnectionName(connName);

      Message result = new Message(channel);
      result.println("Current graph set to: %s", connName);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
