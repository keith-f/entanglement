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

package com.entanglementgraph.couchdb.revlog;

import com.entanglementgraph.couchdb.revlog.commands.TransactionCommit;
import com.entanglementgraph.couchdb.revlog.commands.TransactionRollback;

/**
 *
 * @author Keith Flanagan
 */
public interface RevisionLogListener 
{
  public void notifyRevisionsSubmitted(String graphId, String graphBranchId,
                                       String txnId, int txnSubmitId, int numRevisions)
      throws com.entanglementgraph.revlog.RevisionLogListenerException;

  public void notifyPreCommit(TransactionCommit op)
      throws RevisionLogListenerException;
  public void notifyPostCommit(TransactionCommit op)
      throws RevisionLogListenerException;
  public void notifyCommitFailed(TransactionCommit op)
      throws RevisionLogListenerException;

  public void notifyPreRollback(TransactionRollback op)
      throws RevisionLogListenerException;
  public void notifyPostRollback(TransactionRollback op)
      throws RevisionLogListenerException;
  public void notifyRollbackFailed(TransactionRollback op)
      throws RevisionLogListenerException;
  
  
}
