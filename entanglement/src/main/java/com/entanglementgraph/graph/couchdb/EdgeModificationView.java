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
 * File created: 28-Aug-2012, 15:44:46
 */

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.commands.EdgeModification;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An extension of NodeModification for use with the appropriate CouchDB View.
 * The View contains additional fields from the 'root' document (of type RevisionItemContainer) that
 * are useful when merging modifications
 * 
 * @author Keith Flanagan
 */
@JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
public class EdgeModificationView<C extends Content, F extends Content, T extends Content> extends EdgeModification<C, F, T>
{
  private long timestamp;
  private String graphUid;
  private String patchUid;

  @Override
  public String toString() {
    return "EdgeModificationView{" +
        "timestamp=" + timestamp +
        ", graphUid='" + graphUid + '\'' +
        ", patchUid='" + patchUid + '\'' +
        "} " + super.toString();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getGraphUid() {
    return graphUid;
  }

  public void setGraphUid(String graphUid) {
    this.graphUid = graphUid;
  }

  public String getPatchUid() {
    return patchUid;
  }

  public void setPatchUid(String patchUid) {
    this.patchUid = patchUid;
  }
}
