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
 * File created: 08-Nov-2012, 13:34:32
 */

package com.entanglementgraph.graph;

import com.entanglementgraph.graph.commands.EdgeUpdate;
import com.entanglementgraph.graph.commands.NodeUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ektorp.support.CouchDbDocument;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevisionItemContainer extends CouchDbDocument
{
//  @JsonProperty("_id")
//  private String id; // Mapped to _id for CouchDB
//  @JsonProperty("_rev")
//  private String revision;  // Mapped to _rev for CouchDB
//
//  @JsonProperty("_id")
//  private String _id;
//  @JsonProperty("_rev")
//  private String _rev;

  private long timestamp;
  private String timestampAsText;
  private String graphUid;
  private String patchUid;
  private int patchIdx;
  private boolean committed;
  private Date dateCommitted;
  
  private List<NodeUpdate> nodeUpdates;
  private List<EdgeUpdate> edgeUpdates;

  public RevisionItemContainer()
  {
    nodeUpdates = new LinkedList<>();
    edgeUpdates = new LinkedList<>();
  }

  @Override
  public String toString() {
    return "RevisionItemContainer{" +
        super.toString() +
//        "id='" + _id + '\'' +
//        ", revision='" + _rev + '\'' +
        ", timestamp=" + timestamp +
        ", graphUid='" + graphUid + '\'' +
        ", patchUid='" + patchUid + '\'' +
        ", patchIdx=" + patchIdx +
        ", committed=" + committed +
        ", dateCommitted=" + dateCommitted +
        ", nodeUpdates=" + nodeUpdates +
        ", edgeUpdates=" + edgeUpdates +
        '}';
  }

  public void addOperation(NodeUpdate op) {
    nodeUpdates.add(op);
  }

  public void addOperation(EdgeUpdate op) {
    edgeUpdates.add(op);
  }

  public String getGraphUid()
  {
    return graphUid;
  }

  public void setGraphUid(String graphUid)
  {
    this.graphUid = graphUid;
  }


  public int getPatchIdx() {
    return patchIdx;
  }

  public void setPatchIdx(int patchIdx) {
    this.patchIdx = patchIdx;
  }

  public List<NodeUpdate> getNodeUpdates() {
    return nodeUpdates;
  }

  public void setNodeUpdates(List<NodeUpdate> nodeUpdates) {
    this.nodeUpdates = nodeUpdates;
  }

  public List<EdgeUpdate> getEdgeUpdates() {
    return edgeUpdates;
  }

  public void setEdgeUpdates(List<EdgeUpdate> edgeUpdates) {
    this.edgeUpdates = edgeUpdates;
  }

  public long getTimestamp()
  {
    return timestamp;
  }

  public void setTimestamp(long timestamp)
  {
    this.timestamp = timestamp;
  }

  public String getPatchUid()
  {
    return patchUid;
  }

  public void setPatchUid(String patchUid)
  {
    this.patchUid = patchUid;
  }

  public boolean isCommitted() {
    return committed;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  public Date getDateCommitted() {
    return dateCommitted;
  }

  public void setDateCommitted(Date dateCommitted) {
    this.dateCommitted = dateCommitted;
  }

  public String getTimestampAsText() {
    return timestampAsText;
  }

  public void setTimestampAsText(String timestampAsText) {
    this.timestampAsText = timestampAsText;
  }
}
