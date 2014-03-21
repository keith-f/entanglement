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

package com.entanglementgraph.graph.updateview;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.couchdb.EdgeUpdateView;
import com.entanglementgraph.graph.couchdb.NodeUpdateView;

import java.util.List;
import java.util.Set;

/**
 * Provides methods for returning graph updates for nodes/edges.
 * It is up to the implementation to decide whether to return updates for <i>only</i> the specified keys,
 * or whether a recursive search is performed to find updates for all IDs a given entity is known by.
 *
 * @author Keith Flanagan
 */
public interface UpdateStream {

  /**
   * Given a set of keys, returns a list of timestamp-ordered graph updates.
   * Updates from all available graphs are returned.
   *
   * @param keys the keys for which updates should be returned
   * @return
   */
  public List<NodeUpdateView> findNodeUpdatesForKey(EntityKeys keys);

  /**
   * Given a set of keys, returns a list of timestamp-ordered graph updates.
   *
   * Only updates from named graphs in <code>graphNames</code> are included.
   *
   * @param keys the keys for which updates should be returned
   * @param graphNames a set of graph names to include updates from
   * @return
   */
  public List<NodeUpdateView> findNodeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames);



  public List<EdgeUpdateView> findEdgeUpdatesForKey(EntityKeys keys);
  public List<EdgeUpdateView> findEdgeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames);


}
