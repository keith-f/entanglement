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


import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoObjectParsers;
import com.mongodb.*;

import static com.entanglementgraph.graph.AbstractGraphEntityDAO.FIELD_KEYS;

import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.revlog.commands.NodeModification;
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
  private static final Logger logger = Logger.getLogger(NodeModificationPlayer.class.getName());

//  private static final String FIELD_KEYS = "keys";
  
  /*
   * These are set for every time <code>playItem</code> is called.
   */
  // The currently playing revision item
  private RevisionItem item;
  // The command wrapped by the RevisionItem
  private NodeModification command;
  // A MongoDB document embedded within the command that represents the graph entity being updated.
  private BasicDBObject reqSerializedNode;
  // The deserialized key field from the reqSerializedNode. This identifies the object to be created/updated.
  private EntityKeys reqKeyset;
  
  @Override
  public String getSupportedLogItemType()
  {
    return NodeModification.class.getSimpleName();
  }

  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    try {
      this.item = item;
      command = (NodeModification) item.getOp();
      reqSerializedNode = command.getNode();

      reqKeyset = MongoObjectParsers.parseKeyset(marshaller, reqSerializedNode.getString(FIELD_KEYS) );

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
  private void validateKeyset(EntityKeys keyset) throws LogPlayerException {
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
    try {
      // Does the entity exist by any key specified in the reference?
      boolean exists = nodeDao.existsByKey(keyset);

      // No - create a new document
      if (!exists) {
        // Create a new node and then exit
        createNewNode();
      }
      // Yes  - update existing document
      else {

        updateExistingNode();
      }
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to play back command. Command was: "+command.toString(), e);
    }
  }

  /**
   * Called when a node is found to not exist already - we need to create it.
   */
  private void createNewNode() throws GraphModelException, LogPlayerException
  {
    nodeDao.store(reqSerializedNode);
  }

  private void updateExistingNode() throws LogPlayerException {
    try {
      // Edit existing node - need to perform a merge based on
      BasicDBObject existing = nodeDao.getByKey(reqKeyset);
      switch(command.getMergePol()) {
        case NONE:
//          logger.log(Level.INFO, "Ignoring existing node: {0}", reqKeyset);
          break;
        case ERR:
          throw new LogPlayerException("A node with one or more items in the following keyset already exists: "+reqKeyset);
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
      throw new LogPlayerException("Failed to perform update on node with keyset: "+reqKeyset, e);
    }
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
    try {
      // Deserialize the keyset field of the existing object.
      EntityKeys existingKeyset = MongoObjectParsers.parseKeyset(marshaller, existing.getString(FIELD_KEYS));

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(existing.toMap());

      for (String key : reqSerializedNode.keySet()) {
        if (updated.containsField(key)) {
          continue; //Don't overwrite existing properties
        }
        //Set fields that exist in the request, but not in the existing object
        updated.put(key, reqSerializedNode.get(key));
      }

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, reqKeyset);

      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      nodeDao.update(updated);
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'append new, leave existing' operation on existing node: "+existing, e);
    }
  }
  
  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties. 
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doAppendNewOverwriteExisting(BasicDBObject existing)
          throws GraphModelException
  {
    try {
      // Deserialize the keyset field of the existing object.
      EntityKeys existingKeyset = MongoObjectParsers.parseKeyset(marshaller, existing.getString(FIELD_KEYS));


      BasicDBObject updated = new BasicDBObject();
      updated.putAll(existing.toMap());

      for (String key : reqSerializedNode.keySet()) {
        //Set all fields that exist in the request, regardless of whether they're present in the existing object
        updated.put(key, reqSerializedNode.get(key));
      }

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, reqKeyset);

      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      nodeDao.update(updated);
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'append new, overwrite existing' operation on existing node: "+existing, e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
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
      EntityKeys existingKeyset = MongoObjectParsers.parseKeyset(marshaller, existing.getString(FIELD_KEYS));

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(reqSerializedNode.toMap());

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, reqKeyset);

      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      nodeDao.update(updated);
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
