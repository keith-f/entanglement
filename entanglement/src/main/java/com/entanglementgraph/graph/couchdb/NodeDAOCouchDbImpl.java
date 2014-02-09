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

import com.entanglementgraph.graph.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.*;

/**
 * @author Keith Flanagan
 */
@Views({
    @View(name = "nameToNames", map = "classpath:nodeNameToNamesMap.js"),
    @View(name = "uidToUids", map = "classpath:nodeUidToUidsMap.js"),
    @View(name = "all_nodes_by_name", map = "classpath:nodesByName.js"),
    @View(name = "all_nodes_by_uid", map = "classpath:nodesByUid.js")
})
public class NodeDAOCouchDbImpl extends CouchDbRepositorySupport<Node> implements NodeDAO {

  private static class NodeModificationViewByTimestampComparator implements Comparator<NodeModificationView> {
    @Override
    public int compare(NodeModificationView o1, NodeModificationView o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
  }

  public NodeDAOCouchDbImpl(CouchDbConnector db)  {
    super(Node.class, db);
//    CouchDbRepositorySupport support = new CouchDbRepositorySupport(null, null, null);

    initStandardDesignDocument(); // Causes Ektorp to create the views listed above
  }


  public <T> EntityKeys<T> populateFullKeyset(EntityKeys<T> partial) throws GraphModelException {
    EntityKeys full = new EntityKeys();
    full.setType(partial.getType());
    Set<String> knownUids = full.getUids();
    Set<String> knownNames = full.getNames();

    for (String uid : partial.getUids()) {
      if (!knownUids.contains(uid)) {
        knownUids.addAll(findAllOtherUidsForUid(uid));
      }
    }
    for (String name : partial.getNames()) {
      if (!knownNames.contains(name)) {
        knownNames.addAll(findAllOtherNamesForName(partial.getType(), name));
      }
    }

    return full;
  }

  @Override
  public <C extends Content> Node<C> getByKey(EntityKeys<C> keyset) throws GraphModelException {

    List<NodeModificationView> updates = new ArrayList<>();
    for (String uid : keyset.getUids()) {
      updates.addAll(findUpdatesByUid(uid));
    }
    for (String name : keyset.getNames()) {
      updates.addAll(findUpdatesByName(keyset.getType(), name));
    }

    // Create a 'virtual' node if there's no database entry.
    // Otherwise, find all revisions and construct a 'merged' node.
    Node<C> node;
    if (updates.isEmpty()) {
      node = new Node<>();
      node.setKeys(keyset);
      node.setLoaded(false);
      node.setVirtual(true);
    } else {
      node = mergeRevisions(updates);
      node.setLoaded(true);
    }
    return node;
  }

  @Override
  public <C extends Content> boolean existsByKey(EntityKeys<C> keyset) throws GraphModelException {
    return !getByKey(keyset).isVirtual();
  }

//  @Override
//  public Iterable<Node<? extends Content>> iterateAll() throws GraphModelException {
//
//    return null;
//  }
//
//  @Override
//  public long countAll() throws GraphModelException {
//    return 0;
//  }
//
//  @Override
//  public <C extends Content> Iterable<Node<C>> iterateByType(String typeName) throws GraphModelException {
//    return null;
//  }
//
//  @Override
//  public long countByType(String typeName) throws GraphModelException {
//    return 0;
//  }
//
//  @Override
//  public List<String> listTypes() throws GraphModelException {
//    return null;
//  }




  /*
   * Internal methods
   */

  private <C extends Content> Node<C> mergeRevisions(List<NodeModificationView> mods) throws GraphModelException {
    Collections.sort(mods, new NodeModificationViewByTimestampComparator());

    NodeMerger<C> merger = new NodeMerger<>();
    Node<C> merged = null;
    for (NodeModificationView<C> mod : mods) {
      if (merged == null) {
        merged = mod.getNode();
      } else {
        Node<C> newNode = mod.getNode();
        merged = merger.merge(mod.getMergePol(), merged, newNode);
      }
    }
    return merged;
  }


  private List<NodeModificationView> findUpdatesByUid(String uid) {
    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
    ViewQuery query = createQuery("all_nodes_by_uid");

    //Next, specify a key or key range on to return from the view.
    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
    // Here, we specify the Node's UID only, and want to accept any timestamp
    query = query
        .startKey(ComplexKey.of(uid))
        .endKey(ComplexKey.of(uid, ComplexKey.emptyObject()));

    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
    List<NodeModificationView> updates = db.queryView(query, NodeModificationView.class);


    System.out.println("Found "+updates.size()+" modification entries for the node with UID: "+uid);

    return updates;
  }

  private List<NodeModificationView> findUpdatesByName(String typeName, String name) {
    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
    ViewQuery query = createQuery("all_nodes_by_name");

    //Next, specify a key or key range on to return from the view.
    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
    // Here, we specify the Node's type and name as the key, and want to accept any timestamp
    query = query
        .startKey(ComplexKey.of(typeName, name))
        .endKey(ComplexKey.of(typeName, name, ComplexKey.emptyObject()));

    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
    List<NodeModificationView> updates = db.queryView(query, NodeModificationView.class);

//    ObjectMapper mapper = new ObjectMapper();
//    ViewResult result = db.queryView(query);
//    for (ViewResult.Row row : result) {
//      JsonNode timestampField = row.getKeyAsNode().get(2);
//      Date ts = mapper.readValue(timestampField.asText(), Date.class);
//    }

    System.out.println("Found "+updates.size()+" modification entries for the node with type: "+typeName+", name: "+name);

    return updates;
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
  private Set<String> findAllOtherUidsForUid(String uid) {
    Set<String> queriedFor = new HashSet<>();
//    Set<String> allUids = new HashSet<>();
    findAllOtherUidsForUid(uid, queriedFor);
    return queriedFor;
  }
  private void findAllOtherUidsForUid(String uid, Set<String> queriedFor) { //}, Set<String> allUids) {
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
          findAllOtherUidsForUid(nextUid, queriedFor);
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
  private Set<String> findAllOtherNamesForName(String typeName, String name) {
    Set<String> queriedFor = new HashSet<>();
    findAllOtherNamesForName(typeName, name, queriedFor);
    return queriedFor;
  }
  private void findAllOtherNamesForName(String typeName, String name, Set<String> queriedFor) {
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
          findAllOtherNamesForName(typeName, nextName, queriedFor); //, allUids);
        }
      }
    }
  }

}
