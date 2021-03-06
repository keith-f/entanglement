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

package uk.ac.ncl.aries.entanglement.util;

import com.torrenttamer.util.UidGenerator;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionBegin;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionCommit;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionRollback;

/**
 *
 * @author Keith Flanagan
 */
public class TxnUtils
{
  private static final Logger logger =
      Logger.getLogger(TxnUtils.class.getName());
  
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
  
  public static void rollbackTransaction(RevisionLog revLog, String graphId, String branchId, String txnId)
      throws RevisionLogException
  {
    if (txnId == null) {
      logger.info("txnId was null - ignoring rollback request");
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
  public static void silentRollbackTransaction(RevisionLog revLog, 
          String graphId, String branchId, String txnId)
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
  
  private static void printDuration(long startMs, long endMs)
  {
    double durationSec = (endMs - startMs) / 1000d;
    logger.info("Operation took: "+durationSec+" seconds.");
  }
}
