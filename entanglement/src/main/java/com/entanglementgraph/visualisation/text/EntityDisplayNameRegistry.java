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
package com.entanglementgraph.visualisation.text;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.GraphEntity;
import com.entanglementgraph.graph.data.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * User: keith
 * Date: 14/08/13; 15:14
 *
 * @author Keith Flanagan
 */
public class EntityDisplayNameRegistry {
  private final Map<Class<? extends GraphEntity>, GraphEntityDisplayNameFactory> typeToFactory;
  private final Map<String, GraphEntityDisplayNameFactory> typeNameToFactory;
  private GraphEntityDisplayNameFactory defaultNodeFactory;
  private GraphEntityDisplayNameFactory defaultEdgeFactory;

  public EntityDisplayNameRegistry() {
    this.typeToFactory = new HashMap<>();
    this.typeNameToFactory = new HashMap<>();
    defaultNodeFactory = new DefaultDisplayNameFactory();
    defaultEdgeFactory = new TypeOnlyNameFactory();
  }

  /**
   * Registers both a Java bean type, plus an Entanglement type name with a particular display name factory.
   *
   * @param entityBeanType
   * @param entityTypeName
   * @param displayNameFactory
   */
  public void addMapping(Class<? extends GraphEntity> entityBeanType, String entityTypeName, GraphEntityDisplayNameFactory displayNameFactory) {
    typeToFactory.put(entityBeanType, displayNameFactory);
    typeNameToFactory.put(entityTypeName, displayNameFactory);
  }

  /**
   * Returns a name factory instance for a given bean type.
   *
   * @param type
   * @return
   */
  public GraphEntityDisplayNameFactory getNameFactoryForBeanType(Class<? extends GraphEntity> type) {
    GraphEntityDisplayNameFactory factory = typeToFactory.get(type);
    if (factory != null) {
      return factory;
    }
    if (type.isAssignableFrom(Node.class)) {
      return defaultNodeFactory;
    } else {
      return defaultEdgeFactory;
    }
  }

  /**
   * Returns a name factory instance for a given Entanglement type name.
   * @param typeName
   * @return
   */
  public GraphEntityDisplayNameFactory getNameFactoryForTypeName(String typeName) {
    GraphEntityDisplayNameFactory factory = typeNameToFactory.get(typeName);
    if (factory != null) {
      return factory;
    }
    return defaultNodeFactory;
  }

  /**
   * Convenience function that finds a name factory for the specified entity, and returns a display name.
   *
   * @param keys
   * @return
   */
  public String createNameForEntity(EntityKeys keys) {
    return getNameFactoryForTypeName(keys.getType()).getDisplayName(keys);
  }
}
