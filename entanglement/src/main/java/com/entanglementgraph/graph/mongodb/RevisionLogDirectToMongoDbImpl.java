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

package com.entanglementgraph.graph.mongodb;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.graph.commands.*;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.*;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DeserialisingIterable;
import com.scalesinformatics.util.UidGenerator;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

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
  
  private final Set<RevisionLogListener> listeners;

  private final GraphConnection graphConn;

  
  private final DBCollection revLogCol;

  private final DbObjectMarshaller marshaller;

  public RevisionLogDirectToMongoDbImpl(MongoGraphConnection graphConn, DBCollection revLogCol)
      throws RevisionLogException
  {
    this.listeners = new HashSet<>();

    this.graphConn = graphConn;

    marshaller = ObjectMarshallerFactory.create(graphConn.getClassLoader());

    //Create indexes
    this.revLogCol = revLogCol;
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
  
  private void notifyPostCommit(TransactionCommit op) throws RevisionLogListenerException {
    for (RevisionLogListener listener : listeners) {
      listener.notifyPostCommit(op);
    }
  }
  
  @Override
  public String submitRevision(String graphId, String patchUid, int patchIdx, GraphOperation op)
      throws RevisionLogException
  {
    RevisionItemContainer container = new RevisionItemContainer();
    try
    {
      container.setId(UidGenerator.generateUid());
      container.setGraphUid(graphId);
      container.setPatchUid(patchUid);
      container.setPatchIdx(patchIdx);

      long now = System.currentTimeMillis();
      container.setTimestamp(now);
      container.setTimestampAsText(new Date(now).toString());

      if (op instanceof TransactionCommit) {
        commit((TransactionCommit) op);
      } else if (op instanceof TransactionRollback) {
        rollback((TransactionRollback) op);
      } else  if (op instanceof NodeUpdate) {
        container.addOperation((NodeUpdate) op);
      } else if (op instanceof EdgeUpdate) {
        container.addOperation((EdgeUpdate) op);
      } else {
        throw new UnsupportedOperationException("Currently, operations of type: "+op.getClass()+" aren't supported.");
      }

      DBObject dbObject = marshaller.serialize(container);
      revLogCol.insert(dbObject);
      return container.getId();
    }
    catch(Exception e)
    {
      throw new RevisionLogException("Failed to store revision log item: "+container, e);
    }
  }
  
  @Override
  public String submitRevisions(String graphId, String patchUid, int patchIdx, List<GraphOperation> ops)
      throws RevisionLogException
  {
    if (ops.isEmpty()) {
      return null;
    }
    try
    {
//      List<DBObject> dbObjects = new LinkedList<>();

      RevisionItemContainer container = new RevisionItemContainer();
      container.setId(UidGenerator.generateUid());
      container.setGraphUid(graphId);
      container.setPatchUid(patchUid);
      container.setPatchIdx(patchIdx);

//      container.setRevisionId(nodeCounter.next());
      long now = System.currentTimeMillis();
      container.setTimestamp(now);
      container.setTimestampAsText(new Date(now).toString());

      for (GraphOperation op : ops) {
        if (op instanceof TransactionBegin ||
            op instanceof TransactionCommit ||
            op instanceof TransactionRollback) {
          throw new RevisionLogException("Transaction operations must be "
              + "submitted on their own instead of as a collection.");
        }

        if (op instanceof NodeUpdate) {
          container.addOperation((NodeUpdate) op);
        } else if (op instanceof EdgeUpdate) {
          container.addOperation((EdgeUpdate) op);
        } else {
          throw new UnsupportedOperationException("Currently, operations of type: "+op.getClass()+" aren't supported.");
        }
      }

      DBObject dbObject = marshaller.serialize(container);
      revLogCol.insert(dbObject);
      return container.getId();
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
//      logger.info("************* COMMITTING: "+transactionUid);
      Date now = new Date(System.currentTimeMillis());

      DBObject query = new BasicDBObject(FIELD_TXN_UID, transactionUid);
      DBObject update = new BasicDBObject("$set", 
              new BasicDBObject(FIELD_COMMITTED, true)
              .append(FIELD_DATE_COMMITTED, marshaller.serializeToString(now)));

      WriteResult result = revLogCol.updateMulti(query, update);

//      logger.info("************* COMMIT COMPLETED: "+transactionUid+". Notify listeners...");
      notifyPostCommit(op);
//      logger.info("************* ALL LISTENERS NOTIFIED: "+transactionUid);
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


  @Override
  public Iterable<RevisionItemContainer> iterateCommittedRevisionsForGraph(String graphId)
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_GRPH_UID, graphId);
    query.put(FIELD_COMMITTED, true);

    final DBCursor cursor = revLogCol.find(query).sort(SORT_BY_DATE_COMMITTED).sort(SORT_BY_TXN_SUBMIT_ID);
    cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);

    return new DeserialisingIterable<>(cursor, marshaller, RevisionItemContainer.class);
  }

  public DBCollection getRevLogCol() {
    return revLogCol;
  }
}
