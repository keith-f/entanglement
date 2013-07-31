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

package com.entanglementgraph.visualisation.jgraphx;

import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.visualisation.jgraphx.renderers.CustomCellRenderer;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;

import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class NodeByTypeValueRule implements ValueRule {
  private static final Logger logger = Logger.getLogger(NodeByTypeValueRule.class.getName());

  private final DbObjectMarshaller marshaller;
  private final String nodeType;
  private final Class<? extends CustomCellRenderer> rendererType;

  public NodeByTypeValueRule(DbObjectMarshaller marshaller, String nodeType, Class<? extends CustomCellRenderer> rendererType) {
    this.marshaller = marshaller;
    this.nodeType = nodeType;
    this.rendererType = rendererType;
  }

  @Override
  public boolean appliesToDataValue(Object value) {
    if (!(value instanceof DBObject)) {
      return false;
    }

    try {
      DBObject valObj = (DBObject) value;
      Node valNode = marshaller.deserialize(valObj, Node.class);
      return valNode.getKeys().getType().equals(nodeType);
    } catch (Exception e) {
      //This probably wasn't a Node...
      return false;
    }
  }

  @Override
  public Class<? extends CustomCellRenderer> getRenderer(Object value) {
    logger.info(String.format("Evaluating: %s", value));
    if (!(value instanceof DBObject)) {
      return null;
    }

    try {
      DBObject valObj = (DBObject) value;
      Node valNode = marshaller.deserialize(valObj, Node.class);
      logger.info(String.format("Deserialised: %s to: %s", value, valNode));
      return valNode.getKeys().getType().equals(nodeType)
          ? rendererType
          : null;
    } catch (Exception e) {
      //This probably wasn't a Node...
      return null;
    }
  }
}
