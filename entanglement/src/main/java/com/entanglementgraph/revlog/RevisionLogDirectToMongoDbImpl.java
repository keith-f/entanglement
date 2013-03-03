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
 * File created: 13-Nov-2012, 12:54:48
 */

package com.entanglementgraph.revlog;

import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import com.torrenttamer.util.UidGenerator;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.TransactionBegin;
import com.entanglementgraph.revlog.commands.TransactionCommit;
import com.entanglementgraph.revlog.commands.TransactionRollback;
import com.entanglementgraph.revlog.data.RevisionItem;
import com.entanglementgraph.revlog.data.RevisionItemContainer;

/**
 *
 * @author Keith Flanagan
 */
public class RevisionLogDirectToMongoDbImpl
    implements RevisionLog
{
  private static final Logger logger =
      Logger.getLogger(RevisionLogDirectToMongoDbImpl.class.getName());
  
  /*
   * Document fields
   */
  public static final String FIELD_GRPH_UID = "graphUniqueId";
  public static final String FIELD_GRPH_BRANCH = "graphBranchId";
  public static final String FIELD_TXN_UID = "transactionUid";
  public static final String FIELD_TXN_SUBMIT_ID = "txnSubmitId";
  public static final String FIELD_COMMITTED = "committed";
  public static final String FIELD_DATE_COMMITTED = "dateCommitted";
  
  /*
   * Pre-defined index definitions
   */
  private static final DBObject IDX__TXN_UID__COMMITTED = 
          new BasicDBObject(FIELD_TXN_UID, 1).append(FIELD_COMMITTED, 1);
  private static final DBObject IDX__TXN_SUBMIT_ID = new BasicDBObject(FIELD_TXN_SUBMIT_ID, 1);
  
  private static final DBObject IDX__GRPH_UID__GRPH_BRANCH__COMMITTED = 
          new BasicDBObject(FIELD_GRPH_UID, 1).append(FIELD_GRPH_BRANCH, 1).append(FIELD_COMMITTED, 1);
  
  /*
   * Pre-defined sort orders
   */
  private static final DBObject SORT_BY_DATE_COMMITTED = new BasicDBObject(FIELD_DATE_COMMITTED, 1);
  private static final DBObject SORT_BY_TXN_SUBMIT_ID = new BasicDBObject(FIELD_TXN_SUBMIT_ID, 1); 
  
//  private static final String REV_COUNTER_NAME = "revision_count";
  private static final String DEFAULT_COL_REVLOG = "revisions";
  
  private final Set<RevisionLogListener> listeners;
  
//  private final HazelcastInstance hz;
  private final Mongo m;
  private final DB db;
  
  private final DBCollection revLogCol;
  
//  private final Counter nodeCounter;
  
  private final String revLogColName;
  
//  private final JsonUtils serializer;
  private final DbObjectMarshaller marshaller;
  
  public RevisionLogDirectToMongoDbImpl(ClassLoader classLoader, Mongo m, DB db)
      throws RevisionLogException
  {
    this.listeners = new HashSet<>();
    this.revLogColName = DEFAULT_COL_REVLOG;
    
    
    this.m = m;
    this.db = db;
    this.revLogCol = db.getCollection(revLogColName);
    
    marshaller = ObjectMarshallerFactory.create(classLoader);
    
    //Create indexes
    revLogCol.ensureIndex(IDX__TXN_UID__COMMITTED);
    revLogCol.ensureIndex(IDX__TXN_SUBMIT_ID);
    revLogCol.ensureIndex(IDX__GRPH_UID__GRPH_BRANCH__COMMITTED);
  }

  private String _getLockName(String graphId, String graphBranchId, String entityId)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(graphId).append(".").append(graphBranchId).append(".").append(entityId);
    return sb.toString();
  }
  
  @Override
  public void addListener(RevisionLogListener listener)
  {
    listeners.add(listener);
  }
  
  @Override
  public void removeListener(RevisionLogListener listener)
  {
    listeners.remove(listener);
  }
  
  private void notifyPostCommit(TransactionCommit op)
  {
    for (RevisionLogListener listener : listeners) {
      listener.notifyPostCommit(op);
    }
  }
  
  @Override
  public void submitRevision(String graphId, String graphBranchId, 
      String txnId, int txnSubmitId, GraphOperation op)
      throws RevisionLogException
  {
    RevisionItemContainer container = new RevisionItemContainer();
    RevisionItem item = new RevisionItem();
    try
    {
      container.setUniqueId(UidGenerator.generateUid());
      container.setGraphUniqueId(graphId);
      container.setGraphBranchId(graphBranchId);
      
      container.setTransactionUid(txnId);
      container.setTxnSubmitId(txnSubmitId);
      
      container.setTimestamp(new Date(System.currentTimeMillis()));
      
      item.setOp(op);
      item.setType(op.getClass().getSimpleName());
      
      container.getItems().add(item);
      
//      DBObject dbObject = (DBObject) JSON.parse(serializer.serializeToString(container));
      DBObject dbObject = marshaller.serialize(container);
      revLogCol.insert(dbObject);
      
      if (op instanceof TransactionCommit) {
        commit((TransactionCommit) op);
      } else if (op instanceof TransactionRollback) {
        rollback((TransactionRollback) op);
     }
    }
    catch(Exception e)
    {
      throw new RevisionLogException("Failed to store revision log item: "+container, e);
    }
  }
  
  @Override
  public void submitRevisions(String graphId, String graphBranchId,
      String txnId, int txnSubmitId, List<GraphOperation> ops)
      throws RevisionLogException
  {
    if (ops.isEmpty()) {
      return;
    }
    try
    {
//      List<DBObject> dbObjects = new LinkedList<>();
      
      RevisionItemContainer container = new RevisionItemContainer();
      container.setUniqueId(UidGenerator.generateUid());
      container.setGraphBranchId(graphBranchId);
      container.setGraphUniqueId(graphId);
      container.setTransactionUid(txnId);
      container.setTxnSubmitId(txnSubmitId);

//      container.setRevisionId(nodeCounter.next());
      container.setTimestamp(new Date(System.currentTimeMillis()));
      
      for (GraphOperation op : ops) {
        if (op instanceof TransactionBegin ||
            op instanceof TransactionCommit ||
            op instanceof TransactionRollback) {
          throw new RevisionLogException("Transaction operations must be "
              + "submitted on their own instead of as a collection.");
        }
        
        RevisionItem item = new RevisionItem();
        item.setOp(op);
        item.setType(op.getClass().getSimpleName());
        container.getItems().add(item);
        
//        //Serialize
//        DBObject dbObject = (DBObject) JSON.parse(JsonSerializer.serializeToString(item));
//        dbObjects.add(dbObject);
      }

//      revLogTmpCol.insert(dbObjects);
//      DBObject dbObject = (DBObject) JSON.parse(serializer.serializeToString(container));
      DBObject dbObject = marshaller.serialize(container);
      revLogCol.insert(dbObject);

    }
    catch(Exception e)
    {
      throw new RevisionLogException(
              "Failed to store "+ops.size()+" revision log items", e);
    }
  }
  
  private void commit(TransactionCommit op) throws RevisionLogException
  {
    String transactionUid = op.getUid();
    try {
      logger.info("************* COMMITTING: "+transactionUid);
      Date now = new Date(System.currentTimeMillis());
  //    String nowStr = serializer.serializeToString(now);

      System.out.println("++++++++++++++++++++++++: "+marshaller.serializeToString(now));
  //    DBObject nowObj = (DBObject) JSON.parse(JsonSerializer.serializeToString(now));

      DBObject query = new BasicDBObject(FIELD_TXN_UID, transactionUid);
      DBObject update = new BasicDBObject("$set", 
              new BasicDBObject(FIELD_COMMITTED, true)
              .append(FIELD_DATE_COMMITTED, marshaller.serializeToString(now)));
  //            .append("dateCommitted", nowStr));
      logger.info("Generated query: "+query);
      logger.info("Generated update: "+update);

      WriteResult result = revLogCol.updateMulti(query, update);

      logger.info("************* COMMIT COMPLETED: "+transactionUid+". Notify listeners...");
      notifyPostCommit(op);
      logger.info("************* ALL LISTENERS NOTIFIED: "+transactionUid);
    }
    catch(Exception e) {
      logger.info("************* COMMIT FAILED: "+transactionUid);
      throw new RevisionLogException("Failed to commit transaction: "+transactionUid, e);
    }
  }

  
  private void rollback(TransactionRollback op) throws RevisionLogException
  {
    String transactionUid = op.getUid();
    try {
      logger.info("************* ROLLING BACK TRANSACTION: "+transactionUid);
      DBObject query = new BasicDBObject(FIELD_TXN_UID, transactionUid);
      WriteResult result = revLogCol.remove(query);
      logger.info(result.toString());
      logger.info("************* ROLLING COMPLETED: "+transactionUid);
    }
    catch(Exception e) {
      logger.info("************* ROLLING FAILED: "+transactionUid);
      throw new RevisionLogException("Failed to roll back transaction: "+transactionUid, e);
    }
  }
  
//  @Override
//  public Iterable<RevisionItem> iterateUncommittedRevisions()
//  {
//    DBObject query = new BasicDBObject();
//    final DBCursor cursor = revLogTmpCol.find(query).sort(SORT_BY_REV);
//    return new DeserialisingIterable<>(cursor, new RevisionItemDBObjectDeserializer());
//  }
  
  @Override
  public Iterable<RevisionItemContainer> iterateUncommittedRevisions(String transactionUid)
  {
    DBObject[] andArgs = new BasicDBObject[] {
        new BasicDBObject(FIELD_TXN_UID, transactionUid),
        new BasicDBObject(FIELD_COMMITTED, false)
    };
    DBObject query = new BasicDBObject("$and", Arrays.asList(andArgs));
    final DBCursor cursor = revLogCol.find(query).sort(SORT_BY_TXN_SUBMIT_ID);
    return new DeserialisingIterable<>(cursor, marshaller, RevisionItemContainer.class);
  }
  
  @Override
  public Iterable<RevisionItemContainer> iterateRevisionsForTransaction(String transactionUid)
  {
    DBObject query = new BasicDBObject(FIELD_TXN_UID, transactionUid);
    final DBCursor cursor = revLogCol.find(query).sort(SORT_BY_TXN_SUBMIT_ID);
    return new DeserialisingIterable<>(cursor, marshaller, RevisionItemContainer.class);
  }
  
  


//  @Override
//  public Iterable<RevisionItem> iterateCommittedRevisions()
//  {
//    final DBCursor cursor = revLogCol.find().sort(SORT_BY_REV);
//    return new DeserialisingIterable<>(cursor, new RevisionItemDBObjectDeserializer());
//  }

//  @Override
//  public Iterable<RevisionItem> iterateCommittedRevisionsForGraph(String graphId)
//  {
//    DBObject query = new BasicDBObject("graphUniqueId", graphId);
//    final DBCursor cursor = revLogCol.find(query).sort(SORT_BY_REV);
//    return new DeserialisingIterable<>(cursor, new RevisionItemDBObjectDeserializer());
//  }

  @Override
  public Iterable<RevisionItemContainer> iterateCommittedRevisionsForGraph(
      String graphId, String branchId)
  {    
    DBObject[] andArgs = new BasicDBObject[] {
        new BasicDBObject(FIELD_GRPH_UID, graphId),
        new BasicDBObject(FIELD_GRPH_BRANCH, branchId),
        new BasicDBObject(FIELD_COMMITTED, true)
    };
    DBObject query = new BasicDBObject("$and", Arrays.asList(andArgs));
//    logger.info("Generated query: "+query);
    
    final DBCursor cursor = revLogCol.find(query).sort(SORT_BY_DATE_COMMITTED).sort(SORT_BY_TXN_SUBMIT_ID);
//    return new DeserialisingIterable<>(cursor, new RevisionItemDBObjectDeserializer());
//    return new DeserialisingIterable<>(cursor, new JsonDBObjectDeserializer(RevisionItemContainer.class));
    return new DeserialisingIterable<>(cursor, marshaller, RevisionItemContainer.class);
  }

//  @Override
//  public Iterable<RevisionItem> iterateCommittedRevisionsForGraph(String graphId,
//      String branchId, long fromRevId)
//  {
//    DBObject[] andArgs = new BasicDBObject[] {
//        new BasicDBObject("graphUniqueId", graphId),
//        new BasicDBObject("graphBranchId", branchId),
//        new BasicDBObject("revisionId", new BasicDBObject("$gte", fromRevId))
//    };
//    DBObject query = new BasicDBObject("$and", Arrays.asList(andArgs));
//    logger.info("Generated query: "+query);
//    
//    final DBCursor cursor = revLogCol.find(query).sort(SORT_BY_REV);
//    return new DeserialisingIterable<>(cursor, new RevisionItemDBObjectDeserializer());
//  }

}
