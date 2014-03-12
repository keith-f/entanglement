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
public class NodesAndEdgesViewRowParser {

  public static enum RowType {
    /**
     * Indicates that this row contains a description of a node.
     */
    NODE (0),
    /**
     * Indicates that this row contains a description of an edge starting at the specified node.
     */
    EDGE_FROM_NODE (1),
    /**
     * Indicates that this row contains a description of an edge ending at the specified node.
     */
    EDGE_TO_NODE (2);

    private final int dbTypeIdx;

    RowType(int dbTypeIdx) {
      this.dbTypeIdx = dbTypeIdx;
    }

    public int getDbTypeIdx() {
      return dbTypeIdx;
    }
  }

  private final ViewResult.Row row;
  private final Iterator<JsonNode> keyItr;

  // Node key items (common to all rows in this view)
  private final String nodeTypeName;
  private final String nodeIdentifer;
  private final int rowType;
  private final JsonNode otherNodeUids;

  // When rowType == 1, this indicates a 'from node' edge entry. Additional key fields exist.
  private String edgeTypeName;
  private JsonNode otherEdgeUids;

  public NodesAndEdgesViewRowParser (ViewResult.Row row) {
    this.row = row;
    keyItr = row.getKeyAsNode().iterator();

    nodeTypeName = keyItr.next().asText();   // Eg: "Gene".
    nodeIdentifer = keyItr.next().asText();  // The node UID or Name string
    rowType =  keyItr.next().asInt();        // 0=node; 1=edgeFrom; ...
    otherNodeUids = keyItr.next();           // (some) of the other UIDs this node is known by

     if (rowType == 1 || rowType == 2) {
      // additional key fields: edge.keys.type, allEdgeUids, allEdgeNames
      edgeTypeName = keyItr.next().asText();   // The type name of the edge (e.g., 'has-part')
      otherEdgeUids = keyItr.next();           // (some) of the other UIDs this edge is known by
    }
  }


  public String getNodeTypeName() {
    return nodeTypeName;
  }

  public String getNodeIdentifer() {
    return nodeIdentifer;
  }

  public int getRowType() {
    return rowType;
  }

  public JsonNode getOtherNodeUids() {
    return otherNodeUids;
  }

  public String getEdgeTypeName() {
    return edgeTypeName;
  }

  public JsonNode getOtherEdgeUids() {
    return otherEdgeUids;
  }
}
