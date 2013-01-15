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
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.util.UidGenerator;
import uk.ac.ncl.aries.entanglement.player.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.player.NodeDAO;
import uk.ac.ncl.aries.entanglement.player.NodeDAONodePerDocImpl;
import uk.ac.ncl.aries.entanglement.player.data.Node;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNodeIfNotExistsByName;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public class CreateNodeIfNotExistsByNamePlayer 
    extends AbstractLogItemPlayer
{
  @Override
  public String getSupportedLogItemType()
  {
    return CreateNodeIfNotExistsByName.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    CreateNodeIfNotExistsByName cn = (CreateNodeIfNotExistsByName) item.getOp();
    
    if (nodeDao.existsByName(cn.getName()))
    {
      //Return without doing anything if a node with this name exists.
      return;
    }
    
    Node node = new Node();
    node.setName(cn.getName());
    node.setType(cn.getType());
    node.setUid(cn.getUid());
    
    try 
    {
      nodeDao.store(marshaller.serialize(node));
    }
    catch (DbObjectMarshallerException ex) 
    {
      throw new LogPlayerException("Failed to store item", ex);
    }
  }

}
