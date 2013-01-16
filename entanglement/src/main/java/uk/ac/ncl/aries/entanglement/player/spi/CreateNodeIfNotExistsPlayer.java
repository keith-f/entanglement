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
import uk.ac.ncl.aries.entanglement.player.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.player.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNodeIfNotExists;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 * Creates a graph node if it does not already exist by UID or [type|name].
 * 
 * @author Keith Flanagan
 */
public class CreateNodeIfNotExistsPlayer 
    extends AbstractLogItemPlayer
{
  @Override
  public String getSupportedLogItemType()
  {
    return CreateNodeIfNotExists.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    /******************
     * TODO: instead of just returning if the node already exists, we might 
     *       want to update its contents.
     */
    
    /*
     * Nodes MUST have a UID, and a type. A 'well known name' may also optionally
     * be set. 
     * 
     * However, the DBObject that we receive in the <code>RevisionItem<code>
     * here may not have one or more of these required items set. For example,
     * if the process that created the RevisionItem only cares about the 
     * 'well known name', then it may not have specified a UID. In this case,
     * we need to generate a UID here.
     * 
     * Here, we need to set any required values that don't currently have values
     * specified for them by the incoming RevisionItem and check that others
     * exist.
     */
    CreateNodeIfNotExists cn = (CreateNodeIfNotExists) item.getOp();

    BasicDBObject serializedNode = cn.getNode();
    
    // Find out whether this node already exists by UID
    String nodeUid = null;
    boolean exists;
    if (serializedNode.containsField(NodeDAO.FIELD_UID)) {
      nodeUid = serializedNode.getString(NodeDAO.FIELD_UID);
      exists = nodeDao.existsByUid(nodeUid);
      if (exists) {
        return; // For now, do nothing if a node with the same UID already exists
      }
    }
    
    // Node type is a required property
    if (!serializedNode.containsField(NodeDAO.FIELD_TYPE)) {
      throw new LogPlayerException("Can't play operation: "+item.getOp()
              + ". Property " + NodeDAO.FIELD_TYPE + " was not set.");
    }
    
    // Find out whether this node already exists by type|name
    String nodeType = serializedNode.getString(NodeDAO.FIELD_TYPE);
    String nodeName = serializedNode.getString(NodeDAO.FIELD_NAME);
    if (serializedNode.containsField(NodeDAO.FIELD_NAME)) {
      exists = nodeDao.existsByName(nodeType, nodeName);
      if (exists) {
        return; // For now, do nothing if a node with the same type and name combo already exists
      }
    }
    
    
    /*
     * If we get here, then the node does not currently exist.
     */
    
    // Generate a UID for this node, if one does not already exist
    if (!serializedNode.containsField(NodeDAO.FIELD_UID)) {
      serializedNode.put(NodeDAO.FIELD_UID, UidGenerator.generateUid());
    }
    
    nodeDao.store(serializedNode);
  }

}
