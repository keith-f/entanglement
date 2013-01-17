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
  
  private static void printDuration(long startMs, long endMs)
  {
    double durationSec = (endMs - startMs) / 1000d;
    logger.info("Operation took: "+durationSec+" seconds.");
  }
}
