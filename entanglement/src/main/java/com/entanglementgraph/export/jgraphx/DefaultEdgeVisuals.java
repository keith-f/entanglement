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

package com.entanglementgraph.export.jgraphx;

import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.mongodb.DBObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 26/06/2013
 * Time: 15:28
 * To change this template use File | Settings | File Templates.
 */
public class DefaultEdgeVisuals implements EdgeVisuals {
  @Override
  public String getTypeName() {
    return null;
  }

  @Override
  public Map<String, Object> getStyle() {
    Map<String, Object> style = new HashMap<>();
    return style;
  }

  @Override
  public String toBasicString(EntityKeys<Edge> keys, DBObject rawObject) {
    //Returns the edge type, plus first name or UID.
    StringBuilder sb = new StringBuilder();
    sb.append(keys.getType()).append("\n");
    if (!keys.getNames().isEmpty()) {
      sb.append(keys.getNames().iterator().next());
    } else {
      sb.append(keys.getUids().iterator().next());
    }

    return sb.toString();
  }
}
