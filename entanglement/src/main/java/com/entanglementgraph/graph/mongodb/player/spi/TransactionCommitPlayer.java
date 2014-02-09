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
 */

package com.entanglementgraph.graph.mongodb.player.spi;

import java.util.logging.Logger;

import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.entanglementgraph.graph.commands.TransactionCommit;
import com.entanglementgraph.graph.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public class TransactionCommitPlayer
    extends AbstractLogItemPlayer
{
  private static final Logger logger = Logger.getLogger(TransactionCommitPlayer.class.getName());

  
  @Override
  public String getSupportedLogItemType()
  {
    return TransactionCommit.class.getSimpleName();
  }

  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    TransactionCommit txn = (TransactionCommit) item.getOp();
    
    logger.info("Acknowledging COMMIT of transaction: "+txn.getUid() + "Nothing to do here yet.");
  }

}