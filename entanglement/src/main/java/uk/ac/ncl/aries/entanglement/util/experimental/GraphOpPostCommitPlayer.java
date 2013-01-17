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

package uk.ac.ncl.aries.entanglement.util.experimental;

import org.openide.util.Exceptions;
import uk.ac.ncl.aries.entanglement.player.LogPlayer;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogListener;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionCommit;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionRollback;

/**
 * An experimental RevisionLogListener that plays revision items to a graph 
 * checkout immediately before commit. This will do for now, until we have a 
 * better solution - if the post-commit graph update fails, then the graph will 
 * be inconsistent! We don't do any error correction here.
 * 
 * @author Keith Flanagan
 */
public class GraphOpPostCommitPlayer
    implements RevisionLogListener
{
  private final LogPlayer player;
  
  public GraphOpPostCommitPlayer(LogPlayer player)
  {
    this.player = player;
  }
  
  @Override
  public void notifyRevisionsSubmitted(String graphId, String graphBranchId, 
      String txnId, int txnSubmitId, int numRevisions) {
  }

  @Override
  public void notifyPreCommit(TransactionCommit op) {
  }

  @Override
  public void notifyPostCommit(TransactionCommit op) {
    try {
      player.playRevisionsForTransaction(op.getUid());
    } catch (LogPlayerException ex) {
      Exceptions.printStackTrace(ex);
    }
  }

  @Override
  public void notifyCommitFailed(TransactionCommit op) {
  }

  @Override
  public void notifyPreRollback(TransactionRollback op) {
  }

  @Override
  public void notifyPostRollback(TransactionRollback op) {
  }

  @Override
  public void notifyRollbackFailed(TransactionRollback op) {
  }

}
