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
package com.entanglementgraph.visualisation.jung;

import com.entanglementgraph.util.GraphConnection;
import com.mongodb.DBObject;
import edu.uci.ics.jung.graph.Graph;

/**
 * Specifies methods for creating in-memory Jung <code>Graph</code> data structures from Entanglement graphs.
 *
 * User: keith
 * Date: 15/08/13; 16:39
 *
 * @author Keith Flanagan
 */
public interface EntanglementToJungConverter {

  /**
   * Creates a new Jung <code>Graph</code> from an Entanglement graph.
   *
   * @param sourceGraph
   * @return
   */
  public Graph<DBObject, DBObject> entanglementToJung(GraphConnection sourceGraph);

  /**
   * Creates a new Jung <code>Graph</code> from an Entanglement graph and an existing Jung graph. Existing nodes are
   * not overwritten or changed in any way.
   *
   * @param sourceGraph
   * @param existing
   * @return
   */
  public Graph<DBObject, DBObject> entanglementToJung(GraphConnection sourceGraph, Graph<DBObject, DBObject> existing);
}
