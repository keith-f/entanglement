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
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateEdge;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 * Creates an edge.
 * 
 * @author Keith Flanagan
 */
public class CreateEdgePlayer 
    extends AbstractLogItemPlayer
{
  @Override
  public String getSupportedLogItemType()
  {
    return CreateEdge.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      CreateEdge ce = (CreateEdge) item.getOp();

      /*
       * Edges MUST have a UID, a type, a 'from node uid', and a 'to node uid', as
       * well as a 'from node type' and a 'to node type'.
       * Edges may optionally have from/to node names, if those nodes have well
       * known names. 
       * 
       * However, the DBObject that we receive in the <code>RevisionItem<code>
       * here may not have one or more of these required items set. For example,
       * if the process that created the RevisionItem knows only the node name(s),
       * and not their IDs, then it won't have specified the IDs. 
       * Another example is the edge unique ID - the caller potentially doesn't 
       * care about what UID is assigned to the edge, and therefore for efficiency
       * reasons, won't specify one.
       * 
       * Here, we need to set any required values that don't currently have values
       * specified for them by the incoming RevisionItem.
       */

      BasicDBObject serializedEdge = ce.getEdge();


      //Check that both node's type names are set
      if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_TYPE) ||
          !serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_TYPE)) {
        throw new LogPlayerException("Can't play operation: "+item.getOp()
                + ". Either " + EdgeDAO.FIELD_FROM_NODE_TYPE +" or "
                + EdgeDAO.FIELD_TO_NODE_TYPE + " were not set.");
      }

      // Generate a unique ID for this edge, if one doesn't already exist
      if (!serializedEdge.containsField(EdgeDAO.FIELD_UID)) {
        serializedEdge.put(EdgeDAO.FIELD_UID, UidGenerator.generateUid());
      }

      //If no 'from' node UID is specified, then locate it from the name field
      if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_UID)) {
        if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_NAME)) {
          throw new LogPlayerException("Can't play operation: "+item.getOp()
                  + ". You must set at least at least one of these: " 
                  + EdgeDAO.FIELD_FROM_NODE_UID +", " + EdgeDAO.FIELD_FROM_NODE_NAME);
        }
        String nodeType = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_TYPE);
        String nodeName = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_NAME);
        String nodeUid = nodeDao.lookupUniqueIdForName(nodeType, nodeName);
        serializedEdge.put(EdgeDAO.FIELD_FROM_NODE_UID, nodeUid);
      }

      //If no 'to' node UID is specified, then locate it from the name field
      if (!serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_UID)) {
        if (!serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_NAME)) {
          throw new LogPlayerException("Can't play operation: "+item.getOp()
                  + ". You must set at least at least one of these: " 
                  + EdgeDAO.FIELD_TO_NODE_UID +", " + EdgeDAO.FIELD_TO_NODE_NAME);
        }
        String nodeType = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_TYPE);
        String nodeName = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_NAME);
        String nodeUid = nodeDao.lookupUniqueIdForName(nodeType, nodeName);
        serializedEdge.put(EdgeDAO.FIELD_TO_NODE_UID, nodeUid);
      }


      edgeDao.store(serializedEdge);
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }

}
