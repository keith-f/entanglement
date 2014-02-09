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

package com.entanglementgraph.graph.mongodb.player.spi;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.mongodb.jiti.EdgeMerger;
import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.entanglementgraph.graph.commands.EdgeModification;
import com.entanglementgraph.graph.RevisionItem;
import com.entanglementgraph.graph.mongodb.MongoUtils;
import com.mongodb.BasicDBObject;

import java.util.logging.Logger;

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

  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    try {
      this.item = item;
      command = (EdgeModification) item.getOp();
      reqSerializedEdge = command.getEdge();

      reqKeyset = MongoUtils.parseKeyset(marshaller, reqSerializedEdge);

      //The reference field should contain at least one identification key
      validateKeyset(reqKeyset);

      createOrModify(reqKeyset);

    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command: "+item.toString(), e);
    }
  }

  /*
   * To create or update an entity, then there must be at least one UID and/or at least one name.
   * If one or more names are specified, then a entity type must also be present.
   */
  private static void validateKeyset(EntityKeys keyset) throws LogPlayerException {
    if (keyset.getNames().isEmpty() && keyset.getUids().isEmpty()) {
      throw new LogPlayerException("You must specify at least one entity key (either a UID, or a type/name. " +
          "Keyset was: "+keyset);
    }
    if (!keyset.getNames().isEmpty() && keyset.getType() == null) {
      throw new LogPlayerException("You specified one or more entity names, but did not specify a type. " +
          "Keyset was: "+keyset);
    }
  }


  private void createOrModify(EntityKeys keyset)
      throws LogPlayerException
  {
//    logger.info("Attempting playback of entity: "+keyset);
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
        updateExistingEdge(command, reqSerializedEdge);
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
    try {
      edgeDao.store(reqSerializedEdge);
    } catch(Exception e) {
      throw new LogPlayerException("Failed to create an edge. Command was: "+command.toString(), e);
    }
  }


  private void updateExistingEdge(EdgeModification command, BasicDBObject reqSerializedNode)
      throws LogPlayerException {
    try {
      // Edit existing node - need to perform a merge based on
      BasicDBObject existing = edgeDao.getByKey(reqKeyset);
//      logger.info("NodeModification matched an existing node. Query document: "+reqSerializedNode+".\nExisting (matching) database document was : "+existing);

      EdgeMerger merger = new EdgeMerger(marshaller);
      BasicDBObject updated = merger.merge(command.getMergePol(), existing, reqSerializedNode);
      edgeDao.update(updated);

    } catch (Exception e) {
      throw new LogPlayerException("Failed to perform update on node with keyset: "+reqKeyset, e);
    }
  }

}