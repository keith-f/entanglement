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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.io.IOException;
import java.util.*;

/**
 * @author Keith Flanagan
 */
@Views({
    @View(name = "nameToNames", map = "classpath:nodeNameToNamesMap.js"),
    @View(name = "uidToUids", map = "classpath:nodeUidToUidsMap.js"),
    @View(name = "all_nodes_by_name", map = "classpath:nodesByName.js"),
    @View(name = "all_nodes_by_uid", map = "classpath:nodesByUid.js"),
    @View(name = "nodes_and_edges", map = "classpath:nodesAndEdgesMap.js", reduce = "classpath:nodesAndEdgesReduce.js")
})
public class NodeDAOCouchDbImpl<C extends Content> extends CouchDbRepositorySupport<Node> implements NodeDAO<C> {

  private static final String VIEW_NODES_AND_EDGES = "nodes_and_edges";

  private final ObjectMapper om;

  private static class NodeModificationViewByTimestampComparator implements Comparator<NodeUpdateView> {
    @Override
    public int compare(NodeUpdateView o1, NodeUpdateView o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
  }

  public NodeDAOCouchDbImpl(CouchDbConnector db, ObjectMapper om)  {
    super(Node.class, db);
//    CouchDbRepositorySupport support = new CouchDbRepositorySupport(null, null, null);

    initStandardDesignDocument(); // Causes Ektorp to create the views listed above
    this.om = om;
  }


  public EntityKeys<C> populateFullKeyset(EntityKeys<C> partial) throws GraphModelException {
    return findAllIdentifiersAndUpdatesFor(partial).getFullKeyset();
//    EntityKeys full = new EntityKeys();
//    full.setType(partial.getType());
//    Set<String> knownUids = full.getUids();
//    Set<String> knownNames = full.getNames();
//
//    for (String uid : partial.getUids()) {
//      if (!knownUids.contains(uid)) {
//        knownUids.addAll(findAllOtherUidsForUid(uid));
//      }
//    }
//    for (String name : partial.getNames()) {
//      if (!knownNames.contains(name)) {
//        knownNames.addAll(findAllOtherNamesForName(partial.getType(), name));
//      }
//    }
//
//    return full;
  }

  @Override
  public Node<C> getByKey(EntityKeys<C> keyset) throws GraphModelException {
    LookupResult result = findAllIdentifiersAndUpdatesFor(keyset);

    List<NodeUpdateView> updates = result.getAllUpdates();
//    for (String uid : keyset.getUids()) {
//      updates.addAll(findUpdatesByUid(uid));
//    }
//    for (String name : keyset.getNames()) {
//      updates.addAll(findUpdatesByName(keyset.getType(), name));
//    }

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
  public boolean existsByKey(EntityKeys<C> keyset) throws GraphModelException {
    return !getByKey(keyset).isVirtual();
  }

  @Override
  public Iterable<Node<C>> iterateAll() throws GraphModelException {
    /*
     * WARNING: this implementation is suitable only for small graphs.
     * Due to the nature of data integration, the number of graph elements is only known after iterating.
     * This method must keep track of all 'seen' node identifiers, and is therefore only suitable for datasets whose
     * identifiers fit into RAM.
     */



    return null;
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

  private <C extends Content> Node<C> mergeRevisions(List<NodeUpdateView> mods) throws GraphModelException {
    Collections.sort(mods, new NodeModificationViewByTimestampComparator());

    NodeMerger<C> merger = new NodeMerger<>();
    Node<C> merged = null;
    for (NodeUpdateView<C> mod : mods) {
      if (merged == null) {
        merged = mod.getNode();
      } else {
        Node<C> newNode = mod.getNode();
        merged = merger.merge(mod.getMergePol(), merged, newNode);
      }
    }
    return merged;
  }


//  private List<NodeUpdateView> findUpdatesByUid(String uid) {
//    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
//    ViewQuery query = createQuery("all_nodes_by_uid");
//
//    //Next, specify a key or key range on to return from the view.
//    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
//    // Here, we specify the Node's UID only, and want to accept any timestamp
//    query = query
//        .startKey(ComplexKey.of(uid))
//        .endKey(ComplexKey.of(uid, ComplexKey.emptyObject()));
//
//    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
//    List<NodeUpdateView> updates = db.queryView(query, NodeUpdateView.class);
//
//
//    System.out.println("Found "+updates.size()+" modification entries for the node with UID: "+uid);
//
//    return updates;
//  }

//  private List<NodeUpdateView> findUpdatesByName(String typeName, String name) {
//    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
//    ViewQuery query = createQuery("all_nodes_by_name");
//
//    //Next, specify a key or key range on to return from the view.
//    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
//    // Here, we specify the Node's type and name as the key, and want to accept any timestamp
//    query = query
//        .startKey(ComplexKey.of(typeName, name))
//        .endKey(ComplexKey.of(typeName, name, ComplexKey.emptyObject()));
//
//    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
//    List<NodeUpdateView> updates = db.queryView(query, NodeUpdateView.class);
//
////    ObjectMapper mapper = new ObjectMapper();
////    ViewResult result = db.queryView(query);
////    for (ViewResult.Row row : result) {
////      JsonNode timestampField = row.getKeyAsNode().get(2);
////      Date ts = mapper.readValue(timestampField.asText(), Date.class);
////    }
//
//    System.out.println("Found "+updates.size()+" modification entries for the node with type: "+typeName+", name: "+name);
//
//    return updates;
//  }



//  /**
//   * Given a node UID, finds all other UIDs that the node is known by.
//   * Queries a view whose rows look like:
//   *
//   * [uid1] --> [uid1, uid2, uid3, ...]
//   * [uid1] --> [uid1, uid4, ...]
//   *
//   * We simply read each row, and extract all known names.
//   *
//   * @param uid
//   * @return
//   */
//  private Set<String> findAllOtherUidsForUid(String uid) {
//    Set<String> queriedFor = new HashSet<>();
////    Set<String> allUids = new HashSet<>();
//    findAllOtherUidsForUid(uid, queriedFor);
//    return queriedFor;
//  }
//  private void findAllOtherUidsForUid(String uid, Set<String> queriedFor) { //}, Set<String> allUids) {
////    allUids.add(uid);
//    queriedFor.add(uid);
//
//    ViewQuery query = createQuery("uidToUids");
//    query = query.key(uid);
//
//    ViewResult result = db.queryView(query);
//    //Read each row
//    for (ViewResult.Row row : result.getRows()) {
//      // Read each name in each row
//      for (JsonNode uidNode : row.getValueAsNode()) {
//        String nextUid = uidNode.asText();
//        if (!queriedFor.contains(nextUid)) {
//          findAllOtherUidsForUid(nextUid, queriedFor);
//        }
//      }
//    }
//  }

//  /**
//   * Given a node name, finds all other names that the node is known by.
//   * Queries a view whose rows look like:
//   *
//   * [type, name1] --> [name1, name2, name3, ...]
//   * [type, name1] --> [name1, name4, ...]
//   *
//   * We simply read each row, and extract all known names.
//   *
//   * @param name
//   * @return
//   */
//  private Set<String> findAllOtherNamesForName(String typeName, String name) {
//    Set<String> queriedFor = new HashSet<>();
//    findAllOtherNamesForName(typeName, name, queriedFor);
//    return queriedFor;
//  }
//  private void findAllOtherNamesForName(String typeName, String name, Set<String> queriedFor) {
//    queriedFor.add(name);
//
//    ViewQuery query = createQuery("nameToNames");
//    query = query.key(ComplexKey.of(typeName, name));
//
//    ViewResult result = db.queryView(query);
//    //Read each row
//    for (ViewResult.Row row : result.getRows()) {
//      // Read each name in each row
//      for (JsonNode nameNode : row.getValueAsNode()) {
//        String nextName = nameNode.asText();
//        if (!queriedFor.contains(nextName)) {
//          findAllOtherNamesForName(typeName, nextName, queriedFor); //, allUids);
//        }
//      }
//    }
//  }

  private static enum IdentifierType {
    UID,
    NAME;
  }

  private static class LookupResult {
    private final EntityKeys queryKeyset;
    private final EntityKeys fullKeyset;
    private final List<NodeUpdateView> allUpdates;

    private LookupResult(EntityKeys queryKeyset, EntityKeys fullKeyset, List<NodeUpdateView> allUpdates) {
      this.queryKeyset = queryKeyset;
      this.fullKeyset = fullKeyset;
      this.allUpdates = allUpdates;
    }

    public EntityKeys getQueryKeyset() {
      return queryKeyset;
    }

    public EntityKeys getFullKeyset() {
      return fullKeyset;
    }

    public List<NodeUpdateView> getAllUpdates() {
      return allUpdates;
    }
  }

  /**
   * Given a keyset describing a node, performs iterative database queries to return:
   *   a) the complete keyset containing all identifiers the node is known by
   *   b) the complete list of revision documents containing all updates to the node over time.
   *
   * @param queryKeyset the partial, or full node's Keyset. At least one identifier must be specified. Other identifiers
   *                    will be found and returned in the result.
   * @return an object containing the query keyset, the full keyset and the full list of modifications for the
   * specified node.
   * @throws IOException
   */
  private LookupResult findAllIdentifiersAndUpdatesFor(EntityKeys<C> queryKeyset)
      throws GraphModelException {
    EntityKeys fullKeyset = new EntityKeys(); // Keeps track of which identifiers we've already queried for.
    List<NodeUpdateView> foundUpdates = new ArrayList<>();    //List of update documents found so far.

    for (String uid : queryKeyset.getUids()) {
      findAllIdentifiersAndUpdatesFor(IdentifierType.UID, queryKeyset.getType(), uid, fullKeyset, foundUpdates);
    }
    for (String name : queryKeyset.getNames()) {
      findAllIdentifiersAndUpdatesFor(IdentifierType.NAME, queryKeyset.getType(), name, fullKeyset, foundUpdates);
    }


    LookupResult result = new LookupResult(queryKeyset, fullKeyset, foundUpdates);
    return result;
  }

  private void findAllIdentifiersAndUpdatesFor(IdentifierType idType, String typeName, String identifier,
                                          EntityKeys fullKeyset, List<NodeUpdateView> foundUpdates)
      throws GraphModelException {
    ViewQuery query = createQuery(VIEW_NODES_AND_EDGES);
    switch (idType) {
      case NAME:
        fullKeyset.addName(identifier);
        query = query.key(ComplexKey.of(typeName, "N", identifier));
        break;
      case UID:
        fullKeyset.addUid(identifier);
        query = query.key(ComplexKey.of(typeName, "U", identifier));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported identifier type: "+idType);
    }
    ViewResult result = db.queryView(query.reduce(true));
    //Read each row
    for (ViewResult.Row row : result.getRows()) {
      Iterator<JsonNode> keyItr = row.getKeyAsNode().iterator();
      String nodeTypeName = keyItr.next().asText(); // Eg: "Gene". Should be equal to <code>typeName</code>
      String uidOrName = keyItr.next().asText();    // Either 'U' or 'N'
      String identifier2 = keyItr.next().asText();  // Should be equal to <code>identifier</code>
      int rowType =  keyItr.next().asInt();         //0=node; 1=edgeFrom; ...
      JsonNode value = row.getValueAsNode();

      // If this result row represents a node, then append all NodeUpdates to the discovery list
      for (JsonNode updateNode : value.get("nodeUpdates")) {
        try {
          NodeUpdateView update = om.readValue(updateNode.asText(), NodeUpdateView.class);
          foundUpdates.add(update);
        } catch(IOException e) {
          throw new GraphModelException("Failed to decode NodeUpdateView. Raw text was: "+updateNode.asText(), e);
        }
      }

      /*
       * Regardless of the row type, there will be a field 'nodeNames' and 'nodeUids' that contain all node identifiers
       * that are relevant to the current search. We should extract all identifiers, find out if we've encountered them
       * before, and perform a recursive query for each 'new' identifier.
       */

      // Extract the 'nodeNames' from the value. This should be a list of strings.
      // Check to see if any of these names are 'new' to us.
      for (JsonNode nameJsonNode : value.get("nodeNames")) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getNames().contains(nameJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(IdentifierType.NAME, typeName, nameJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
      for (JsonNode uidJsonNode : value.get("nodeUids")) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getNames().contains(uidJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(IdentifierType.UID, typeName, uidJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
    }
  }

}
