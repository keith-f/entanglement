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

import static com.entanglementgraph.graph.GraphEntityDAO.FIELD_KEYS;

import com.entanglementgraph.graph.data.EntityKeys;
import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;

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
import com.entanglementgraph.revlog.data.RevisionItem;

/**
 * Creates an edge.
 * 
 * @author Keith Flanagan
 */
public class EdgeModificationPlayer 
    extends AbstractLogItemPlayer
{
  private static final Logger logger = Logger.getLogger(EdgeModificationPlayer.class.getName());

  /*
   * These are set for every time <code>playItem</code> is called.
   */
  // The currently playing revision item
  private RevisionItem item;
  // The command wrapped by the RevisionItem
  private EdgeModification command;
  // A MongoDB document embedded within the command that represents the graph entity being updated.
  private BasicDBObject reqSerializedEdge;
  // The deserialized key field from the reqSerializedEdge. This identifies the object to be created/updated.
  private EntityKeys reqKeyset;
  
  @Override
  public String getSupportedLogItemType()
  {
    return EdgeModification.class.getSimpleName();
  }

  private EntityKeys parseKeyset(DBObject dbObject, String fieldName) throws DbObjectMarshallerException {
    String jsonKeyset = dbObject.get(fieldName).toString();
    EntityKeys keyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);
    return keyset;
  }

  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    try {
      this.item = item;
      command = (EdgeModification) item.getOp();
      reqSerializedEdge = command.getEdge();

      //Deserialize 'key set' field from the entity because we'll need it.
      String jsonKeyset = reqSerializedEdge.get(FIELD_KEYS).toString();
      reqKeyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);

      //The reference field should contain at least one identification key
      validateKeyset(reqKeyset);

      createOrModify(reqKeyset);

    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }

  /*
   * To create or update an entity, then there must be at least one UID and/or at least one name.
   * If one or more names are specified, then a entity type must also be present.
   */
  private static void validateKeyset(EntityKeys keyset) throws LogPlayerException {
    if (keyset.getNames().isEmpty() && keyset.getUids().isEmpty()) {
      throw new LogPlayerException("You must specify at least one entity key (either a UID, or a type/name");
    }
    if (!keyset.getNames().isEmpty() && keyset.getType() == null) {
      throw new LogPlayerException("You specified one or more entity names, but did not specify a type. " +
          "Names were: "+keyset.getNames());
    }
  }


  private void createOrModify(EntityKeys keyset)
      throws LogPlayerException
  {
    logger.info("Attempting playback of entity: "+keyset);
    try {
      // Does the entity exist by any key specified in the reference?
      boolean exists = edgeDao.existsByKey(keyset);

      // No - create a new document
      if (!exists) {
        // Create a new node and then exit
        createNewEdge();
      }
      // Yes  - update existing document
      else {
        updateExistingEdge();
      }
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to play back command. Command was: "+command.toString(), e);
    }
  }

  /**
   * Called when an edge is found to not exist already - we need to create it.
   */
  private void createNewEdge() throws GraphModelException, LogPlayerException {
   /*
    * In the case of a hanging edge, the to/from node UIDs my be either be omitted, or point to a non-existent node.
    *
    */
    try {
      logger.info("Creating new edge in: "+edgeDao.getCollection().getFullName());

      //If the command definitely doesn't allow hanging edges, then we need to make sure that the edge isn't hanging.
      if (!command.isAllowHanging()) {
        //Check that both to/from node references are set and valid
        if (!reqSerializedEdge.containsField(EdgeDAO.FIELD_FROM) ||
            !reqSerializedEdge.containsField(EdgeDAO.FIELD_TO_NODE_TYPE)) {
          throw new LogPlayerException("Can't play operation: "
              + ". Either " + EdgeDAO.FIELD_FROM +" or "+ EdgeDAO.FIELD_TO + " were not set." +
              "\nOperation was: "+item.getOp());
        }

        EntityKeys from = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_FROM);
        validateKeyset(from);

        EntityKeys to = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_TO);
        validateKeyset(to);

        // Check that from/to nodes exist
        if (!nodeDao.existsByKey(from)) {
          throw new LogPlayerException(
              "While creating an edge, allowHanging was set to "+command.isAllowHanging()
                  + ", and the 'from' node doesn't exist: "+from);
        }
        if (!nodeDao.existsByKey(to)) {
          throw new LogPlayerException(
              "While creating an edge, allowHanging was set to "+command.isAllowHanging()
                  + ", and the 'to' node doesn't exist: "+to);
        }

        // We've proved that this edge isn't hanging, so set the appropriate edge property.
        reqSerializedEdge.put(EdgeDAO.FIELD_HANGING, false);
      } //End !command.isAllowHanging()

      /*
       * Finally, store the edge
       */
      edgeDao.store(reqSerializedEdge);
    } catch(Exception e) {
      throw new LogPlayerException("Failed to create an edge. Command was: "+command.toString(), e);
    }
  }

  private void updateExistingEdge() throws LogPlayerException {
    try {
      // Edit existing edge
      BasicDBObject existing = edgeDao.getByKey(reqKeyset);
      switch(command.getMergePol()) {
        case NONE:
          logger.log(Level.INFO, "Ignoring existing edge: {0}", reqKeyset);
          break;
        case ERR:
          throw new LogPlayerException("An edge with one or more items in the following keyset already exists: "+reqKeyset);
        case APPEND_NEW__LEAVE_EXISTING:
          doAppendNewLeaveExisting(existing);
          break;
        case APPEND_NEW__OVERWRITE_EXSITING:
          doAppendNewOverwriteExisting(existing);
          break;
        case OVERWRITE_ALL:
          doOverwriteAll(existing);
          break;
        default:
          throw new LogPlayerException("Unsupported merge policy type: "+command.getMergePol());
      }
    } catch (Exception e) {
      throw new LogPlayerException("Failed to perform update on edge with keyset: "+reqKeyset, e);
    }
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
    try {
      // Deserialize the keyset field of the existing object.
      EntityKeys existingKeyset = parseKeyset(existing, FIELD_KEYS);

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(existing.toMap());

      for (String key : reqSerializedEdge.keySet()) {
        if (updated.containsField(key)) {
          continue; //Don't overwrite existing properties
        }
        //Set fields that exist in the request, but not in the existing object
        updated.put(key, reqSerializedEdge.get(key));
      }

      // Allow new keys to be added to identify the edge, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, reqKeyset);
      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      // Also allow new keys to be added for the from/to node fields
      EntityKeys fromExistingKeyset = parseKeyset(existing, EdgeDAO.FIELD_FROM);
      EntityKeys fromReqKeyset = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_FROM);
      EntityKeys fromMergedKeys = _mergeKeys(fromExistingKeyset, fromReqKeyset);
      updated.put(EdgeDAO.FIELD_FROM, marshaller.serialize(fromMergedKeys));

      // Repeat for the 'to' node:
      EntityKeys toExistingKeyset = parseKeyset(existing, EdgeDAO.FIELD_TO);
      EntityKeys toReqKeyset = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_TO);
      EntityKeys toMergedKeys = _mergeKeys(toExistingKeyset, toReqKeyset);
      updated.put(EdgeDAO.FIELD_TO, marshaller.serialize(toMergedKeys));


      edgeDao.update(updated);
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'append new, leave existing' operation on existing node: "+existing, e);
    }
  }
  
  /**
   * This method adds new properties to an existing edge and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doAppendNewOverwriteExisting(BasicDBObject existing)
          throws GraphModelException
  {
    try {
      // Deserialize the keyset field of the existing object.
      String jsonExistingKeyset = existing.get(FIELD_KEYS).toString();
      EntityKeys existingKeyset = marshaller.deserialize(jsonExistingKeyset, EntityKeys.class);

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(existing.toMap());

      for (String key : reqSerializedEdge.keySet()) {
        //Set all fields that exist in the request, regardless of whether they're present in the existing object
        updated.put(key, reqSerializedEdge.get(key));
      }

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, reqKeyset);
      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      // Also allow new keys to be added for the from/to node fields
      EntityKeys fromExistingKeyset = parseKeyset(existing, EdgeDAO.FIELD_FROM);
      EntityKeys fromReqKeyset = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_FROM);
      EntityKeys fromMergedKeys = _mergeKeys(fromExistingKeyset, fromReqKeyset);
      updated.put(EdgeDAO.FIELD_FROM, marshaller.serialize(fromMergedKeys));

      // Repeat for the 'to' node:
      EntityKeys toExistingKeyset = parseKeyset(existing, EdgeDAO.FIELD_TO);
      EntityKeys toReqKeyset = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_TO);
      EntityKeys toMergedKeys = _mergeKeys(toExistingKeyset, toReqKeyset);
      updated.put(EdgeDAO.FIELD_TO, marshaller.serialize(toMergedKeys));

      edgeDao.update(updated);
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'append new, overwrite existing' operation on existing node: "+existing, e);
    }
  }

  /**
   * This method adds new properties to an existing edge and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doOverwriteAll(BasicDBObject existing)
          throws GraphModelException
  {
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to merge keys from the existing object.
     */
    try {
      // Deserialize the keyset field of the existing object.
      String jsonExistingKeyset = existing.get(FIELD_KEYS).toString();
      EntityKeys existingKeyset = marshaller.deserialize(jsonExistingKeyset, EntityKeys.class);

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(reqSerializedEdge.toMap());

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, reqKeyset);
      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      // Also allow new keys to be added for the from/to node fields
      EntityKeys fromExistingKeyset = parseKeyset(existing, EdgeDAO.FIELD_FROM);
      EntityKeys fromReqKeyset = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_FROM);
      EntityKeys fromMergedKeys = _mergeKeys(fromExistingKeyset, fromReqKeyset);
      updated.put(EdgeDAO.FIELD_FROM, marshaller.serialize(fromMergedKeys));

      // Repeat for the 'to' node:
      EntityKeys toExistingKeyset = parseKeyset(existing, EdgeDAO.FIELD_TO);
      EntityKeys toReqKeyset = parseKeyset(reqSerializedEdge, EdgeDAO.FIELD_TO);
      EntityKeys toMergedKeys = _mergeKeys(toExistingKeyset, toReqKeyset);
      updated.put(EdgeDAO.FIELD_TO, marshaller.serialize(toMergedKeys));

      edgeDao.update(updated);
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'overwrite' operation on existing node: "+existing, e);
    }
  }


  private static EntityKeys _mergeKeys(EntityKeys existingKeyset, EntityKeys newKeyset)
      throws DbObjectMarshallerException, GraphModelException {

    //If entity types mismatch, then we have a problem.
    if (newKeyset.getType() != null && existingKeyset.getType() != null &&
        !newKeyset.getType().equals(existingKeyset.getType())) {
      throw new GraphModelException("Attempt to merge existing keyset with keyset from current request failed. " +
          "The type fields are non-null and mismatch. Existing keyset: "+existingKeyset
          +". Request keyset: "+newKeyset);
    }

    EntityKeys merged = new EntityKeys();
    // Set entity type. Here, existing 'type' gets priority, followed by new type (just to be sure...)
    if (existingKeyset.getType() != null) {
      merged.setType(existingKeyset.getType());
    } else if (newKeyset.getType() != null) {
      merged.setType(newKeyset.getType());
    }

    // Merge UIDs
    merged.addUids(existingKeyset.getUids());
    merged.addUids(newKeyset.getUids());

    // Merge names
    merged.addNames(existingKeyset.getNames());
    merged.addNames(newKeyset.getUids());

    return merged;
  }

}
