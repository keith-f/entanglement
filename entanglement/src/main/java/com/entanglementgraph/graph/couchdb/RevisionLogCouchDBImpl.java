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

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.graph.commands.*;
import com.scalesinformatics.util.UidGenerator;
import org.ektorp.CouchDbConnector;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class RevisionLogCouchDBImpl implements RevisionLog {

  private static final Logger logger = Logger.getLogger(RevisionLogCouchDBImpl.class.getName());

//  private final RevisionsCouchDbDAO revLogDao;
  private final RevisionsCouchDbDAO revLogDao;
  private final Set<RevisionLogListener> listeners;

  public RevisionLogCouchDBImpl(CouchDbConnector db) {
    this.revLogDao = new RevisionsCouchDbDAO(db);
    this.listeners = new HashSet<>();
  }

  @Override
  public void addListener(RevisionLogListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(RevisionLogListener listener) {
    listeners.remove(listener);
  }

  private void notifyPostCommit(TransactionCommit op) throws RevisionLogListenerException {
    for (RevisionLogListener listener : listeners) {
      listener.notifyPostCommit(op);
    }
  }

  @Override
  public void submitRevision(String graphId, String patchUid, int patchIdx, GraphOperation op)
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


      if (op instanceof TransactionBegin) {
        logger.info("Start of new transaction. Nothing else to do at this point.");
      } else if (op instanceof TransactionCommit) {
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
    }
    catch(Exception e)
    {
      throw new RevisionLogException("Failed to store revision log item: "+container, e);
    }
  }

  @Override
  public String submitRevisions(String graphId, String patchUid, int patchIdx, List<GraphOperation> ops)
      throws RevisionLogException {
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

      revLogDao.add(container);
      return container.getId(); //Return database UID of the container document
    }
    catch(Exception e)
    {
      throw new RevisionLogException(
          "Failed to store "+ops.size()+" revision log items", e);
    }
  }

  private void commit(TransactionCommit op) throws RevisionLogException
  {
    //FIXME for now, 'commit' does nothing except fire an event. This might be fine with CouchDB's larger docs
    String transactionUid = op.getUid();
    try {
//      logger.info("************* COMMITTING: "+transactionUid);
//      Date now = new Date(System.currentTimeMillis());

//      DBObject query = new BasicDBObject(FIELD_TXN_UID, transactionUid);
//      DBObject update = new BasicDBObject("$set",
//          new BasicDBObject(FIELD_COMMITTED, true)
//              .append(FIELD_DATE_COMMITTED, marshaller.serializeToString(now)));
//
//      WriteResult result = revLogCol.updateMulti(query, update);

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
    //FIXME for now, 'commit' does nothing except fire an event. This might be fine with CouchDB's larger docs
//    String transactionUid = op.getUid();
//    try {
//      logger.info("************* ROLLING BACK TRANSACTION: "+transactionUid);
//      DBObject query = new BasicDBObject(FIELD_TXN_UID, transactionUid);
//      WriteResult result = revLogCol.remove(query);
//      logger.info(result.toString());
//      logger.info("************* ROLLING COMPLETED: "+transactionUid);
//    }
//    catch(Exception e) {
//      logger.info("************* ROLLING FAILED: "+transactionUid);
//      throw new RevisionLogException("Failed to roll back transaction: "+transactionUid, e);
//    }
  }

  @Override
  public Iterable<RevisionItemContainer> iterateUncommittedRevisions(String patchUid) {
    return null;
  }

  @Override
  public Iterable<RevisionItemContainer> iterateRevisionsForTransaction(String patchUid) {
    return null;
  }

  @Override
  public Iterable<RevisionItemContainer> iterateCommittedRevisionsForGraph(String graphId) {
    return null;
  }
}
