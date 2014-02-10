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
package com.entanglementgraph.graph.mongodb.jiti;

import static com.entanglementgraph.graph.mongodb.MongoUtils.parseKeyset;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.mongodb.MongoUtils;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import static com.entanglementgraph.graph.mongodb.NodeDAONodePerDocImpl.FIELD_KEYS;
import static com.entanglementgraph.graph.mongodb.EdgeDAOSeparateDocImpl.*;

/**
 * A convenience class for merging two BasicDBObject instances that represent graph edges. This functionality is
 * required in several places so has been extracted from its original location within EdgeModificationPlayer.
 *
 * Edges may be merged according to a <code>MergePolicy</code>, which determines which data fields are ignored,
 * overwritten, or combined.
 *
 * Elements of the object's identifier (UIDs and names of EntityKeys) are always merged. All EntityKeys are merged,
 * including keysets for the Edge itself, as well as the source and destination node keysets.
 *
 * User: keith
 * Date: 10/07/13; 11:22
 *
 * @author Keith Flanagan
 */
public class EdgeMerger {

  private final DbObjectMarshaller marshaller;
  public EdgeMerger(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }

  public BasicDBObject merge(MergePolicy mergePolicy, BasicDBObject existingEdge, BasicDBObject newEdge)
      throws GraphModelException {
    try {
      switch(mergePolicy) {
        case NONE:
//          logger.log(Level.INFO, "Ignoring existing edge: {0}", reqKeyset);
          return existingEdge;
        case ERR:
          EntityKeys existingKeyset = MongoUtils.parseKeyset(marshaller, existingEdge.getString(FIELD_KEYS));
          throw new LogPlayerException("An edge with one or more items in the following keyset already exists: "+existingKeyset);
        case APPEND_NEW__LEAVE_EXISTING:
          return doAppendNewLeaveExisting(existingEdge, newEdge);
        case APPEND_NEW__OVERWRITE_EXSITING:
          return doAppendNewOverwriteExisting(existingEdge, newEdge);
        case OVERWRITE_ALL:
          return doOverwriteAll(existingEdge, newEdge);
        default:
          throw new LogPlayerException("Unsupported merge policy type: "+mergePolicy);
      }
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform merge between edges: "+existingEdge+" and: "+newEdge, e);
    }
  }


  /**
   * This method adds new properties to an existing edge. Where there are
   * properties on the existing edge with the same name as the ones specified
   * in the update command, we leave the existing values as they are.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private BasicDBObject doAppendNewLeaveExisting(BasicDBObject existingEdge, BasicDBObject newEdge)
      throws GraphModelException
  {
    try {
      // Deserialize the keyset fields of each object.
      EntityKeys existingKeyset = parseKeyset(marshaller, existingEdge, FIELD_KEYS);
      EntityKeys newEdgeKeyset = parseKeyset(marshaller, newEdge, FIELD_KEYS);

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(existingEdge.toMap());

      for (String key : newEdge.keySet()) {
        if (updated.containsField(key)) {
          continue; //Don't overwrite existing properties
        }
        //Set fields that exist in the request, but not in the existing object
        updated.put(key, newEdge.get(key));
      }

      // Allow new keys to be added to identify the edge, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, newEdgeKeyset);
      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      // Also allow new keys to be added for the from/to node fields
      EntityKeys fromExistingKeyset = parseKeyset(marshaller, existingEdge, FIELD_FROM_KEYS);
      EntityKeys fromReqKeyset = parseKeyset(marshaller, newEdge, FIELD_FROM_KEYS);
      EntityKeys fromMergedKeys = _mergeKeys(fromExistingKeyset, fromReqKeyset);
      updated.put(FIELD_FROM_KEYS, marshaller.serialize(fromMergedKeys));

      // Repeat for the 'to' node:
      EntityKeys toExistingKeyset = parseKeyset(marshaller, existingEdge, FIELD_TO_KEYS);
      EntityKeys toReqKeyset = parseKeyset(marshaller, newEdge, FIELD_TO_KEYS);
      EntityKeys toMergedKeys = _mergeKeys(toExistingKeyset, toReqKeyset);
      updated.put(FIELD_TO_KEYS, marshaller.serialize(toMergedKeys));

      return updated;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'append new, leave existing' operation on existing node: "+existingEdge, e);
    }
  }

  /**
   * This method adds new properties to an existing edge and overwrites the
   * values of existing properties.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private BasicDBObject doAppendNewOverwriteExisting(BasicDBObject existingEdge, BasicDBObject newEdge)
      throws GraphModelException
  {
    try {
      // Deserialize the keyset fields of each object.
      EntityKeys existingKeyset = parseKeyset(marshaller, existingEdge, FIELD_KEYS);
      EntityKeys newEdgeKeyset = parseKeyset(marshaller, newEdge, FIELD_KEYS);

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(existingEdge.toMap());

      for (String key : newEdge.keySet()) {
        //Set all fields that exist in the request, regardless of whether they're present in the existing object
        updated.put(key, newEdge.get(key));
      }

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, newEdgeKeyset);
      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      // Also allow new keys to be added for the from/to node fields
      EntityKeys fromExistingKeyset = parseKeyset(marshaller, existingEdge, FIELD_FROM_KEYS);
      EntityKeys fromReqKeyset = parseKeyset(marshaller, newEdge, FIELD_FROM_KEYS);
      EntityKeys fromMergedKeys = _mergeKeys(fromExistingKeyset, fromReqKeyset);
      updated.put(FIELD_FROM_KEYS, marshaller.serialize(fromMergedKeys));

      // Repeat for the 'to' node:
      EntityKeys toExistingKeyset = parseKeyset(marshaller, existingEdge, FIELD_TO_KEYS);
      EntityKeys toReqKeyset = parseKeyset(marshaller, newEdge, FIELD_TO_KEYS);
      EntityKeys toMergedKeys = _mergeKeys(toExistingKeyset, toReqKeyset);
      updated.put(FIELD_TO_KEYS, marshaller.serialize(toMergedKeys));

      return updated;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'append new, overwrite existing' operation on existing node: "+existingEdge, e);
    }
  }

  /**
   * This method adds new properties to an existing edge and overwrites the
   * values of existing properties.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private BasicDBObject doOverwriteAll(BasicDBObject existingEdge, BasicDBObject newEdge)
      throws GraphModelException
  {
    /*
     * In this case, we can simply use the new DBObject from the command.
     * We just need to merge keys from the existing object.
     */
    try {
      // Deserialize the keyset fields of each object.
      EntityKeys existingKeyset = parseKeyset(marshaller, existingEdge, FIELD_KEYS);
      EntityKeys newEdgeKeyset = parseKeyset(marshaller, newEdge, FIELD_KEYS);

      BasicDBObject updated = new BasicDBObject();
      updated.putAll(newEdge.toMap());

      // Allow new keys to be added, but disallow editing type of the entity
      EntityKeys mergedKeys = _mergeKeys(existingKeyset, newEdgeKeyset);
      // Replace the 'keys' field with the merged keys sub-document.
      updated.put(FIELD_KEYS, marshaller.serialize(mergedKeys));

      // Also allow new keys to be added for the from/to node fields
      EntityKeys fromExistingKeyset = parseKeyset(marshaller, existingEdge, FIELD_FROM_KEYS);
      EntityKeys fromReqKeyset = parseKeyset(marshaller, newEdge, FIELD_FROM_KEYS);
      EntityKeys fromMergedKeys = _mergeKeys(fromExistingKeyset, fromReqKeyset);
      updated.put(FIELD_FROM_KEYS, marshaller.serialize(fromMergedKeys));

      // Repeat for the 'to' node:
      EntityKeys toExistingKeyset = parseKeyset(marshaller, existingEdge, FIELD_TO_KEYS);
      EntityKeys toReqKeyset = parseKeyset(marshaller, newEdge, FIELD_TO_KEYS);
      EntityKeys toMergedKeys = _mergeKeys(toExistingKeyset, toReqKeyset);
      updated.put(FIELD_TO_KEYS, marshaller.serialize(toMergedKeys));

      return updated;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'overwrite' operation on existing node: "+existingEdge, e);
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
