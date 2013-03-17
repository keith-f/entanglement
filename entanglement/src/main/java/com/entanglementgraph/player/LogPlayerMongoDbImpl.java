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
 * File created: 14-Nov-2012, 11:25:26
 */

package com.entanglementgraph.player;

import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.player.spi.LogItemPlayerProvider;
import com.entanglementgraph.util.GraphConnection;

import java.util.logging.Logger;
import com.entanglementgraph.player.spi.LogItemPlayer;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.revlog.data.RevisionItem;
import com.entanglementgraph.revlog.data.RevisionItemContainer;

/**
 *
 * @author Keith Flanagan
 */
public class LogPlayerMongoDbImpl
    implements LogPlayer
{
  private static final Logger logger =
      Logger.getLogger(LogPlayerMongoDbImpl.class.getName());

  private final GraphConnection srcGraphConn;
  private final GraphConnection tgtGraphConn;

//  private final LogItemPlayerProvider playerProvider;

  public LogPlayerMongoDbImpl(GraphConnection srcGraphConn, GraphConnection tgtGraphConn)
      throws RevisionLogException
  {
    this.srcGraphConn = srcGraphConn;
    this.tgtGraphConn = tgtGraphConn;


//    playerProvider = new LogItemPlayerProvider(graphConn);
  }
  
  @Override
  public void deleteWorkingCopy()
      throws LogPlayerException
  {
    try {
      tgtGraphConn.getNodeDao().getCollection().drop();
      tgtGraphConn.getEdgeDao().getCollection().drop();
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to delete working copy for graph: "+tgtGraphConn.getGraphName()+"/"+tgtGraphConn.getGraphBranch(), e);
    }
  }

  @Override
  public void replayAllRevisions()
      throws LogPlayerException
  {
    try {
      Iterable<RevisionItemContainer> containers = 
              srcGraphConn.getRevisionLog().iterateCommittedRevisionsForGraph(
                  srcGraphConn.getGraphName(), srcGraphConn.getGraphBranch());

      LogItemPlayerProvider playerProvider = new LogItemPlayerProvider(tgtGraphConn);
      for (RevisionItemContainer container : containers)
      {
        for (RevisionItem item : container.getItems()) {
  //        logger.info("Going to play revision: "+item);

          LogItemPlayer itemPlayer = playerProvider.getPlayerFor(item.getType());
          itemPlayer.playItem(item);
        }
      }
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to replay log from graph: "+srcGraphConn.getGraphName()+"/"+srcGraphConn.getGraphBranch()
          + " to graph: "+tgtGraphConn.getGraphName()+"/"+tgtGraphConn.getGraphBranch(), e);
    }
  }
  
  @Override
  public void playRevisionsForTransaction(String transactionUid)
      throws LogPlayerException
  {
    try {
      logger.info("Going to play revision items for txn: "+transactionUid);

      Iterable<RevisionItemContainer> containers = 
              srcGraphConn.getRevisionLog().iterateRevisionsForTransaction(transactionUid);
      LogItemPlayerProvider playerProvider = new LogItemPlayerProvider(tgtGraphConn);
      for (RevisionItemContainer container : containers)
      {
        for (RevisionItem item : container.getItems()) {
          LogItemPlayer itemPlayer = playerProvider.getPlayerFor(item.getType());
          itemPlayer.playItem(item);
        }
      }
    }
    catch(Exception | Error e) {
      throw new LogPlayerException(
          "Failed to replay transaction: "+transactionUid
              +" from graph: "+srcGraphConn.getGraphName()+"/"+srcGraphConn.getGraphBranch()
              + " to graph: "+tgtGraphConn.getGraphName()+"/"+tgtGraphConn.getGraphBranch(), e);
    }
  }
//  @Override
//  public void replayToRevision(long toRevId)
//      throws LogPlayerException
//  {
//    try {
//      long fromRevId = -1;
//      for (RevisionItem item : 
//          revLog.iterateCommittedRevisionsForGraph(graphName, graphBranch, fromRevId)) {
//        if (item.getRevisionId() > toRevId) {
//          break;
//        }
////        logger.info("Going to play revision: "+item);
//        
//        LogItemPlayer itemPlayer = playerProvider.getPlayerFor(item.getOperationClassType());
//        itemPlayer.playItem(nodeDao, edgeDao, item);
//
//      }
//    }
//    catch(Exception e) {
//      throw new LogPlayerException(
//          "Failed to replay log to a working copy: "+graphName+"/"+graphBranch, e);
//    }
//  }
  

}
