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
 * File created: 07-Dec-2012, 15:15:25
 */

package com.entanglementgraph.util;

import com.entanglementgraph.graph.commands.GraphOperation;
import com.scalesinformatics.util.UidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.entanglementgraph.graph.RevisionLog;
import com.entanglementgraph.graph.RevisionLogException;
import com.entanglementgraph.graph.commands.TransactionBegin;
import com.entanglementgraph.graph.commands.TransactionCommit;
import com.entanglementgraph.graph.commands.TransactionRollback;

/**
 *
 * @author Keith Flanagan
 */
public class TxnUtils
{
  private static final Logger logger = Logger.getLogger(TxnUtils.class.getName());

  /**
   * Convenience method that takes a single graph operation, wraps it in a transaction and submits it to the
   * revision log. This method is useful if you have an individual graph operation to submit as part
   * of a single patch set and don't want to deal with creating / committing transactions manually.
   * @param conn
   * @param graphOp the graph operation to apply.
   * @throws RevisionLogException
   */
  public static void submitAsTxn(GraphConnection conn, GraphOperation graphOp) throws RevisionLogException {
    List<GraphOperation> ops = new ArrayList<>(1);
    ops.add(graphOp);
    submitAsTxn(conn, ops);
  }

  /**
   * Convenience method that takes a list of graph operations, wraps them in a transaction and submits them to the
   * revision log. This method is useful if you have a relatively small number of graph operations to submit as part
   * of a single transaction; you don't need to deal with creating / committing transactions manually in this case.
   * @param conn
   * @param ops the graph operations to apply.
   * @throws RevisionLogException
   */
  public static void submitAsTxn(GraphConnection conn, List<GraphOperation> ops) throws RevisionLogException {
    String txnId = null;
    if (ops.isEmpty()) {
      return;
    }
    try {
      txnId = TxnUtils.beginNewTransaction(conn);
      conn.getRevisionLog().submitRevisions(conn.getGraphName(), conn.getGraphBranch(), txnId, 1, ops);
      TxnUtils.commitTransaction(conn, txnId);
    } catch (Exception e) {
      TxnUtils.silentRollbackTransaction(conn, txnId);
      throw new RevisionLogException("Failed to perform graph operations on "
          +conn.getGraphBranch()+"/"+conn.getGraphBranch()
          +". Exception attached.", e);
    }
  }


  /**
   * Submits a batch of operations as part of a transaction. You must specify a transaction ID, and a <code>partId</code>.
   * This main use-cases of this method are as follows:
   * <ul>
   *   <li>You need to submit a massive number of graph operations as part of the same transaction (too many to submit
   *   with the <code>submitAsTxn</code> method.</li>
   *   <li>You have a cluster of machines whose combined output should be considered as part of the same transaction.</li>
   * </ul>
   *
   * In both of these cases, you need to manage the start, submission and end of a transaction block manually. This is
   * achieved by:
   * <ol>
   *   <li>A call to <code>beginNewTransaction</code> in order to obtain a new String transaction ID</li>
   *   <li>One or more calls to <code>submitTxnPart</code> (this method) to submit graph operations</li>
   *   <li>A final call to either <code>commitTransaction</code> or <code>rollbackTransaction</code> to mark the
   *   end of a transaction block.</li>
   * </ol>
   *
   * When calling <code>submitTxnPart</code>, you need to specify an integer <code>partId</code>. This number should
   * be zero or positive, and determines the order that elements submitted in this transaction part are processed in,
   * with respect to other pars. The value of <code>partId</code> only really matters when order is important - for
   * example, if you want to ensure that a deletion occurs before an update.
   *
   * Transaction parts can be submitted out of order, but are played back in ascending order. This allows, multiple
   * machines in a cluster to complete work at their own rate, but be able to coordinate an order-dependent set of
   * graph updates as a single graph 'transaction'.
   * 
   * @param conn the connection information to use - a convenient way to obtain the graph name/branch information.
   * @param txnId the unique ID of this transaction
   * @param partId an integer ID for this submission that will determine the order in which it is played back on
   *               commit.
   * @param ops the graph operations that form this part of the transaction.
   * @throws RevisionLogException
   */
  public static void submitTxnPart(GraphConnection conn, String txnId, int partId, List<GraphOperation> ops) 
      throws RevisionLogException {
    conn.getRevisionLog().submitRevisions(conn.getGraphName(), conn.getGraphBranch(), txnId, partId, ops);
  }

  public static String beginNewTransaction(GraphConnection conn)
      throws RevisionLogException
  {
    return beginNewTransaction(conn.getRevisionLog(), conn.getGraphName(), conn.getGraphBranch());
  }

  public static String beginNewTransaction(RevisionLog revLog, String graphId, String branchId)
      throws RevisionLogException
  {
    long start = System.currentTimeMillis();
    try {
      
      String txnId = UidGenerator.generateUid();
      int txnSubmitId = -1;
      revLog.submitRevision(graphId, branchId, txnId, txnSubmitId, new TransactionBegin(txnId));
      return txnId;
    }
    catch(Exception e) {
      throw new RevisionLogException("Failed to start a new transaction", e);
    }
    finally {
      printDuration(start, System.currentTimeMillis());
    }
  }

  public static void commitTransaction(GraphConnection conn, String txnId)
      throws RevisionLogException
  {
    commitTransaction(conn.getRevisionLog(), conn.getGraphName(), conn.getGraphBranch(), txnId);
  }
  
  public static void commitTransaction(RevisionLog revLog, String graphId, String branchId, String txnId)
      throws RevisionLogException
  {
    long start = System.currentTimeMillis();
    try {
      int txnSubmitId = Integer.MAX_VALUE;
      revLog.submitRevision(graphId, branchId, txnId, txnSubmitId, new TransactionCommit(txnId));
    }
    catch(Exception e) {
      throw new RevisionLogException("Failed to commit transaction: "+txnId, e);
    }
    finally {
      printDuration(start, System.currentTimeMillis());
    }
  }

  public static void rollbackTransaction(GraphConnection conn, String txnId)
      throws RevisionLogException
  {
    rollbackTransaction(conn.getRevisionLog(), conn.getGraphName(), conn.getGraphBranch(), txnId);
  }
  
  public static void rollbackTransaction(RevisionLog revLog, String graphId, String branchId, String txnId)
      throws RevisionLogException
  {
    if (txnId == null) {
//      logger.info("txnId was null - ignoring rollback request");
      return;
    }
    long start = System.currentTimeMillis();
    try {
      int txnSubmitId = Integer.MAX_VALUE;
      revLog.submitRevision(graphId, branchId, txnId, txnSubmitId, new TransactionRollback(txnId));
    }
    catch(Exception e) {
      throw new RevisionLogException("Failed to rollback transaction: "+txnId, e);
    }
    finally {
      printDuration(start, System.currentTimeMillis());
    }
  }
  
  /**
   * Same as <code>rollbackTransaction</code>, except that no exception is 
   * thrown here if the rollback operation failed. This is useful in cases where
   * we need to roll back in a <code>catch</code> block - so something has
   * already failed - and additionally, the rollback has failed. In these cases,
   * since we're already going to throw an application-level exception, having
   * to deal with a secondary failure is a big inconvenience. 
   * 
   * By catching any exceptions that occur during rollback here, code elsewhere
   * can be much tidier. In addition, there's nothing bad about NOT rolling 
   * back a transaction successfully; since it's not committed, it won't 
   * affect the graph structure at all. The worst that can happen is that the
   * uncommitted revisions will consume unnecessary disk space. This could be
   * solved by (manually) removing these rare cases, if it became a problem.
   * 
   * @param revLog
   * @param graphId
   * @param branchId
   * @param txnId
   * @throws RevisionLogException 
   */
  public static void silentRollbackTransaction(RevisionLog revLog, String graphId, String branchId, String txnId)
  {
    try {
      rollbackTransaction(revLog, graphId, branchId, txnId);
    }
    catch(Exception e) {
      logger.info("An exception occurred while attempting to roll back transaction: "+txnId
              + ". We're going to silently ignore this, apart from printing a stack trace (follows)");
      e.printStackTrace();
    }
  }

  public static void silentRollbackTransaction(GraphConnection conn, String txnId)
  {
    silentRollbackTransaction(conn.getRevisionLog(), conn.getGraphName(), conn.getGraphBranch(), txnId);
  }
  
  private static void printDuration(long startMs, long endMs)
  {
    double durationSec = (endMs - startMs) / 1000d;
//    logger.info("Operation took: "+durationSec+" seconds.");
  }
}
