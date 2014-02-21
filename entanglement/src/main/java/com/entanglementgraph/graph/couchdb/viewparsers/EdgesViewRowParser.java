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
 * A parser (particularly for row keys) for query results from the 'edges' View.
 *
 * @author Keith Flanagan
 */
public class EdgesViewRowParser {

  private final ViewResult.Row row;
  private final Iterator<JsonNode> keyItr;


  private final String edgeTypeName;
  private final String uidOrName;
  private final String edgeIdentifer;
  private final int rowType;
  private final JsonNode otherEdgeUids;
  private final JsonNode otherEdgeNames;


  public EdgesViewRowParser(ViewResult.Row row) {
    this.row = row;
    keyItr = row.getKeyAsNode().iterator();

    edgeTypeName = keyItr.next().asText();   // Eg: "has-part".
    uidOrName = keyItr.next().asText();      // Either 'U' or 'N'
    edgeIdentifer = keyItr.next().asText();  // The edge UID or Name string
    rowType =  keyItr.next().asInt();        // currently only '0'
    otherEdgeUids = keyItr.next();           // (some) of the other UIDs this edge is known by
    otherEdgeNames = keyItr.next();          // (some) of the other names this edge is known by

  }

  public String getEdgeTypeName() {
    return edgeTypeName;
  }

  public String getUidOrName() {
    return uidOrName;
  }

  public String getEdgeIdentifer() {
    return edgeIdentifer;
  }

  public int getRowType() {
    return rowType;
  }

  public JsonNode getOtherEdgeUids() {
    return otherEdgeUids;
  }

  public JsonNode getOtherEdgeNames() {
    return otherEdgeNames;
  }
}
