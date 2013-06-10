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

package com.entanglementgraph.util.experimental;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.entanglementgraph.revlog.RevisionLogListenerException;
import org.openide.util.Exceptions;
import com.entanglementgraph.player.LogPlayer;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.revlog.RevisionLogListener;
import com.entanglementgraph.revlog.commands.TransactionCommit;
import com.entanglementgraph.revlog.commands.TransactionRollback;

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
  private static final Logger logger =
          Logger.getLogger(GraphOpPostCommitPlayer.class.getName());
  
  private final LogPlayer player;
  
  public GraphOpPostCommitPlayer(LogPlayer player)
  {
    this.player = player;
  }
  
  @Override
  public void notifyRevisionsSubmitted(String graphId, String graphBranchId, 
      String txnId, int txnSubmitId, int numRevisions) throws RevisionLogListenerException {
  }

  @Override
  public void notifyPreCommit(TransactionCommit op) throws RevisionLogListenerException {
  }

  @Override
  public void notifyPostCommit(TransactionCommit op) throws RevisionLogListenerException {
    try {
      logger.log(Level.INFO, "Received notification of transaction commit: {0}", op.getUid());
      player.playRevisionsForTransaction(op.getUid());
      logger.info("Playback of this transaction is now complete");
    } catch (LogPlayerException ex) {
//      Exceptions.printStackTrace(ex);
      throw new RevisionLogListenerException("Failed to play back a one or more revisions.", ex);
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

}
