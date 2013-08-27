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
 * File created: 13-Nov-2012, 12:44:19
 */

package com.entanglementgraph.revlog;

import java.util.List;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.data.RevisionItemContainer;
import com.mongodb.DBCollection;

/**
 * This interface defines the operations that can be performed on a revision
 * history of a graph data structure. Before any mutations can be made to a
 * graph, the requested change must first be added to a revision log instance.
 * There are several advantages to this approach:
 * <ul>
 * <li>The entire changelog for a graph is stored, allowing us to roll-forward
 * /roll-back changes to a graph at any point in the graph's history.</li>
 * <li>Multiple branches can be made at any point in the history, enabling 
 * exploration of different algorithms, different datasets, etc.</li>
 * <li>Modifications to the graph structure can be transactional.</li>
 * <li>Provenance information is stored about which process submitted which
 * transactions</li>
 * </ul>
 * @author Keith Flanagan
 */
public interface RevisionLog
{
  public void addListener(RevisionLogListener listener);
  public void removeListener(RevisionLogListener listener);
  
  /**
   * Submits a single revision to the revision history.
   * 
   * @param graphId the ID of of the graph to submit the revision to.
   * @param graphBranchId the branch of the graph to submit the revision to.
   * @param txnId a unique ID of the transaction for which this revision is
   * associated with. You can call this method multiple times for
   * the same transaction.
   * @param txnSubmitId a monotonically increasing integer associated with transaction
   * <code>txnId</code> that can be used to determine the playback order of this
   * revision when multiple revisions are submitted with the same transaction ID.
   * The <code>txnSubmitId</code> is client-generated In most cases, this should be 
   * trivial since most applications will have one transaction per thread.
   * This approach reduces database load substantially when inserting many 
   * thousands of revisions.
   * @param op the operation to be performed on the graph
   * @throws RevisionLogException 
   */
  public void submitRevision(String graphId, String graphBranchId,
          String txnId, int txnSubmitId, GraphOperation op)
          throws RevisionLogException;
  
  /**
   * Submits multiple revisions to the revision history as a batch. Submitting
   * several revisions at the same time is vastly more efficient than calling
   * <code>submitRevision</code> when your graph is likely to require large 
   * numbers of modifications.
   * 
   * All operations submitted as part of a call to this method should be
   * associated with the same transaction to the same graph/branch.
   * 
   * If <code>ops</code> is empty, then this method has no effect.
   * 
   * @param graphId the ID of of the graph to submit the revision to.
   * @param graphBranchId the branch of the graph to submit the revision to.
   * @param txnId a unique ID of the transaction for which this revision is
   * associated with. You can call this method multiple times for
   * the same transaction.
   * @param txnSubmitId a monotonically increasing integer associated with transaction
   * <code>txnId</code> that can be used to determine the playback order of this
   * revision when multiple revisions are submitted with the same transaction ID.
   * The <code>txnSubmitId</code> is client-generated In most cases, this should be 
   * trivial since most applications will have one transaction per thread.
   * This approach reduces database load substantially when inserting many 
   * thousands of revisions.
   * @param ops a list of operations to submit,
   * @throws RevisionLogException 
   */
  public void submitRevisions(String graphId, String graphBranchId, 
          String txnId, int txnSubmitId, List<GraphOperation> ops)
          throws RevisionLogException;  
  
//  public Iterable<RevisionItem> iterateUncommittedRevisions();
  
  /**
   * Returns an iterator for revision containers for a transaction that is
   * uncommitted. Returned items are ordered by their <code>txnSubmitId</code>.
   * @param transactionUid
   * @return 
   */
  public Iterable<RevisionItemContainer> iterateUncommittedRevisions(String transactionUid);
  
  
  public Iterable<RevisionItemContainer> iterateRevisionsForTransaction(String transactionUid);
  
  
//  public Iterable<RevisionItem> iterateCommittedRevisions();
  
//  public Iterable<RevisionItem> iterateCommittedRevisionsForGraph(String graphId);
  
  /**
   * For a given graph/branch, iterates committed revision containers, ordered 
   * first by transaction commit date, and second by the <code>txnSubmitId</code>
   * of the revision container.
   * 
   * @param graphId
   * @param branchId
   * @return 
   */
  public Iterable<RevisionItemContainer> iterateCommittedRevisionsForGraph(String graphId, String branchId);
  
//  public Iterable<RevisionItem> iterateCommittedRevisionsForGraph(String graphId, String branchId, long fromRevId);

  public DBCollection getRevLogCol();
}
