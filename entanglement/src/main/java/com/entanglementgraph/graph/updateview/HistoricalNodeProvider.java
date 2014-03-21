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
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.graph.couchdb.EdgeUpdateView;
import com.entanglementgraph.graph.couchdb.NodeUpdateView;

import java.util.List;
import java.util.Set;

/**
 * Takes a set of timestamp-ordered node updates and provides a merged domain object.
 * Either the complete set or a subset of the node updates may be used to construct the merged object, allowing
 * the caller to either see the 'latest' domain object, or a version of the object at some previous point in time.
 *
 * @author Keith Flanagan
 */
public interface HistoricalNodeProvider<C extends Content> {

  /**
   * Returns the merged node produced from all available updates.
   *
   * @return
   */
  public Node<C> latest();

  /**
   * Returns a timestamp-ordered list of nodes, composed of node updates merged up to that timestamp.
   *
   * @return
   */
  public List<Snapshot<Node<C>>> entireHistory();

}
