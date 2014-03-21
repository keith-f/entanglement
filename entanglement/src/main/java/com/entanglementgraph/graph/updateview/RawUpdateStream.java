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

package com.entanglementgraph.graph.updateview;

import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.couchdb.EdgeUpdateView;
import com.entanglementgraph.graph.couchdb.NodeUpdateView;

import java.util.List;
import java.util.Set;

/**
 * An implementation of <code>UpdateStream</code> that returns a set of updates by key.
 *
 * For keys of type: <code>{type, [UID, ...] }</code>, returns a stream of updates <i>for the specified keys only</i>.
 * If a partial keyset is specified (i.e., the user requests fewer keys than the total set that the entity is
 * known by), then the extra key names are returned, but no additional updates are returned.
 *
 * No integration is performed at this stage. The methods defined here only return a set of raw updates from
 * the database. Updates may be filtered by graph name(s) if necessary.
 *
 * @author Keith Flanagan
 */
public class RawUpdateStream implements UpdateStream {
  @Override
  public List<NodeUpdateView> findNodeUpdatesForKey(EntityKeys keys) {
    return null;
  }

  @Override
  public List<NodeUpdateView> findNodeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames) {
    return null;
  }

  @Override
  public List<EdgeUpdateView> findEdgeUpdatesForKey(EntityKeys keys) {
    return null;
  }

  @Override
  public List<EdgeUpdateView> findEdgeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames) {
    return null;
  }
}
