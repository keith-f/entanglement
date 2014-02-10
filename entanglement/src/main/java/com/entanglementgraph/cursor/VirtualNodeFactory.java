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
package com.entanglementgraph.cursor;

import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.graph.mongodb.NodeDAONodePerDocImpl;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import java.util.logging.Logger;

/**
 * Used to create transient DBObject instances for nodes that have no corresponding document in the database.
 * Such DBObject instances only have their 'keys' field set, plus another field flagging that they are non-existent.
 *
 * Virtual nodes are essential to allow graph cursors and exporters to function when edges link to non-existent data.
 *
 * User: keith
 * Date: 23/08/13; 11:47
 *
 * @author Keith Flanagan
 */
public class VirtualNodeFactory {
  private static final Logger logger = Logger.getLogger(VirtualNodeFactory.class.getName());

  public static BasicDBObject createVirtualNodeForLocation(
      DbObjectMarshaller marshaller, EntityKeys<? extends Node> location) throws DbObjectMarshallerException {
    BasicDBObject virtNode = new BasicDBObject();
    virtNode.put(NodeDAONodePerDocImpl.FIELD_KEYS, marshaller.serialize(location));
    virtNode.put(NodeDAONodePerDocImpl.FIELD_VIRTUAL, true); // Flag this 'node' as fake
    logger.info("Created 'virtual' document for missing node: "+virtNode);
    return virtNode;
  }

}
