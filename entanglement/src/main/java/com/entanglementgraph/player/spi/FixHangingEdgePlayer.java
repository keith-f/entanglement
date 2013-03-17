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

package com.entanglementgraph.player.spi;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;

import java.util.logging.Logger;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.revlog.commands.FixHangingEdge;
import com.entanglementgraph.revlog.data.RevisionItem;

/**
 * 
 * @author Keith Flanagan
 */
public class FixHangingEdgePlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger = Logger.getLogger(FixHangingEdgePlayer.class.getName());
  
  @Override
  public String getSupportedLogItemType()
  {
    return FixHangingEdge.class.getSimpleName();
  }
  
  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    try {
      FixHangingEdge command = (FixHangingEdge) item.getOp();
      
      BasicDBObject edge = edgeDao.getByUid(command.getEdgeUid());
      if (edge == null) {
        if (command.isErrOnMissingEdge()) {
          throw new LogPlayerException(
                  "Attempt to fix hanging edge: "+command.getEdgeUid() + ", but no such edge exists.");
        } else {
          return; //Ignore missing edges
        }
      }
      
      boolean hanging = edge.getBoolean(EdgeDAO.FIELD_HANGING);
      if (!hanging) {
        logger.info("Ignoring attempt to fix hanging edge: "+command.getEdgeUid()
            + " because actual 'hanging' status was: "+hanging);
        return;
      }

      if (command.getFromType() == null || command.getFromUid() == null ||
          command.getToType() == null || command.getToUid() == null) {
        throw new LogPlayerException("One or more of the required fields were not set");
      }

      edge.put(EdgeDAO.FIELD_FROM_NODE_TYPE, command.getFromType());
      edge.put(EdgeDAO.FIELD_FROM_NODE_UID, command.getFromUid());

      edge.put(EdgeDAO.FIELD_TO_NODE_TYPE, command.getToType());
      edge.put(EdgeDAO.FIELD_TO_NODE_UID, command.getToUid());

      edge.put(EdgeDAO.FIELD_FROM_NODE_TYPE, command.getFromType());
      edge.put(EdgeDAO.FIELD_FROM_NODE_UID, command.getFromUid());

      hanging = checkHangingStatus(nodeDao, edge);
      logger.info("Still hanging: "+hanging);

      // Update edge in all cases, since we might have added UIDs etc that will be valid in future
      edgeDao.update(edge);
      
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }
  
  private boolean checkHangingStatus(NodeDAO nodeDao, BasicDBObject serializedEdge) throws GraphModelException
  {
    // Check that the from/to node UID fields are set
    String fromUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
    if (fromUid == null) {
      return true;
    }

    String toUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
    if (toUid == null) {
      return true;
    }

    // Check that from/to nodes exist
    if (!nodeDao.existsByUid(fromUid)) {
      return true;
    }
    if (!nodeDao.existsByUid(toUid)) {
      return true;
    }
    
    serializedEdge.put(EdgeDAO.FIELD_HANGING, false);
    return false;
  }


}
