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

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.graph.Node;
import com.entanglementgraph.graph.RevisionItemContainer;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;

import java.util.List;

/**
 * A DAO for storing, updating, and querying revision items.
 *
 * By extending CouchDBRepositorySupport, we get the following operations for free:
 * <pre>
 *   public void add(Sofa entity);
 *   public void update(Sofa entity);
 *   public void remove(Sofa entity);
 *   public Sofa get(String id);
 *   public Sofa get(String id, String rev);
 *   public List<T> getAll();
 *   public boolean contains(String docId);
 * </pre>
 *
 * @author Keith Flanagan
 */
public class RevisionsCouchDbDAO
  extends CouchDbRepositorySupport<RevisionItemContainer> {
  public RevisionsCouchDbDAO(CouchDbConnector db) {
    super(RevisionItemContainer.class, db);
    initStandardDesignDocument();
  }


//  @View( name = "nodes_by_name", map = "classpath:nodesByName.js")
  public List<Node> getAllNodes() {
    ViewQuery query = new ViewQuery()
        .designDocId("_design/nodes_by_name")
        .viewName("_all");
//        .key("red");

    List<Node> nodes = db.queryView(query, Node.class);
    return nodes;
  }

  @View( name = "all_nodes_by_name", map = "classpath:nodesByName.js")
  public List<Node> getAllNodes2() {
//    ViewQuery query = new ViewQuery()
//        .designDocId("_design/nodes_by_name")
//        .viewName("_all");
////        .key("red");

    ViewQuery query = createQuery("all_nodes_by_name");

    List<Node> nodes = db.queryView(query, Node.class);
    return nodes;
  }


}
