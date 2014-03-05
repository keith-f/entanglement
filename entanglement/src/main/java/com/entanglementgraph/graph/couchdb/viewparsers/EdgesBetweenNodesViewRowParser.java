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

package com.entanglementgraph.graph.couchdb.viewparsers;

import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.ViewResult;

import java.util.Iterator;

/**
 * A parser (particularly for row keys) for query results from the 'nodes and edges' View.
 * This view contains multiple 'row types', determined by an integer to define sort order.
 * This parser key elements common to all row types, and, depending on the row type, extracts
 * additional row-specific key elements.
 *
 * @author Keith Flanagan
 */
public class EdgesBetweenNodesViewRowParser {

  private final ViewResult.Row row;
  private final Iterator<JsonNode> keyItr;

  // Node key items (common to all rows in this view)
  private final String fromNodeType;
  private final String fromNodeUidOrName;
  private final String fromNodeIdentifer;

  private final String toNodeType;
  private final String toNodeUidOrName;
  private final String toNodeIdentifer;

  private final String edgeTypeName;
  private final JsonNode edgeUids;
  private final JsonNode edgeNames;

  public EdgesBetweenNodesViewRowParser(ViewResult.Row row) {
    this.row = row;
    keyItr = row.getKeyAsNode().iterator();

    fromNodeType = keyItr.next().asText();       // Eg: "Gene".
    fromNodeUidOrName = keyItr.next().asText();  // Either 'U' or 'N'
    fromNodeIdentifer = keyItr.next().asText();  // The node UID or Name string

    toNodeType = keyItr.next().asText();         // Eg: "Gene".
    toNodeUidOrName = keyItr.next().asText();    // Either 'U' or 'N'
    toNodeIdentifer = keyItr.next().asText();    // The node UID or Name string

    edgeTypeName = keyItr.next().asText();       // The type name of the edge (e.g., 'has-part')
    edgeUids = keyItr.next();                    // (some) of the UIDs this edge is known by
    edgeNames = keyItr.next();                   // (some) of the names this edge is known by

  }

  public String getFromNodeType() {
    return fromNodeType;
  }

  public String getFromNodeUidOrName() {
    return fromNodeUidOrName;
  }

  public String getFromNodeIdentifer() {
    return fromNodeIdentifer;
  }

  public String getToNodeType() {
    return toNodeType;
  }

  public String getToNodeUidOrName() {
    return toNodeUidOrName;
  }

  public String getToNodeIdentifer() {
    return toNodeIdentifer;
  }

  public String getEdgeTypeName() {
    return edgeTypeName;
  }

  public JsonNode getEdgeUids() {
    return edgeUids;
  }

  public JsonNode getEdgeNames() {
    return edgeNames;
  }
}
