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

package com.entanglementgraph.graph.mongodb.player;

import com.entanglementgraph.graph.commands.EdgeModification;
import com.entanglementgraph.graph.commands.NodeModification;
import com.entanglementgraph.graph.mongodb.EdgeDAOSeparateDocImpl;
import com.entanglementgraph.graph.mongodb.MongoGraphConnection;
import com.entanglementgraph.graph.mongodb.NodeDAONodePerDocImpl;
import com.entanglementgraph.graph.mongodb.player.spi.EdgeModificationPlayer;
import com.entanglementgraph.graph.mongodb.player.spi.NodeModificationPlayer;
import com.entanglementgraph.util.GraphConnection;

import java.util.logging.Logger;
import com.entanglementgraph.graph.mongodb.player.spi.LogItemPlayer;
import com.entanglementgraph.graph.RevisionLogException;
import com.entanglementgraph.graph.RevisionItemContainer;

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
  private final MongoGraphConnection tgtGraphConn;


  public LogPlayerMongoDbImpl(GraphConnection srcGraphConn, MongoGraphConnection tgtGraphConn)
      throws RevisionLogException
  {
    this.srcGraphConn = srcGraphConn;
    this.tgtGraphConn = tgtGraphConn;
  }
  
  @Override
  public void deleteWorkingCopy()
      throws LogPlayerException
  {
    try {
      NodeDAONodePerDocImpl nodeDaoMongo = (NodeDAONodePerDocImpl) tgtGraphConn.getNodeDao();
      EdgeDAOSeparateDocImpl edgeDaoMongo = (EdgeDAOSeparateDocImpl) tgtGraphConn.getEdgeDao();
      nodeDaoMongo.getCollection().drop();
      edgeDaoMongo.getCollection().drop();
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to delete working copy for graph: "+tgtGraphConn.getGraphName(), e);
    }
  }

  @Override
  public void replayAllRevisions()
      throws LogPlayerException
  {
    try {
      Iterable<RevisionItemContainer> containers = 
              srcGraphConn.getRevisionLog().iterateCommittedRevisionsForGraph(
                  srcGraphConn.getGraphName());

      for (RevisionItemContainer container : containers)
      {
        for (NodeModification item : container.getNodeUpdates()) {
          NodeModificationPlayer itemPlayer = new NodeModificationPlayer();
          itemPlayer.setGraphConnection(tgtGraphConn);
          itemPlayer.playItem(item);
        }

        for (EdgeModification item : container.getEdgeUpdates()) {
          EdgeModificationPlayer itemPlayer = new EdgeModificationPlayer();
          itemPlayer.setGraphConnection(tgtGraphConn);
          itemPlayer.playItem(item);
        }

        // Old impl
//        for (RevisionItem item : container.getItems()) {
//          //        logger.info("Going to play revision: "+item);
//
//          LogItemPlayer itemPlayer = playerProvider.getPlayerFor(item.getType());
//          itemPlayer.playItem(item);
//        }
      }

    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to replay log from graph: "+srcGraphConn.getGraphName()
          + " to graph: "+tgtGraphConn.getGraphName(), e);
    }
  }
  
  @Override
  public void playRevisionsForTransaction(String transactionUid)
      throws LogPlayerException
  {
    try {
//      logger.info("Going to play revision items for txn: "+transactionUid);

      Iterable<RevisionItemContainer> containers = 
              srcGraphConn.getRevisionLog().iterateRevisionsForTransaction(transactionUid);
      for (RevisionItemContainer container : containers)
      {
        for (NodeModification item : container.getNodeUpdates()) {
          NodeModificationPlayer itemPlayer = new NodeModificationPlayer();
          itemPlayer.setGraphConnection(tgtGraphConn);
          itemPlayer.playItem(item);
        }

        for (EdgeModification item : container.getEdgeUpdates()) {
          EdgeModificationPlayer itemPlayer = new EdgeModificationPlayer();
          itemPlayer.setGraphConnection(tgtGraphConn);
          itemPlayer.playItem(item);
        }
      }
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to replay transaction: "+transactionUid
              +" from graph: "+srcGraphConn.getGraphName()
              + " to graph: "+tgtGraphConn.getGraphName(), e);
    }
    catch(Throwable e) {
      throw new LogPlayerException(
          "Failed to replay transaction: "+transactionUid
              +" from graph: "+srcGraphConn.getGraphName()
              + " to graph: "+tgtGraphConn.getGraphName(), e);
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
