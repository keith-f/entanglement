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

import com.entanglementgraph.couchdb.revlog.data.RevisionItemContainer;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.CouchDbRepositorySupport;

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
public class NodeCouchDbDAO
  extends CouchDbRepositorySupport<NewNode> {
  public NodeCouchDbDAO(CouchDbConnector db) {
    super(NewNode.class, db);
  }

//  public RevisionItemContainer

//  public List<RevisionItemContainer> findByPatchUid(String patchUid) {
//    // View name, followed by key name
//    return queryView("by_transaction_uid", patchUid);
//  }
//
//
//  public List<RevisionItemContainer> findByPatchUid(String patchUid) {
//    // View name, followed by key name
//    return queryView("by_transaction_uid", patchUid);
//  }

}
