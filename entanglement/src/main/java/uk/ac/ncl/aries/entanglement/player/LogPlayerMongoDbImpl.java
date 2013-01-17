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

package uk.ac.ncl.aries.entanglement.player;

import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.player.spi.LogItemPlayer;
import uk.ac.ncl.aries.entanglement.player.spi.LogItemPlayerProvider;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItemContainer;

/**
 *
 * @author Keith Flanagan
 */
public class LogPlayerMongoDbImpl
    implements LogPlayer
{
  private static final Logger logger =
      Logger.getLogger(LogPlayerMongoDbImpl.class.getName());
  
//  private final Mongo m;
//  private final DB db;
  
  private final String graphId;
  private final String graphBranch;
  
//  private final DBCollection nodeCol;
//  private final DBCollection edgeCol;
  
  private final RevisionLog revLog;
  private final NodeDAO nodeDao;
  private final EdgeDAO edgeDao;
  
  private final LogItemPlayerProvider playerProvider;
  
//  public LogPlayerMongoDbImpl(Mongo m, DB db, String graphName, String graphBranch, 
//          DbObjectMarshaller marshaller,
//          RevisionLog revLog, NodeDAO nodeDao, EdgeDAO edgeDao)
//      throws RevisionLogException
//  {
  public LogPlayerMongoDbImpl(ClassLoader cl, DbObjectMarshaller marshaller,
          String graphName, String graphBranch, 
          RevisionLog revLog, NodeDAO nodeDao, EdgeDAO edgeDao)
      throws RevisionLogException
  {
//    this.m = m;
//    this.db = db;
    this.graphId = graphName;
    this.graphBranch = graphBranch;
    
    playerProvider = new LogItemPlayerProvider(cl, marshaller);
    
    this.revLog = revLog;
    this.nodeDao = nodeDao;
    this.edgeDao = edgeDao;
    
//    GraphCheckoutNamingScheme collectionNamer = new GraphCheckoutNamingScheme(graphName, graphBranch);
//    nodeCol = db.getCollection(collectionNamer.getNodeCollectionName());
//    edgeCol = db.getCollection(collectionNamer.getEdgeCollectionName());
//    
//    revLog = new RevisionLogDirectToMongoDbImpl(m, db);
//    nodeDao = new NodeDAO(m, db, nodeCol, graphName, graphBranch);
//    edgeDao = PlayerDAOFactory.createDefaultEdgeDAO(m, db, nodeCol, edgeCol);
  }
  
  @Override
  public void deleteWorkingCopy()
      throws LogPlayerException
  {
    try {
      nodeDao.getCollection().drop();
      edgeDao.getCollection().drop();
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to delete working copy for graph: "+graphId+"/"+graphBranch, e);
    }
  }

  @Override
  public void replayAllRevisions()
      throws LogPlayerException
  {
    try {
      Iterable<RevisionItemContainer> containers = 
              revLog.iterateCommittedRevisionsForGraph(graphId, graphBranch);
      for (RevisionItemContainer container : containers)
      {
        for (RevisionItem item : container.getItems()) {
  //        logger.info("Going to play revision: "+item);

          LogItemPlayer itemPlayer = playerProvider.getPlayerFor(item.getType());
          itemPlayer.playItem(nodeDao, edgeDao, item);
        }
      }
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to replay log to a working copy: "+graphId+"/"+graphBranch, e);
    }
  }
  
  @Override
  public void playRevisionsForTransaction(String transactionUid)
      throws LogPlayerException
  {
    try {
      logger.info("Going to play revision items for txn: "+transactionUid);
      Iterable<RevisionItemContainer> containers = 
              revLog.iterateRevisionsForTransaction(transactionUid);
      for (RevisionItemContainer container : containers)
      {
        for (RevisionItem item : container.getItems()) {
          LogItemPlayer itemPlayer = playerProvider.getPlayerFor(item.getType());
          itemPlayer.playItem(nodeDao, edgeDao, item);
        }
      }
    }
    catch(Exception e) {
      throw new LogPlayerException(
          "Failed to replay log items for transaction: "+transactionUid
              +" to a working copy: "+graphId+"/"+graphBranch, e);
    }
    catch(Error e) {
      /*
       * /Make sure we pick up on ServiceConfigurationError and the like for
       * cases where something isn't quite right with the SPI definition file
       * or classpath.
       */
      throw new LogPlayerException(
          "Failed to replay log items for transaction: "+transactionUid
              +" to a working copy: "+graphId+"/"+graphBranch, e);
    }
  }
//  @Override
//  public void replayToRevision(long toRevId)
//      throws LogPlayerException
//  {
//    try {
//      long fromRevId = -1;
//      for (RevisionItem item : 
//          revLog.iterateCommittedRevisionsForGraph(graphId, graphBranch, fromRevId)) {
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
//          "Failed to replay log to a working copy: "+graphId+"/"+graphBranch, e);
//    }
//  }
  

}
