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
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.player.LogPlayer;
import com.entanglementgraph.player.LogPlayerMongoDbImpl;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.LinkedList;
import java.util.List;

import static com.entanglementgraph.irc.commands.EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault;

/**
 * This command takes the revision history of the specified graph and plays back all revisions marked as 'complete'.
 * This is useful in situations where you may have recovered a revision history from backup and now need to play back
 * all the revisions to node/edge collections.
 *
 * @author Keith Flanagan
 */
public class PlaybackCommittedLogItemsCommand extends AbstractEntanglementCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "This command takes the revision history of the specified graph and plays back all revisions marked as 'complete'. " +
        "This is useful in situations where you may have recovered a revision history from backup and now need to play back " +
        "all the revisions to node/edge collections.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    return params;
  }

  public PlaybackCommittedLogItemsCommand() {
    super(Requirements.GRAPH_CONN_NEEDED);
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {

    try {
      bot.infoln("Starting to replay committed revisions in: %s/%s",
          graphConn.getGraphName(), graphConn.getGraphBranch());
      LogPlayer logPlayer = new LogPlayerMongoDbImpl(graphConn, graphConn);
      logPlayer.replayAllRevisions();

      Message result = new Message(channel);
      result.println("%s%s/%s:%s Replay of revisions complete complete",
          Colors.CYAN,
          graphConn.getGraphName(), graphConn.getGraphBranch(),
          Colors.NORMAL);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}