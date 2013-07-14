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

package com.entanglementgraph.jiti;

import static com.entanglementgraph.util.MongoUtils.parseKeyset;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import static com.entanglementgraph.graph.GraphEntityDAO.FIELD_KEYS;

/**
 * A convenience class for merging two BasicDBObject instances that represent graph nodes. This functionality is
 * required in several places so has been extracted from its original location within NodeModificationPlayer.
 *
 * Nodes may be merged according to a <code>MergePolicy</code>, which determines which data fields are ignored,
 * overwritten, or combined. Elements of the object's identifier (UIDs and names of EntityKeys) are always merged.
 *
 * @author Keith Flanagan
 */
public class NodeMerger {

  private final DbObjectMarshaller marshaller;
  public NodeMerger(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }

  public BasicDBObject merge(MergePolicy mergePolicy, BasicDBObject existingNode, BasicDBObject newNode)
      throws GraphModelException {
    try {
      switch(mergePolicy) {
        case NONE:
          return existingNode;
        case ERR:
          EntityKeys existingKeyset = parseKeyset(marshaller, existingNode, FIELD_KEYS);
          throw new LogPlayerException("Attempt to merge nodes with one or more keyset items in common: "+existingKeyset);
        case APPEND_NEW__LEAVE_EXISTING:
          return doAppendNewLeaveExisting(existingNode, newNode);
        case APPEND_NEW__OVERWRITE_EXSITING:
          return doAppendNewOverwriteExisting(existingNode, newNode);
        case OVERWRITE_ALL:
          return doOverwriteAll(existingNode, newNode);
        default:
          throw new LogPlayerException("Unsupported merge policy type: "+mergePolicy);
      }
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform merge between nodes: "+existingNode+" and: "+newNode, e);
    }
  }



  /**
   * This method adds new properties to an existing node. Where there are
   * properties on the existing node with the same name as the ones specified
   * in the update command, we leave the existing values as they are.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private BasicDBObject doAppendNewLeaveExisting(BasicDBObject existingNode, BasicDBObject newNode)
      throws GraphModelException
  {
    try {
      // Deserialize the keyset fields of each object.
      EntityKeys existingKeyset = parseKeyset(marshaller, existingNode, FIELD_KEYS);
      EntityKeys newNodeKeyset = parseKeyset(marshaller, newNode, FIELD_KEYS);

      BasicDBObject merged = new BasicDBObject();
      merged.putAll(existingNode.toMap());

      for (String key : newNode.keySet()) {
        if (merged.containsField(key)) {
          continue; //Don't overwrite existing properties
        }
        //Set fields that exist in the request, but not in the existing object
        merged.put(key, newNode.get(key));
      }

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, newNodeKeyset);

      // Replace the 'keys' field with the merged keys sub-document.
      merged.put(FIELD_KEYS, marshaller.serialize(mergedKeys));
      return merged;
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform 'append new, leave existing' operation on existing node: "+existingNode, e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private BasicDBObject doAppendNewOverwriteExisting(BasicDBObject existingNode, BasicDBObject newNode)
      throws GraphModelException
  {
    try {
      // Deserialize the keyset fields of each object.
      EntityKeys existingKeyset = parseKeyset(marshaller, existingNode, FIELD_KEYS);
      EntityKeys newNodeKeyset = parseKeyset(marshaller, newNode, FIELD_KEYS);


      BasicDBObject merged = new BasicDBObject();
      merged.putAll(existingNode.toMap());

      for (String key : newNode.keySet()) {
        //Set all fields that exist in the request, regardless of whether they're present in the existing object
        merged.put(key, newNode.get(key));
      }

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, newNodeKeyset);

      // Replace the 'keys' field with the merged keys sub-document.
      merged.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      return merged;
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform 'append new, overwrite existing' operation on existing node: "+existingNode, e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private BasicDBObject doOverwriteAll(BasicDBObject existingNode, BasicDBObject newNode)
      throws GraphModelException
  {
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to merge keys from the existing object.
     */
    try {
      // Deserialize the keyset fields of each object.
      EntityKeys existingKeyset = parseKeyset(marshaller, existingNode, FIELD_KEYS);
      EntityKeys newNodeKeyset = parseKeyset(marshaller, newNode, FIELD_KEYS);

      BasicDBObject merged = new BasicDBObject();
      merged.putAll(newNode.toMap());

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, newNodeKeyset);

      // Replace the 'keys' field with the merged keys sub-document.
      merged.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      return merged;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'overwrite' operation on existing node: "+existingNode, e);
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
    merged.addNames(newKeyset.getNames());

    return merged;
  }

}
