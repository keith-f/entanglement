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
 * File created: 13-Nov-2012, 17:26:13
 */

package com.entanglementgraph.player;

/**
 * A <code>LogPlayer</code> takes a <code>RevisionLog</code> history, or
 * part of that history and 'replays' it into an actual graph data structure
 * (a 'working copy').
 * 
 * This interface defines a number of operations that can be used to transform
 * a working copy 'checkout' of a particular graph revision log.
 * 
 * @author Keith Flanagan
 */
public interface LogPlayer
{
  
  public void deleteWorkingCopy()
      throws LogPlayerException;
  
  
  public void replayAllRevisions()
      throws LogPlayerException;
  
  public void playRevisionsForTransaction(String transactionUid)
      throws LogPlayerException;
  
  /**
   * Replays log items for a particular graph/branch up to the specified 
   * revision ID.
   * 
   * If a working copy of this graph/branch currently exists, then an incremental
   * update may be performed, rather than a complete checkout from scratch.
   * In the case of an incremental update, we assume that now local modifications
   * to the checkout have taken place (i.e., that the graph structure exactly
   * matches that of the revision ID stored in its metadata).
   * 
   * @param revId 
   */
//  public void replayToRevision(long revId)
//      throws LogPlayerException;
}
