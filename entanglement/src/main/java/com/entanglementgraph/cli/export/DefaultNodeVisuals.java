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

package com.entanglementgraph.cli.export;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.mongodb.DBObject;
import com.mxgraph.util.mxConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 26/06/2013
 * Time: 14:50
 * To change this template use File | Settings | File Templates.
 */
public class DefaultNodeVisuals implements NodeVisuals {
  @Override
  public String getTypeName() {
    return null;
  }

  @Override
  public Map<String, Object> getStyle() {
    Map<String, Object> style = new HashMap<>();
    style.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
//    style.put(mxConstants.STYLE_DASHED, true);
//    style.put(mxConstants.STYLE_OPACITY, 50);
//    style.put(mxConstants.STYLE_GLASS, 50);
//    style.put(mxConstants.STYLE_FONTCOLOR, "#774400");

    return style;
  }

  @Override
  public double getDefaultWidth() {
    return 20;
  }

  @Override
  public double getDefaultHeight() {
    return 20;
  }

  @Override
  public String toBasicString(EntityKeys<Node> keys, DBObject rawObject) {
    return String.format("%s\n%s", keys.getType(), rawObject.toString());
  }
}
