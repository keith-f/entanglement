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

import java.util.Iterator;

/**
 * A display name factory implementation that causes the entity type to be displayed only.
 *
 * User: keith
 * Date: 14/08/13; 15:01
 *
 * @author Keith Flanagan
 */
public class TypeOnlyNameFactory<T extends GraphEntity> implements GraphEntityDisplayNameFactory<T> {
  @Override
  public String getDisplayName(EntityKeys<T> entityKeys) {
    return entityKeys.getType();
  }

  @Override
  public String getDisplayName(T entityInstance) {
    return getDisplayName(entityInstance.getKeys());
  }
}
