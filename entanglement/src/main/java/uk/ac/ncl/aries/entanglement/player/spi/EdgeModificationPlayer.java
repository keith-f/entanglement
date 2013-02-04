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
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.torrenttamer.util.UidGenerator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.commands.EdgeModification;
import uk.ac.ncl.aries.entanglement.revlog.commands.IdentificationType;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

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
    FIELD_UID, FIELD_TYPE, FIELD_NAME,
    EdgeDAO.FIELD_FROM_NODE_NAME, EdgeDAO.FIELD_FROM_NODE_TYPE, EdgeDAO.FIELD_FROM_NODE_UID,
    EdgeDAO.FIELD_TO_NODE_NAME, EdgeDAO.FIELD_TO_NODE_TYPE, EdgeDAO.FIELD_TO_NODE_UID
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
        case EDGE_TYPE_CONNECTED_VIA_NODE_UIDS:
          createOrModifyByEdgeTypeToNodeUids();
          break;
        case EDGE_TYPE_CONNECTED_VIA_NODE_NAMES:
          createOrModigyByEdgeTypeToNodeNames();
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
      String entityName = serializedEdge.getString(FIELD_NAME);
      if (entityType == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no entity type set. Item was: " + item);
      }
      if (entityName == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no entity name set. Item was: " + item);
      }

      if (!edgeDao.existsByName(entityType, entityName)) {
        // Create a new edge
        createNewEdge();
      } else {
        // Edit existing edge - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, "Ignoring existing edge: {0}, {1}", new Object[]{entityType, entityName});
            break;
          case ERR:
            throw new LogPlayerException("An edge with Type: "+entityType
                    +" and Name: "+entityName+" already exists.");
          case APPEND_NEW__LEAVE_EXISTING:
            existing = edgeDao.getByName(
                    serializedEdge.getString(FIELD_TYPE), 
                    serializedEdge.getString(FIELD_NAME));
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = edgeDao.getByName(
                    serializedEdge.getString(FIELD_TYPE), 
                    serializedEdge.getString(FIELD_NAME));
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = edgeDao.getByName(
                    serializedEdge.getString(FIELD_TYPE), 
                    serializedEdge.getString(FIELD_NAME));
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
  
  
  
  private void createOrModifyByEdgeTypeToNodeUids() throws LogPlayerException
  {
    try {
      String entityType = serializedEdge.getString(FIELD_TYPE);
      String fromNodeUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
      String toNodeUid = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_UID);
      
      if (entityType == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no entity type set. Item was: " + item);
      }
      if (fromNodeUid == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no fromNodeUid set. Item was: " + item);
      }
      if (toNodeUid == null) {
        throw new LogPlayerException(RevisionItem.class.getName()
                + " had no toNodeUid set. Item was: " + item);
      }

      long edgeCount = edgeDao.countEdgesOfTypeBetweenNodes(
              entityType, fromNodeUid, toNodeUid);
      if (edgeCount == 0) {
        // Create a new edge
        createNewEdge();
      } else {
        // Edit of existing edge - need to perform a merge
        BasicDBObject existing;
        switch(command.getMergePol()) {
          case NONE:
            logger.log(Level.INFO, 
                "Ignoring existing edge of type: {0}, between node UIDs {1} and {2}", 
                new Object[]{entityType, fromNodeUid, toNodeUid});
            break;
          case ERR:
            throw new LogPlayerException(
                "Ignoring existing edge of type: "+entityType
                +", between node UIDs "+fromNodeUid+"and "+toNodeUid);
          case APPEND_NEW__LEAVE_EXISTING:
            existing = edgeDao.getByName(
                    serializedEdge.getString(FIELD_TYPE), 
                    serializedEdge.getString(FIELD_NAME));
            doAppendNewLeaveExisting(existing);
            break;
          case APPEND_NEW__OVERWRITE_EXSITING:
            existing = edgeDao.getByName(
                    serializedEdge.getString(FIELD_TYPE), 
                    serializedEdge.getString(FIELD_NAME));
            doAppendNewOverwriteExisting(existing);
            break;
          case OVERWRITE_ALL:
            existing = edgeDao.getByName(
                    serializedEdge.getString(FIELD_TYPE), 
                    serializedEdge.getString(FIELD_NAME));
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
    
    //Check that both node's type names are set
    if (!serializedEdge.containsField(EdgeDAO.FIELD_FROM_NODE_TYPE) ||
        !serializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_TYPE)) {
      throw new LogPlayerException("Can't play operation: "+item.getOp()
              + ". Either " + EdgeDAO.FIELD_FROM_NODE_TYPE +" or "
              + EdgeDAO.FIELD_TO_NODE_TYPE + " were not set.");
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
    
    /*
     * If necessary, check that the linked nodes exist before creating the edge
     */
    if (!command.isAllowDangling()) {
      String fromUid = serializedEdge.getString(EdgeDAO.FIELD_FROM_NODE_UID);
      String toUid = serializedEdge.getString(EdgeDAO.FIELD_TO_NODE_UID);
      if (!nodeDao.existsByUid(fromUid)) {
        throw new LogPlayerException(
                "While creating an edge, allowDangling was set to "+command.isAllowDangling()
                + ", and the 'from' node: "+fromUid+" doesn't exist!");
      }
      if (!nodeDao.existsByUid(toUid)) {
        throw new LogPlayerException(
                "While creating an edge, allowDangling was set to "+command.isAllowDangling()
                + ", and the 'to' node: "+toUid+" doesn't exist!");
      }
    }
    
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
    
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to ensure that the UID/type/name properties are carried 
     * over from the existing object.
     */
    for (String fieldName : EDGE_SPECIAL_FIELDS) {
      serializedEdge.put(fieldName, existing.getString(fieldName));
    }

    edgeDao.update(serializedEdge);
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
