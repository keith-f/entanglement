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

package uk.ac.ncl.aries.entanglement.player.spi;

import static uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO.FIELD_UID;
import static uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO.FIELD_TYPE;
import static uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO.FIELD_NAME;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.FixHangingEdge;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 * 
 * @author Keith Flanagan
 */
public class FixHangingEdgePlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger =
          Logger.getLogger(FixHangingEdgePlayer.class.getName());
  
  private static final Set<String> EDGE_SPECIAL_FIELDS = 
    new HashSet<>(Arrays.asList(new String[]{ 
    FIELD_UID, FIELD_TYPE, FIELD_NAME,
    EdgeDAO.FIELD_FROM_NODE_NAME, EdgeDAO.FIELD_FROM_NODE_TYPE, EdgeDAO.FIELD_FROM_NODE_UID,
    EdgeDAO.FIELD_TO_NODE_NAME, EdgeDAO.FIELD_TO_NODE_TYPE, EdgeDAO.FIELD_TO_NODE_UID
  }));
  
  @Override
  public void initialise(ClassLoader cl, Mongo mongo, DB db)
  {
  }
  
  @Override
  public String getSupportedLogItemType()
  {
    return FixHangingEdge.class.getSimpleName();
  }
  
  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      FixHangingEdge command = (FixHangingEdge) item.getOp();
      
      BasicDBObject edge = edgeDao.getByUid(command.getEdgeUid());
      if (edge == null) {
        if (command.isErrOnMissingEdge()) {
          throw new LogPlayerException(
                  "Attempt to fix hanging edge: "+command.getEdgeUid() 
                  + ", but no such edge exists.");
        } else {
          return; //Ignore missing edges
        }
      }
      
      boolean hanging = edge.getBoolean(EdgeDAO.FIELD_HANGING);
      if (!hanging) {
        throw new LogPlayerException(
            "Attempt to fix hanging edge: "+command.getEdgeUid() 
            + ", however, the edge 'hanging' status was: "+hanging);
      }
      
      edge.put(EdgeDAO.FIELD_FROM_NODE_NAME, command.getFromName());
      edge.put(EdgeDAO.FIELD_FROM_NODE_TYPE, command.getFromType());
      edge.put(EdgeDAO.FIELD_FROM_NODE_UID, command.getFromUid());

      edge.put(EdgeDAO.FIELD_TO_NODE_NAME, command.getToName());
      edge.put(EdgeDAO.FIELD_TO_NODE_TYPE, command.getToType());
      edge.put(EdgeDAO.FIELD_TO_NODE_UID, command.getToUid());

      hanging = checkHangingStatus(nodeDao, edge);
      logger.info("Updated hanging status: "+hanging);
      
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }
  
  private boolean checkHangingStatus(NodeDAO nodeDao, BasicDBObject serializedEdge) throws GraphModelException
  {
    //Check that both node's type names are set
    if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_TYPE) ||
        !serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_TYPE)) {
      return true;
    }
    
    //If no 'from' node UID is specified, then locate it from the name field
    boolean verifiedFromExists = false;
    if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_UID)) {
      if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_NAME)) {
        return true;
      }
      String nodeType = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_TYPE);
      String nodeName = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_NAME);
      String nodeUid = nodeDao.lookupUniqueIdForName(nodeType, nodeName);
      if (nodeUid == null) {
        return true; //This graph node doesn't exist
      }
      serializedEdge.put(EdgeDAO.FIELD_FROM_NODE_UID, nodeUid);
      verifiedFromExists = true;
    }

    //If no 'to' node UID is specified, then locate it from the name field
    boolean verifiedToExists = false;
    if (!serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_UID)) {
      if (!serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_NAME)) {
        return true;
      }
      String nodeType = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_TYPE);
      String nodeName = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_NAME);
      String nodeUid = nodeDao.lookupUniqueIdForName(nodeType, nodeName);
      if (nodeUid == null) {
        return true; //This graph node doesn't exist
      }
      serializedEdge.put(EdgeDAO.FIELD_TO_NODE_UID, nodeUid);
      verifiedToExists = true;
    }
    
    /*
     * Check that the linked nodes exist
     */
    if (!verifiedFromExists) {
      String fromUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
      boolean exists = nodeDao.existsByUid(fromUid);
      if (!exists) {
        return true;
      }
    }
    
    if (!verifiedToExists) {
      String toUid = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_UID);
      boolean exists = nodeDao.existsByUid(toUid);
      if (!exists) {
        return true;
      }
    }
    
    serializedEdge.put(EdgeDAO.FIELD_HANGING, false);
    return false;
  }


}
