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


import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.graph.couchdb.NodeMerger;

import java.util.logging.Logger;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.entanglementgraph.graph.commands.NodeUpdate;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

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

  // The deserialized key field from the reqSerializedNode. This identifies the object to be created/updated.
  private EntityKeys reqKeyset;
  
  @Override
  public String getSupportedLogItemType()
  {
    return NodeUpdate.class.getSimpleName();
  }

  @Override
  public void playItem(GraphOperation op)
      throws LogPlayerException
  {
    try {
      NodeUpdate command = (NodeUpdate) op;
      Node node = command.getNode();

      reqKeyset = node.getKeys();

      //The reference field should contain at least one identification key
      validateKeyset(reqKeyset);

      createOrModify(command);

    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command: "+op.toString(), e);
    }
  }


  /*
   * To create or update an entity, then there must be at least one UID and/or at least one name.
   * If one or more names are specified, then a entity type must also be present.
   */
  private void validateKeyset(EntityKeys keyset) throws LogPlayerException {
    if (keyset.getNames().isEmpty() && keyset.getUids().isEmpty()) {
      throw new LogPlayerException("You must specify at least one entity key (either a UID, or a type/name"
          + "Keyset was: "+keyset);
    }
    if (!keyset.getNames().isEmpty() && keyset.getType() == null) {
      throw new LogPlayerException("You specified one or more entity names, but did not specify a type. "
          + "Keyset was: "+keyset);
    }
  }

  private void createOrModify(NodeUpdate command)
          throws LogPlayerException
  {
    try {
      // Does the entity exist by any key specified in the reference?
      boolean exists = nodeDao.existsByKey(command.getNode().getKeys());

      // No - create a new document
      if (!exists) {
        // Create a new node and then exit
//        logger.info("NodeModification refers to a new node. Query keyset was: "+keyset);
        createNewNode(command);
      }
      // Yes  - update existing document
      else {
//        logger.info("NodeModification matched an existing node. Query keyset was: "+keyset+". Entire document: "+reqSerializedNode);
        updateExistingNode(command);
      }
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to play back command. Command was: "+command.toString(), e);
    }
  }

  /**
   * Called when a node is found to not exist already - we need to create it.
   */
  private void createNewNode(NodeUpdate command)
      throws GraphModelException, LogPlayerException, DbObjectMarshallerException {
    nodeDao.store(marshaller.serialize(command.getNode()));
  }

  private void updateExistingNode(NodeUpdate command)
      throws LogPlayerException {
    try {
      // Edit existing node - need to perform a merge based on
      Node existing = nodeDao.getByKey(reqKeyset);
//      logger.info("NodeModification matched an existing node. Query document: "+reqSerializedNode+".\nExisting (matching) database document was : "+existing);

      NodeMerger merger = new NodeMerger();
      merger.merge(command.getMergePol(), existing, command.getNode());

      nodeDao.update(marshaller.serialize(existing));

    } catch (Exception e) {
      throw new LogPlayerException("Failed to perform update on node with keyset: "+reqKeyset, e);
    }
  }

}
