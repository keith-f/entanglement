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


import com.mongodb.BasicDBList;
import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_UID;
import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_TYPE;
import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_NAMES;

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
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.revlog.commands.IdentificationType;
import com.entanglementgraph.revlog.data.RevisionItem;

/**
 * Creates a node without performing any checks regarding whether it already
 * exists. 
 * 
 * @author Keith Flanagan
 */
public class NodeModificationPlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger =
          Logger.getLogger(NodeModificationPlayer.class.getName());
  
  private static final Set<String> NODE_SPECIAL_FIELDS = 
    new HashSet<>(Arrays.asList(new String[]{ FIELD_UID, FIELD_TYPE }));
  
  
  /*
   * These are set for every time <code>playItem</code> is called.
   */
  private NodeDAO nodeDao;
  private EdgeDAO edgeDao;
  // The currently playing revision item
  private RevisionItem item;
  // The command wrapped by the RevisionItem
  private NodeModification command;
  // A MongoDB document embedded within the command that represents the graph entity being updated.
  private BasicDBObject serializedNode;
  
  @Override
  public void initialise(ClassLoader cl, Mongo mongo, DB db)
  {
  }
  
  @Override
  public String getSupportedLogItemType()
  {
    return NodeModification.class.getSimpleName();
  }

  @Override
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException
  {
    try {
      this.nodeDao = nodeDao;
      this.edgeDao = edgeDao;
      command = (NodeModification) item.getOp();
      serializedNode = command.getNode();

      switch(command.getIdType()) {
        case UID:
          createOrModifyByUid();
          break;
        case NAME:
          createOrModifyByName();
          break;
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
   *
   */
  private void createOrModifyByUid()
          throws LogPlayerException
  {
    try {
      String uid = serializedNode.getString(FIELD_UID);
      if (uid == null) {
        throw new LogPlayerException(
              RevisionItem.class.getName()+" had no UID set. Item was: "+item);
      }

      if (!nodeDao.existsByUid(uid)) {
        // Create a new node and then exit
        createNewNode();
      } else {
        // Edit existing node - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, "Ignoring existing node: {0}", uid);
            break;
          case ERR:
            throw new LogPlayerException("A node with UID: "+uid+" already exists.");
          case APPEND_NEW__LEAVE_EXISTING:
            existing = nodeDao.getByUid(serializedNode.getString(FIELD_UID));
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = nodeDao.getByUid(serializedNode.getString(FIELD_UID));
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = nodeDao.getByUid(serializedNode.getString(FIELD_UID));
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
      String entityType = serializedNode.getString(FIELD_TYPE);
//      String entityName = serializedNode.getString(FIELD_NAME);
      BasicDBList entityNamesDbList = (BasicDBList) serializedNode.get(FIELD_NAMES);

      if (entityType == null) {
        throw new LogPlayerException(RevisionItem.class.getName() + " had no entity type set. Item was: " + item);
      }
      if (entityNamesDbList == null) {
        throw new LogPlayerException(RevisionItem.class.getName() + " had no entity name set. Item was: " + item);
      }
      
      Set<String> entityNames = new HashSet(entityNamesDbList);

      //if (!nodeDao.existsByName(entityType, entityName)) {
      if (!nodeDao.existsByAnyName(entityType, entityNames)) {
        // Create a new node
        createNewNode();
      } else {
        // Edit existing node - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, "Ignoring existing node: {0} with name(s) {1}", new Object[]{entityType, entityNames});
            break;
          case ERR:
            throw new LogPlayerException("A node with Type: "+entityType+" and name(s): "+entityNames+" already exists.");
          case APPEND_NEW__LEAVE_EXISTING:
            existing = nodeDao.getByAnyName(serializedNode.getString(FIELD_TYPE), entityNames);
//                    serializedNode.getString(FIELD_NAME));
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = nodeDao.getByAnyName(serializedNode.getString(FIELD_TYPE), entityNames);
//                    serializedNode.getString(FIELD_NAME));
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = nodeDao.getByAnyName(serializedNode.getString(FIELD_TYPE), entityNames);
//                    serializedNode.getString(FIELD_NAME));
            doOverwriteAll(existing);
            break;
          default:
            throw new LogPlayerException("Unsupported merge policy type: "+command.getMergePol());
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
   * Called when a node is found to not exist already - we need to create it.
   */
  private void createNewNode() throws GraphModelException, LogPlayerException
  {
    // Node type is a required property
    if (!serializedNode.containsField(FIELD_TYPE)) {
      throw new LogPlayerException("Can't play operation: "+item.getOp()
              + ". Property " + FIELD_TYPE + " was not set.");
    }
    /*
     * Generate a UID if one doesn't exist already (specifying a UID isn't 
     * required if the caller has specified a type/name instead)
     */
    if (!serializedNode.containsField(FIELD_UID)) {
      serializedNode.put(FIELD_UID, UidGenerator.generateUid());
    }
    nodeDao.store(serializedNode);
  }
  
  /**
   * This method adds new properties to an existing node. Where there are
   * properties on the existing node with the same name as the ones specified
   * in the update command, we leave the existing values as they are.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doAppendNewLeaveExisting(BasicDBObject existing)
          throws GraphModelException
  {
    _checkAllImmutableFieldsAreEqual(existing);
    _mergeNames(existing); // Allow new names to be added to an existing node

    for (String key : serializedNode.keySet()) {
      if (NODE_SPECIAL_FIELDS.contains(key)) {
        continue; //Skip immutable identity/type fields
      }
      if (existing.containsField(key)) {
        continue; //Don't overwrite existing properties
      }
      existing.append(key, serializedNode.get(key));
    }
    nodeDao.update(existing);
  }
  
  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doAppendNewOverwriteExisting(BasicDBObject existing)
          throws GraphModelException
  {
    _checkAllImmutableFieldsAreEqual(existing);
    _mergeNames(existing); // Allow new names to be added to an existing node

    for (String key : serializedNode.keySet()) {
      if (NODE_SPECIAL_FIELDS.contains(key)) {
        continue; //Skip immutable identity/type fields
      }
      existing.append(key, serializedNode.get(key));
    }
    nodeDao.update(existing);
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doOverwriteAll(BasicDBObject existing)
          throws GraphModelException
  {
    _checkAllImmutableFieldsAreEqual(existing);
    _mergeNames(existing); // Allow new names to be added to an existing node
    
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to ensure that the UID/type/name properties are carried 
     * over from the existing object.
     */
    for (String fieldName : NODE_SPECIAL_FIELDS) {
      serializedNode.put(fieldName, existing.get(fieldName));
    }
    serializedNode.put(FIELD_NAMES, existing.get(FIELD_NAMES)); //Copy merged name list

    nodeDao.update(serializedNode);
  }

  private void _mergeNames(BasicDBObject existing) {
    // A MongoDB list containing the existing (known) names for this entity
    BasicDBList namesDbList = (BasicDBList) existing.get(FIELD_NAMES);
    // A list containing (potentially) new names for this entity
    BasicDBList newEntityNamesDbList = (BasicDBList) serializedNode.get(FIELD_NAMES);

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
    for (String fieldName : NODE_SPECIAL_FIELDS) {
      _checkImmutableFieldIsEqual(fieldName, existing);
    }
  }
  
  private void _checkImmutableFieldIsEqual(String fieldName, BasicDBObject existing)
          throws GraphModelException
  {
    if (serializedNode.containsField(fieldName)) {
      String existingFieldVal = existing.getString(fieldName);
      String newFieldVal = serializedNode.getString(fieldName);
      if (!existingFieldVal.equals(newFieldVal)) {
        throw new GraphModelException(
                "You cannot update the immutable field: "+fieldName+": "
                +existingFieldVal+" != "+newFieldVal);
      }
    }
  }
  
}
