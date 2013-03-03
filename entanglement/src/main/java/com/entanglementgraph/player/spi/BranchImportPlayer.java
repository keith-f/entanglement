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

package com.entanglementgraph.player.spi;


import com.mongodb.DB;
import com.mongodb.Mongo;
import java.util.logging.Logger;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.player.LogPlayer;
import com.entanglementgraph.player.LogPlayerMongoDbImpl;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.RevisionLogDirectToMongoDbImpl;
import com.entanglementgraph.revlog.commands.BranchImport;
import com.entanglementgraph.revlog.data.RevisionItem;

/**
 * Creates a node without performing any checks regarding whether it already
 * exists. 
 * 
 * @author Keith Flanagan
 */
public class BranchImportPlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger =
          Logger.getLogger(BranchImportPlayer.class.getName());
 
  /*
   * Set on initialisation
   */
  private ClassLoader cl;
  private Mongo m;
  private DB db;

  
  @Override
  public void initialise(ClassLoader cl, Mongo mongo, DB db)
  {
    this.cl = cl;
    this.m = mongo;
    this.db = db;
  }
  
  @Override
  public String getSupportedLogItemType()
  {
    return BranchImport.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      BranchImport command = (BranchImport) item.getOp();
      
      String fromGraphUid = command.getFromGraphUid();
      String fromBranchUid = command.getFromBranchUid();
      
      
      
      RevisionLog fromRevLog = new RevisionLogDirectToMongoDbImpl(cl, m, db);
      
      LogPlayer fromLogPlayer = new LogPlayerMongoDbImpl(cl, marshaller, 
              fromGraphUid, fromBranchUid, fromRevLog, nodeDao, edgeDao);
      fromLogPlayer.replayAllRevisions();
       
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play +"
              +item.getOp().getClass().getSimpleName()+" command. "
              +" Failed to merge graphs.", e);
    }
  }

  
}
