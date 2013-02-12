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

package uk.ac.ncl.aries.entanglement.player.spi;

import com.mongodb.DB;
import com.mongodb.Mongo;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionCommit;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public class TransactionCommitPlayer
    extends AbstractLogItemPlayer
{
  private static final Logger logger =
          Logger.getLogger(TransactionCommitPlayer.class.getName());
  
  @Override
  public void initialise(ClassLoader cl, Mongo mongo, DB db)
  {
  }
  
  @Override
  public String getSupportedLogItemType()
  {
    return TransactionCommit.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    TransactionCommit txn = (TransactionCommit) item.getOp();
    
    logger.info("Acknowledging COMMIT of transaction: "+txn.getUid()
            + "Nothing to do here yet.");
  }

}
