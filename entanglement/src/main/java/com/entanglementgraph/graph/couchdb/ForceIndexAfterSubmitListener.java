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
import com.entanglementgraph.graph.commands.TransactionCommit;
import com.entanglementgraph.graph.commands.TransactionRollback;
import com.scalesinformatics.util.UidGenerator;

import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class ForceIndexAfterSubmitListener implements RevisionLogListener {
  private static final Logger logger = Logger.getLogger(ForceIndexAfterSubmitListener.class.getSimpleName());
  private final NodeDAO nodeDao;
  private final EdgeDAO edgeDao;

  private long totalMsSpentIndexing = 0;
  private long totalMsSpentIndexingNodes = 0;
  private long totalMsSpentIndexingEdges = 0;

  public ForceIndexAfterSubmitListener(NodeDAO nodeDao, EdgeDAO edgeDao) {
    this.nodeDao = nodeDao;
    this.edgeDao = edgeDao;
  }

  @Override
  public void notifyRevisionsSubmitted(String graphId, String graphBranchId, String txnId, int txnSubmitId, int numRevisions) throws RevisionLogListenerException {

  }

  @Override
  public void notifyPreCommit(TransactionCommit op) throws RevisionLogListenerException {

  }

  @Override
  public void notifyPostCommit(TransactionCommit op) throws RevisionLogListenerException {
    try {
      long startNode = System.currentTimeMillis();
//      logger.info("Forcing Node View index updates");
      nodeDao.getByKey(new EntityKeys(UidGenerator.generateUid(), UidGenerator.generateUid()));
      long endNode = System.currentTimeMillis();
      totalMsSpentIndexingNodes = totalMsSpentIndexingNodes + (endNode - startNode);

      long startEdge = System.currentTimeMillis();
//      logger.info("Forcing Edge View index updates");
      edgeDao.getByKey(new EntityKeys(UidGenerator.generateUid(), UidGenerator.generateUid()));
      edgeDao.iterateEdgesBetweenNodes(
          new EntityKeys(UidGenerator.generateUid(), UidGenerator.generateUid()),
          new EntityKeys(UidGenerator.generateUid(), UidGenerator.generateUid())
      );
      long endEdge = System.currentTimeMillis();
      totalMsSpentIndexingEdges = totalMsSpentIndexingEdges + (endEdge - startEdge);



      totalMsSpentIndexing = totalMsSpentIndexingNodes + totalMsSpentIndexingEdges;

    } catch (Exception e) {
      logger.warning("Failed to update one or more view indexes. Error caught here and not propagated. Error was:");
      e.printStackTrace();
    }
  }

  @Override
  public void notifyCommitFailed(TransactionCommit op) throws RevisionLogListenerException {

  }

  @Override
  public void notifyPreRollback(TransactionRollback op) throws RevisionLogListenerException {

  }

  @Override
  public void notifyPostRollback(TransactionRollback op) throws RevisionLogListenerException {

  }

  @Override
  public void notifyRollbackFailed(TransactionRollback op) throws RevisionLogListenerException {

  }

  public long getTotalMsSpentIndexing() {
    return totalMsSpentIndexing;
  }

  public long getTotalMsSpentIndexingNodes() {
    return totalMsSpentIndexingNodes;
  }

  public long getTotalMsSpentIndexingEdges() {
    return totalMsSpentIndexingEdges;
  }
}
