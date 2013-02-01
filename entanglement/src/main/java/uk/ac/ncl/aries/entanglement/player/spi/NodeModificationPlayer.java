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


import static uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO.FIELD_UID;
import static uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO.FIELD_TYPE;
import static uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO.FIELD_NAME;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.torrenttamer.util.UidGenerator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.graph.AbstractGraphEntityDAO;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.NodeModification;
import uk.ac.ncl.aries.entanglement.revlog.commands.MergePolicy;
import uk.ac.ncl.aries.entanglement.revlog.commands.IdentificationType;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

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
    new HashSet<>(Arrays.asList(new String[]{ FIELD_UID, FIELD_TYPE, FIELD_NAME}));
 
  
  
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
   * @param nodeDao
   * @param edgeDao
   * @param item
   * @param cn 
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
      String entityName = serializedNode.getString(FIELD_NAME);
      if (entityType == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no entity type set. Item was: " + item);
      }
      if (entityName == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no entity name set. Item was: " + item);
      }

      if (!nodeDao.existsByName(entityType, entityName)) {
        // Create a new node
        createNewNode();
      } else {
        // Edit existing node - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, "Ignoring existing node: {0}, {1}", new Object[]{entityType, entityName});
            break;
          case ERR:
            throw new LogPlayerException("A node with Type: "+entityType
                    +" and Name: "+entityName+" already exists.");
          case APPEND_NEW__LEAVE_EXISTING:
            existing = nodeDao.getByName(
                    serializedNode.getString(FIELD_TYPE), 
                    serializedNode.getString(FIELD_NAME));
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = nodeDao.getByName(
                    serializedNode.getString(FIELD_TYPE), 
                    serializedNode.getString(FIELD_NAME));
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = nodeDao.getByName(
                    serializedNode.getString(FIELD_TYPE), 
                    serializedNode.getString(FIELD_NAME));
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
//    BasicDBObject existing = nodeDao.getByUid(serializedNode.getString(FIELD_UID));
    _checkAllImmutableFieldsAreEqual(existing);

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
    
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to ensure that the UID/type/name properties are carried 
     * over from the existing object.
     */
//    serializedNode.put(FIELD_UID, existing.getString(FIELD_UID));
//    serializedNode.put(FIELD_TYPE, existing.getString(FIELD_TYPE));
//    serializedNode.put(FIELD_NAME, existing.getString(FIELD_NAME));
    for (String fieldName : NODE_SPECIAL_FIELDS) {
      serializedNode.put(fieldName, existing.getString(fieldName));
    }

    nodeDao.update(serializedNode);
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
                "You cannot update the immutbable field: "+fieldName+": "
                +existingFieldVal+" != "+newFieldVal);
      }
    }
  }
  
}
