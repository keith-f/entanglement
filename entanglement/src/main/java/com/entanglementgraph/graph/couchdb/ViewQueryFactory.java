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

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;

/**
 * @author Keith Flanagan
 */
public class ViewQueryFactory {

  public static ViewQuery createNodesAndEdgesQuery(CouchDbConnector db) {
    ViewQuery query = new ViewQuery()
        .dbPath(db.path())
        .designDocId(NodeDAOCouchDbImpl.DESIGN_DOC_ID)
        .viewName("nodes_and_edges");
    return query;
  }

  public static ViewQuery createEdgesQuery(CouchDbConnector db) {
    ViewQuery query = new ViewQuery()
        .dbPath(db.path())
        .designDocId(EdgeDAOCouchDbImpl.DESIGN_DOC_ID)
        .viewName("edges");
    return query;
  }

  public static ViewQuery createEdgesBetweenNodesQuery(CouchDbConnector db) {
    ViewQuery query = new ViewQuery()
        .dbPath(db.path())
        .designDocId(EdgeDAOCouchDbImpl.DESIGN_DOC_ID)
        .viewName("edges_between_nodes");
    return query;
  }

//  public static ViewQuery createReducedNodesAndEdgesQuery(CouchDbConnector db) {
//    ViewQuery query = new ViewQuery()
//        .dbPath(db.path())
//        .designDocId(NodeDAOCouchDbImpl.DESIGN_DOC_ID)
//        .viewName("nodes_and_edges")
//        .reduce(true)
//        .group(true)
//        .groupLevel(4);
//    return query;
//  }
}
