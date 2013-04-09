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
 * File created: 15-Nov-2012, 15:30:12
 */

package com.entanglementgraph.graph;

/**
 * @author Keith Flanagan
 */
public interface NodeDAO
    extends GraphEntityDAO {
  /**
   * Attempts to add a Node instance to an existing graph.
   * @param node
   * @throws GraphModelException if a node with the same unique ID already
   * exists in the data structure.
   * deprecated : as this method is deprecated *AND* commented out, IntelliJ seems to think it is the
   * *class* and not the method that is deprecated, causing problems for the inspection routines. Therefore
   * have removed the "@" symbol from in front of "deprecated".
   */
//  public void store(Node node)
//      throws GraphModelException;

}
