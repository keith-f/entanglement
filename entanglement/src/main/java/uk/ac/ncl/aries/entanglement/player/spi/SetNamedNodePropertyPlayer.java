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
 * File created: 27-Nov-2012, 11:56:00
 */

package uk.ac.ncl.aries.entanglement.player.spi;

import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.SetNamedNodeProperty;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public class SetNamedNodePropertyPlayer
    extends AbstractLogItemPlayer
{
  @Override
  public String getSupportedLogItemType()
  {
    return SetNamedNodeProperty.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      SetNamedNodeProperty op = (SetNamedNodeProperty) item.getOp();

      String nodeUniqueId = nodeDao.lookupUniqueIdForName(op.getnType(), op.getnName());

      if (nodeUniqueId == null) {
        throw new LogPlayerException("Failed to find named node: "+op.getnName()
                +" while attempting to add a property: "+op.getpName()
                +", with value: "+op.getpVal());
      }

      nodeDao.setPropertyByName(op.getnType(), op.getnName(), op.getpName(), op.getpVal());
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }

}
