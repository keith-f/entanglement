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

package com.entanglementgraph.couchdb;

import com.entanglementgraph.couchdb.revlog.commands.NodeModification;
import com.entanglementgraph.graph.GraphModelException;
import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.DesignDocumentFactory;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.*;

/**
 * @author Keith Flanagan
 */
@Views({
    @View(name = "nameToNames", map = "classpath:nameToNamesMap.js"),
    @View(name = "uidToUids", map = "classpath:uidToUidsMap.js"),
    @View(name = "all_nodes_by_name", map = "classpath:nodesByName.js"),
    @View(name = "all_nodes_by_uid", map = "classpath:nodesByUid.js")
})
public class NodeDAOCouchDbImpl extends CouchDbRepositorySupport<Node> implements NodeDAO {

//  private final CouchDbConnector db;

  public NodeDAOCouchDbImpl(CouchDbConnector db)  {
    super(Node.class, db);
//    CouchDbRepositorySupport support = new CouchDbRepositorySupport(null, null, null);

    initStandardDesignDocument();
  }

  @Override
  public <C extends Content> Node<C> getByKey(EntityKeys<C> keyset, boolean loadContent) throws GraphModelException {

    List<NodeModification> updates = new ArrayList<>();
    for (String uid : keyset.getUids()) {
      updates.addAll(findUpdatesByUid(uid));
    }
    for (String name : keyset.getNames()) {
      updates.addAll(findUpdatesByName(keyset.getType(), name));
    }

    //TODO sort and merge by timestamp here. For now, just return the first NodeModification
    if (updates.isEmpty()) {
      return null;
    }
    NodeModification first = updates.iterator().next();
    Node<C> node = first.getNode();

    return node;
  }

  private List<NodeModification> findUpdatesByUid(String uid) {
    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
    ViewQuery query = createQuery("all_nodes_by_uid");

    //Next, specify a key or key range on to return from the view.
    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
    // Here, we specify the Node's UID only, and want to accept any timestamp
    query = query
        .startKey(ComplexKey.of(uid))
        .endKey(ComplexKey.of(uid, ComplexKey.emptyObject()));

    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
    List<NodeModification> updates = db.queryView(query, NodeModification.class);

    System.out.println("Found "+updates.size()+" modification entries for the node with UID: "+uid);

    return updates;
  }

  private List<NodeModification> findUpdatesByName(String typeName, String name) {
    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
    ViewQuery query = createQuery("all_nodes_by_name");

    //Next, specify a key or key range on to return from the view.
    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
    // Here, we specify the Node's type and name as the key, and want to accept any timestamp
    query = query
        .startKey(ComplexKey.of(typeName, name))
        .endKey(ComplexKey.of(typeName, name, ComplexKey.emptyObject()));

    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
    List<NodeModification> updates = db.queryView(query, NodeModification.class);

    System.out.println("Found "+updates.size()+" modification entries for the node with type: "+typeName+", name: "+name);

    return updates;
  }

  /**
   * Given a partial keyset (a keyset suspected of containing less then the complete number of UIDs or names for a
   * given node), queries the database and returns a fully populated keyset.
   * @param partial
   * @return
   */
  public <T> EntityKeys<T> populateFullKeyset(EntityKeys<T> partial) {
    EntityKeys full = new EntityKeys();
    full.setType(partial.getType());
    Set<String> knownUids = full.getUids();
    Set<String> knownNames = full.getNames();

    for (String uid : partial.getUids()) {
      if (!knownUids.contains(uid)) {
        knownUids.addAll(findAllUidsForUid(uid));
      }
    }
    for (String name : partial.getNames()) {
      if (!knownNames.contains(name)) {
        knownNames.addAll(findAllNamesForNode(partial.getType(), name));
      }
    }

    return full;
  }

  /**
   * Given a node UID, finds all other UIDs that the node is known by.
   * Queries a view whose rows look like:
   *
   * [uid1] --> [uid1, uid2, uid3, ...]
   * [uid1] --> [uid1, uid4, ...]
   *
   * We simply read each row, and extract all known names.
   *
   * @param uid
   * @return
   */
  private Set<String> findAllUidsForUid(String uid) {
    Set<String> queriedFor = new HashSet<>();
//    Set<String> allUids = new HashSet<>();
    findAllUidsForUid(uid, queriedFor);
    return queriedFor;
  }
  private void findAllUidsForUid(String uid, Set<String> queriedFor) { //}, Set<String> allUids) {
//    allUids.add(uid);
    queriedFor.add(uid);

    ViewQuery query = createQuery("uidToUids");
    query = query.key(uid);

    ViewResult result = db.queryView(query);
    //Read each row
    for (ViewResult.Row row : result.getRows()) {
      // Read each name in each row
      for (JsonNode uidNode : row.getValueAsNode()) {
        String nextUid = uidNode.asText();
        if (!queriedFor.contains(nextUid)) {
          findAllUidsForUid(nextUid, queriedFor); //, allUids);
        }
      }
    }
  }

  /**
   * Given a node name, finds all other names that the node is known by.
   * Queries a view whose rows look like:
   *
   * [type, name1] --> [name1, name2, name3, ...]
   * [type, name1] --> [name1, name4, ...]
   *
   * We simply read each row, and extract all known names.
   *
   * @param name
   * @return
   */
  private Set<String> findAllNamesForNode(String typeName, String name) {
    Set<String> queriedFor = new HashSet<>();
    findAllNamesForNode(typeName, name, queriedFor);
    return queriedFor;
  }
  private void findAllNamesForNode(String typeName, String name, Set<String> queriedFor) {
    queriedFor.add(name);

    ViewQuery query = createQuery("nameToNames");
    query = query.key(ComplexKey.of(typeName, name));

    ViewResult result = db.queryView(query);
    //Read each row
    for (ViewResult.Row row : result.getRows()) {
      // Read each name in each row
      for (JsonNode nameNode : row.getValueAsNode()) {
        String nextName = nameNode.asText();
        if (!queriedFor.contains(nextName)) {
          findAllUidsForUid(nextName, queriedFor); //, allUids);
        }
      }
    }
  }

//  @Override
//  public <C extends Content> Node<C> getByUid(String uid, boolean loadContent) throws GraphModelException {
//    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
//    ViewQuery query = createQuery("all_nodes_by_uid");
//
//    //Next, specify a key or key range on to return from the view.
//    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
//    // Here, we specify the Node's UID only, and want to accept all modifications (will accept any timestamp)
//    query = query.key(ComplexKey.of(uid, ComplexKey.emptyObject()));
//
//    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
//    List<NodeModification> updates = db.queryView(query, NodeModification.class);
//
//    System.out.println("Found "+updates.size()+" modification entries for the node with UID: "+uid);
//
//    //TODO merge here. For now, just return the first NodeModification
//    if (updates.isEmpty()) {
//      return null;
//    }
//    NodeModification first = updates.iterator().next();
//    Node<C> node = first.getNode();
//
//    return node;
//  }

//  @Override
//  public <C extends Content> Node<C> getByUid(String uid, Class<C> castToType) throws GraphModelException {
//
//    return null;
//  }

//  @View( name = "all_nodes_by_name", map = "classpath:nodesByName.js")
  public List<NodeModification> getAllNodes3() {
//    ViewQuery query = new ViewQuery()
//        .designDocId("_design/nodes_by_name")
//        .viewName("_all");
////        .key("red");

    ViewQuery query = createQuery("all_nodes_by_name");

    List<NodeModification> nodes = db.queryView(query, NodeModification.class);
    return nodes;
  }

//  @View( name = "all_nodes_by_uid", map = "classpath:nodesByUid.js")
  public List<NodeModification> getAllNodes4() {
    ViewQuery query = createQuery("all_nodes_by_uid");

    List<NodeModification> nodes = db.queryView(query, NodeModification.class);
    return nodes;
  }

//  @View( name = "all_nodes_by_name", map = "classpath:nodesByName.js")
//  public List<Node> getAllNodes3() {
//    ViewQuery query = new ViewQuery()
//        .designDocId("_design/nodes_by_name")
//        .viewName("all");
////        .key("red");
//
//    List<Node> nodes = db.queryView(query, Node.class);
//    return nodes;
//  }
}
