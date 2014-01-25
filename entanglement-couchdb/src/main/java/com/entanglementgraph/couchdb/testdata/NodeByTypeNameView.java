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

package com.entanglementgraph.couchdb.testdata;

import org.ektorp.CouchDbConnector;
import org.ektorp.StreamingViewResult;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Keith Flanagan
 */
public class NodeByTypeNameView { //extends CouchDbRepositorySupport<NodeWithContent> {
//  protected NodeByTypeNameView(Class<NodeWithContent> type, CouchDbConnector db) {
//    super(type, db);
//  }

  private final CouchDbConnector db;

  public NodeByTypeNameView(CouchDbConnector db)  {
    this.db = db;
  }

//  @View( name = "avg_sofa_size", map = "function(doc) {...}", reduce = "function(doc) {...}")
//  public int getAverageSofaSize() {
//    ViewResult r = db.queryView(createQuery("avg_sofa_size"));
//    return r.getRows().get(0).getValueAsInt();
//  }

//  @View( name = "nodes_by_name", map = "classpath:nodesByName.js")
//  //, reduce = "function(doc) {...}")
//  public List<NodeWithContent> getAllNodesByName() {
//    //ViewResult vr = db.queryView(createQuery("nodes_by_name"));
//    StreamingViewResult vr = db.queryForStreamingView(createQuery("nodes_by_name"));
//    List<NodeWithContent> nodes = new LinkedList<>();
//    for (ViewResult.Row r : vr) {
////      db.getO
////      nodes.add(r.getValue());
//    }
//    return nodes;
//  }

//  @View( name = "nodes_by_name", map = "classpath:nodesByName.js")
  public List<NodeWithContent> getAllNodes() {
    ViewQuery query = new ViewQuery()
        .designDocId("_design/nodes_by_name")
        .viewName("all");
//        .key("red");

    List<NodeWithContent> nodes = db.queryView(query, NodeWithContent.class);
    return nodes;
  }

//  public <C extends NodeContent> List<NodeWithContent<C>> getAllNodesOfType(Class c) {
  public List<NodeWithContent> getAllNodesOfType(Class c) {
    ViewQuery query = new ViewQuery()
        .designDocId("_design/Sofa")
        .viewName("by_color")
        .key("red");

    List<NodeWithContent> nodes = db.queryView(query, NodeWithContent.class);
    return nodes;
  }
}
