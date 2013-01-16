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

import com.mongodb.BasicDBObject;
import com.torrenttamer.util.UidGenerator;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNode;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 * Creates a node without performing any checks regarding whether it already
 * exists. 
 * 
 * @author Keith Flanagan
 */
public class CreateNodePlayer 
    extends AbstractLogItemPlayer
{
  @Override
  public String getSupportedLogItemType()
  {
    return CreateNode.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      CreateNode cn = (CreateNode) item.getOp();

      BasicDBObject serializedNode = cn.getNode();

      // Node type is a required property
      if (!serializedNode.containsField(NodeDAO.FIELD_TYPE)) {
        throw new LogPlayerException("Can't play operation: "+item.getOp()
                + ". Property " + NodeDAO.FIELD_TYPE + " was not set.");
      }


      /*
       * If we get here, then the node does not currently exist.
       */

      // Generate a UID for this node, if one does not already exist
      if (!serializedNode.containsField(NodeDAO.FIELD_UID)) {
        serializedNode.put(NodeDAO.FIELD_UID, UidGenerator.generateUid());
      }

      nodeDao.store(serializedNode);
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }

}
