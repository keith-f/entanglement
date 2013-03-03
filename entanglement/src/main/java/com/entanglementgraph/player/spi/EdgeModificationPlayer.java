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

package com.entanglementgraph.player.spi;

import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_UID;
import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_TYPE;
import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_NAMES;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.torrenttamer.util.UidGenerator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.revlog.commands.EdgeModification;
import com.entanglementgraph.revlog.commands.IdentificationType;
import com.entanglementgraph.revlog.data.RevisionItem;

/**
 * Creates an edge.
 * 
 * @author Keith Flanagan
 */
public class EdgeModificationPlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger =
          Logger.getLogger(EdgeModificationPlayer.class.getName());
  
  private static final Set<String> EDGE_SPECIAL_FIELDS = 
    new HashSet<>(Arrays.asList(new String[]{ 
    FIELD_UID, FIELD_TYPE, //FIELD_NAME,
    EdgeDAO.FIELD_FROM_NODE_TYPE, EdgeDAO.FIELD_FROM_NODE_UID,
    EdgeDAO.FIELD_TO_NODE_TYPE, EdgeDAO.FIELD_TO_NODE_UID
  }));
  
  /*
   * These are set for every time <code>playItem</code> is called.
   */
  private NodeDAO nodeDao;
  private EdgeDAO edgeDao;
  // The currently playing revision item
  private RevisionItem item;
  // The command wrapped by the RevisionItem
  private EdgeModification command;
  // A MongoDB document embedded within the command that represents the graph entity being updated.
  private BasicDBObject serializedEdge;
  
  @Override
  public void initialise(ClassLoader cl, Mongo mongo, DB db)
  {
  }
  
  @Override
  public String getSupportedLogItemType()
  {
    return EdgeModification.class.getSimpleName();
  }
  
  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      this.nodeDao = nodeDao;
      this.edgeDao = edgeDao;
      command = (EdgeModification) item.getOp();
      serializedEdge = command.getEdge();

      switch(command.getIdType()) {
        case UID:
          createOrModifyByUid();
          break;
        case NAME:
          createOrModifyByName();
          break;
//        case EDGE_TYPE_CONNECTED_VIA_NODE_UIDS:
//          createOrModifyByEdgeTypeToNodeUids();
//          break;
//        case EDGE_TYPE_CONNECTED_VIA_NODE_NAMES:
//          createOrModigyByEdgeTypeToNodeNames();
//          break;
        default:
          throw new LogPlayerException("Unsupported "
            + IdentificationType.class.getName() + " type: " + command.getIdType());
      }
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }

  
  /**
   * Called if the graph operation uses the UID field for identification.
   */
  private void createOrModifyByUid()
          throws LogPlayerException
  {
    try {
      String uid = serializedEdge.getString(FIELD_UID);
      if (uid == null) {
        throw new LogPlayerException(
              RevisionItem.class.getName()+" had no UID set. Item was: "+item);
      }

      if (!edgeDao.existsByUid(uid)) {
        // Create a new edge and then exit
        createNewEdge();
      } else {
        // Edit existing edge - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, "Ignoring existing edge: {0}", uid);
            break;
          case ERR:
            throw new LogPlayerException("An edge with UID: "+uid+" already exists.");
          case APPEND_NEW__LEAVE_EXISTING:
            existing = edgeDao.getByUid(serializedEdge.getString(FIELD_UID));
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = edgeDao.getByUid(serializedEdge.getString(FIELD_UID));
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = edgeDao.getByUid(serializedEdge.getString(FIELD_UID));
            doOverwriteAll(existing);
            break;
          default:
            throw new LogPlayerException(
                    "Unsupported merge policy type: "+command.getMergePol());
        }
      }
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to play back command using "
              +IdentificationType.class.getName()+": "+command.getIdType()
              + ". Command was: "+command.toString(), e);
    }
  }
  
  private void createOrModifyByName() throws LogPlayerException
  {
    try {
      String entityType = serializedEdge.getString(FIELD_TYPE);
//      String entityName = serializedEdge.getString(FIELD_NAME);
      BasicDBList entityNamesDbList = (BasicDBList) serializedEdge.get(FIELD_NAMES);

      if (entityType == null) {
        throw new LogPlayerException(RevisionItem.class.getName() + " had no entity type set. Item was: " + item);
      }
      if (entityNamesDbList == null) {
        throw new LogPlayerException(RevisionItem.class.getName() + " had no entity name set. Item was: " + item);
      }

      Set<String> entityNames = new HashSet(entityNamesDbList);

      if (!edgeDao.existsByAnyName(entityType, entityNames)) {
        // Create a new edge
        createNewEdge();
      } else {
        // Edit existing edge - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, "Ignoring existing edge: {0}, with name(s) {1}", new Object[]{entityType, entityNames});
            break;
          case ERR:
            throw new LogPlayerException("An edge with Type: "+entityType
                    +" and Name(s): "+entityNames+" already exists.");
          case APPEND_NEW__LEAVE_EXISTING:
            existing = edgeDao.getByAnyName(serializedEdge.getString(FIELD_TYPE), entityNames);
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = edgeDao.getByAnyName(serializedEdge.getString(FIELD_TYPE), entityNames);
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = edgeDao.getByAnyName(serializedEdge.getString(FIELD_TYPE), entityNames);
            doOverwriteAll(existing);
            break;
          default:
            throw new LogPlayerException(
                    "Unsupported merge policy type: "+command.getMergePol());
        }
      }
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to play back command using "
              +IdentificationType.class.getName()+": "+command.getIdType()
              + ". Command was: "+command.toString(), e);
    }
  }
  
  /**
   * Called when an edge is found to not exist already - we need to create it.
   */
  private void createNewEdge() throws GraphModelException, LogPlayerException
  {
    // Edge type is a required property
    if (!serializedEdge.containsField(FIELD_TYPE)) {
      throw new LogPlayerException("Can't play operation: "+item.getOp()
              + ". Property " + FIELD_TYPE + " was not set.");
    }
    /*
     * Generate a UID if one doesn't exist already (specifying a UID isn't 
     * required if the caller has specified a type/name instead)
     */
    if (!serializedEdge.containsField(FIELD_UID)) {
      serializedEdge.put(FIELD_UID, UidGenerator.generateUid());
    }
    
    /*
     * Edges MUST have a UID, and a type, and must also specify to/from node UIDs (unless the edge is specified as to
     * be a 'hanging' edge.
     *
     * In the case of a hanging edge, the to/from node UIDs my be either be omitted, or point to a non-existent node.
     *
     */
    
    if (command.isAllowHanging()) {
      serializedEdge.put(EdgeDAO.FIELD_HANGING, true);  
    } 
    else 
    {
      serializedEdge.put(EdgeDAO.FIELD_HANGING, false);
    
      //Check that both node's type names are set
      if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_TYPE) ||
          !serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_TYPE)) {
        throw new LogPlayerException("Can't play operation: "+item.getOp()
                + ". Either " + EdgeDAO.FIELD_FROM_NODE_TYPE +" or "
                + EdgeDAO.FIELD_TO_NODE_TYPE + " were not set.");
      }

      // Check that the from/to node UID fields are set
      String fromUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
      if (fromUid == null) {
        throw new LogPlayerException(
            "While creating an edge, allowHanging was set to "+command.isAllowHanging()
                + ", and the 'from' node UID was NULL.");
      }

      String toUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
      if (toUid == null) {
        throw new LogPlayerException(
            "While creating an edge, allowHanging was set to "+command.isAllowHanging()
                + ", and the 'to' node UID was NULL.");
      }

      // Check that from/to nodes exist
      if (!nodeDao.existsByUid(fromUid)) {
        throw new LogPlayerException(
            "While creating an edge, allowHanging was set to "+command.isAllowHanging()
                + ", and the 'from' node: "+fromUid+" doesn't exist!");
      }
      if (!nodeDao.existsByUid(toUid)) {
        throw new LogPlayerException(
            "While creating an edge, allowHanging was set to "+command.isAllowHanging()
                + ", and the 'from' node: "+toUid+" doesn't exist!");
      }
    } //End !command.isAllowHanging()
    
    /*
     * Finally, store the edge
     */
    edgeDao.store(serializedEdge);
  }
  
  
  /**
   * This method adds new properties to an existing edge. Where there are
   * properties on the existing edge with the same name as the ones specified
   * in the update command, we leave the existing values as they are.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doAppendNewLeaveExisting(BasicDBObject existing)
          throws GraphModelException
  {
    _checkAllImmutableFieldsAreEqual(existing);
    _mergeNames(existing); // Allow new names to be added to an existing edge

    for (String key : serializedEdge.keySet()) {
      if (EDGE_SPECIAL_FIELDS.contains(key)) {
        continue; //Skip immutable identity/type fields
      }
      if (existing.containsField(key)) {
        continue; //Don't overwrite existing properties
      }
      existing.append(key, serializedEdge.get(key));
    }
    edgeDao.update(existing);
  }
  
  /**
   * This method adds new properties to an existing edge and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doAppendNewOverwriteExisting(BasicDBObject existing)
          throws GraphModelException
  {
    _checkAllImmutableFieldsAreEqual(existing);
    _mergeNames(existing); // Allow new names to be added to an existing edge

    for (String key : serializedEdge.keySet()) {
      if (EDGE_SPECIAL_FIELDS.contains(key)) {
        continue; //Skip immutable identity/type fields
      }
      existing.append(key, serializedEdge.get(key));
    }
    edgeDao.update(existing);
  }

  /**
   * This method adds new properties to an existing edge and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doOverwriteAll(BasicDBObject existing)
          throws GraphModelException
  {
    _checkAllImmutableFieldsAreEqual(existing);
    _mergeNames(existing); // Allow new names to be added to an existing edge
    
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to ensure that the UID/type/name properties are carried 
     * over from the existing object.
     */
    for (String fieldName : EDGE_SPECIAL_FIELDS) {
      serializedEdge.put(fieldName, existing.getString(fieldName));
    }
    serializedEdge.put(FIELD_NAMES, existing.get(FIELD_NAMES)); //Copy merged name list

    edgeDao.update(serializedEdge);
  }

  private void _mergeNames(BasicDBObject existing) {
    // A MongoDB list containing the existing (known) names for this entity
    BasicDBList namesDbList = (BasicDBList) existing.get(FIELD_NAMES);
    // A list containing (potentially) new names for this entity
    BasicDBList newEntityNamesDbList = (BasicDBList) serializedEdge.get(FIELD_NAMES);

    //Not elegant, but there doesn't appear to be a better way...
    for (Object newName : newEntityNamesDbList) {
      if (!namesDbList.contains(newName)) {
        namesDbList.add(newName);
      }
    }

    // Save merged list
    existing.put(FIELD_NAMES, namesDbList);
  }
  
  private void _checkAllImmutableFieldsAreEqual(BasicDBObject existing) throws GraphModelException
  {
    for (String fieldName : EDGE_SPECIAL_FIELDS) {
      _checkImmutableFieldIsEqual(fieldName, existing);
    }
  }
  
  private void _checkImmutableFieldIsEqual(String fieldName, BasicDBObject existing)
          throws GraphModelException
  {
    if (serializedEdge.containsField(fieldName)) {
      String existingFieldVal = existing.getString(fieldName);
      String newFieldVal = serializedEdge.getString(fieldName);
      if (!existingFieldVal.equals(newFieldVal)) {
        throw new GraphModelException(
                "You cannot update the immutbable field: "+fieldName+": "
                +existingFieldVal+" != "+newFieldVal);
      }
    }
  }
  

}
