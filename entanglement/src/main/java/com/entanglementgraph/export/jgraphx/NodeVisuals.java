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

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.mongodb.DBObject;

import java.util.Map;

/**
 * Provides node-specific rendering hints for JGraphX exports.
 * For each of your datatypes that require datatype-specific layout, size, or style information, provide an
 * implementation of this interface. Then, pass instances of those implementations to MongoToJGraphExporter prior
 * to performing the export. When adding a particular JGraphX node, the exporter will perform a lookup for node-specific
 * visual information using the <code>type</code> field of the <code>EntityKeys</code> value.
 */
public interface NodeVisuals {
  /**
   * The type name that this NodeVisuals instance applies to. This name should match the <code>type</code> field of
   * the <code>EntityKeys</code> value of your node.
   * @return
   */
  public String getTypeName();

  /**
   * Provides a JGraphX stylesheet. Refer to the JGraphX documentation for valid values.
   * @return
   */
  public Map<String, Object> getStyle();

  /**
   * The default width of the node.
   * @return
   */
  public double getDefaultWidth();

  /**
   * The default height of the node.
   * @return
   */
  public double getDefaultHeight();

  /**
   * Takes a Node and turns it into a basic text string for displaying within a JGraphX node.
   * @param node
   * @return
   */
  public String toBasicString(EntityKeys<Node> keys, DBObject rawObject);
}
