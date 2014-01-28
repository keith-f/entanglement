/*
 * Copyright 2012 Keith Flanagan
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
 * File created: 27-Nov-2012, 11:56:00
 */

package com.entanglementgraph.graph.mongodb.player.spi;


import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.graph.mongodb.GraphConnectionFactory;

import java.util.logging.Logger;

import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.entanglementgraph.graph.mongodb.player.LogPlayer;
import com.entanglementgraph.graph.mongodb.player.LogPlayerMongoDbImpl;
import com.entanglementgraph.graph.commands.BranchImport;
import com.entanglementgraph.graph.RevisionItem;

/**
 *
 * 
 * @author Keith Flanagan
 */
public class BranchImportPlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger =
          Logger.getLogger(BranchImportPlayer.class.getName());

  
  @Override
  public String getSupportedLogItemType()
  {
    return BranchImport.class.getSimpleName();
  }

  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    String sourceGraphName = null;
    String sourceGraphBranch = null;
    try {
      BranchImport command = (BranchImport) item.getOp();
      
      sourceGraphName = command.getFromGraphUid();
      sourceGraphBranch = command.getFromBranchUid();
      
      /*
       * The graphConnection object in this player is the TARGET graph connection. To replay updates from the SOURCE
       * graph, we'll need to create another connection object.
       *
       * To do this, we assume that the SOURCE graph is in the same MongoDB cluster and database as the TARGET graph.
       * We may wish to change this in future.
       */
      GraphConnectionFactory connFact = new GraphConnectionFactory(
          graphConnection.getClassLoader(),
          graphConnection.getPoolName(), graphConnection.getDb().getName());

      GraphConnection sourceGraphConn = connFact.connect(sourceGraphName, sourceGraphBranch);

      LogPlayer sourceLogPlayer = new LogPlayerMongoDbImpl(sourceGraphConn, graphConnection);
      sourceLogPlayer.replayAllRevisions();
       
    } catch (Exception e) {
      throw new LogPlayerException("Failed to import all revision history from graph/branch: "
          + sourceGraphName + "/" + sourceGraphBranch + ". Commands was:\n"+item.getOp(), e);
    }
  }

  
}
