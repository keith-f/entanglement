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
import com.entanglementgraph.util.EntityKeyElementCache;
import com.entanglementgraph.util.InMemoryEntityKeyElementCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.*;
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
    @View(name = "all_edges_by_name", map = "classpath:nodesByName.js"),
    @View(name = "all_edges_by_uid", map = "classpath:nodesByUid.js"),
    @View(name = "nodes_and_edges", map = "classpath:nodesAndEdgesMap.js", reduce = "classpath:nodesAndEdgesReduce.js")
})
public class EdgeDAOCouchDbImpl extends CouchDbRepositorySupport<Edge> implements EdgeDAO {
  public static final String DESIGN_DOC_ID = "_design/"+Edge.class.getSimpleName();


  private static class EdgeModificationViewByTimestampComparator implements Comparator<EdgeModificationView> {
    @Override
    public int compare(EdgeModificationView o1, EdgeModificationView o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
  }

  private final ObjectMapper om;

  public EdgeDAOCouchDbImpl(CouchDbConnector db, ObjectMapper om)  {
    super(Edge.class, db);

    initStandardDesignDocument(); // Causes Ektorp to create the views listed above
    this.om = om;
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
  public <C extends Content, F extends Content, T extends Content> Edge<C, F, T> getByKey(EntityKeys<C> keyset)
      throws GraphModelException {

    List<EdgeModificationView> updates = new ArrayList<>();
    for (String uid : keyset.getUids()) {
      updates.addAll(findUpdatesByUid(uid));
    }
    for (String name : keyset.getNames()) {
      updates.addAll(findUpdatesByName(keyset.getType(), name));
    }

    // If the edge doesn't exist in the database, return null.
    // Otherwise, find all revisions and construct a 'merged' node.
    Edge<C, F, T> edge = null;
    if (!updates.isEmpty()) {
      edge = mergeRevisions(updates);
      edge.setLoaded(true);
    }
    return edge;
  }

  @Override
  public <C extends Content> boolean existsByKey(EntityKeys<C> keyset) throws GraphModelException {
    return getByKey(keyset) != null;
  }

  @Override
  public <C extends Content, F extends Content, T extends Content> Iterable<Edge<C, F, T>> iterateEdgesBetweenNodes(EntityKeys<F> fromNode, EntityKeys<T> to) throws GraphModelException {
    return null;
  }

  @Override
  public <C extends Content, F extends Content, T extends Content> Iterable<Edge<C, F, T>> iterateEdgesBetweenNodes(String edgeType, EntityKeys<F> from, EntityKeys<T> to) throws GraphModelException {
    return null;
  }

  @Override
  public <C extends Content, F extends Content, T extends Content> Iterable<Edge<C, F, T>> iterateEdgesFromNode(EntityKeys<F> fromFullKeyset) throws GraphModelException {
    try {
      EntityKeyElementCache seenEdges = new InMemoryEntityKeyElementCache();

      Set<String> nodeNamesToCheck = new HashSet<>();
      nodeNamesToCheck.addAll(from.getNames());
      while (!nodeNamesToCheck.isEmpty()) {
        for (String name : nodeNamesToCheck) {

          ViewQuery query = createQuery("nodes_and_edges");
          query = query
              .key(ComplexKey.of(from.getType(), name))
              .reduce(true)
              .group(true)
              .groupLevel(2);

          StreamingViewResult result2 = db.queryForStreamingView(query);


          ViewResult result = db.queryView(query);
          /*
           * Read each row.
           * Here, a keys will look like:
           * [node_type, a, 0, [a, b, c]]               (node type, node name, followed by 0, followed by node synonyms)
           * [node_type, a, 1, edge_type, [x, y, z]]    (node type, node name, followed by 1, followed by edge names for edges FROM the named node)
           *
           * Values will be of the form:
           * TODO... (NodeMod docs for nodes, EdgeMod docs for edges??)
           */
          for (ViewResult.Row row : result.getRows()) {
            JsonNode key = row.getKeyAsNode();
            String nodeType = key.get(0).asText();
            String nodeName = key.get(1).asText();
            int docType = key.get(2).asInt();

            if (docType == 0) {
              JsonNode otherNameList = key.get(3);
              log.info("Found node entry with node type:"+nodeType+", node name: "+nodeName+", other node names: "+otherNameList.asText());
              // Add new node synonyms to the list to check
              for (JsonNode otherNameEntry : otherNameList) {
                nodeNamesToCheck.add(otherNameEntry.asText());
              }
            } else if (docType == 1) {
              String edgeType = key.get(3).asText();
              JsonNode otherNameList = key.get(4);
              log.info("Found edge entry with node type:"+nodeType+", node name: "+nodeName
                  +", edge type: "+edgeType+", edge names: "+otherNameList.asText());
              EntityKeys fromEdge = new EntityKeys();
              fromEdge.setType(edgeType);
              for (JsonNode edgeNameEntry : otherNameList) {
                fromEdge.addName(edgeNameEntry.asText());
              }
              seenEdges.cacheElementsOf(fromEdge);
            } else {
              throw new GraphModelException("Unsupported document type found in view: "+docType+" on row: "+row);
            }

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
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public Iterable<JsonNode> iterateEdgesFromNode(String edgeType, EntityKeys from) throws GraphModelException {
//    try {
//      queriedFor.add(name);
//
//      ViewQuery query = createQuery("nodes_and_edges");
//      query = query.key(ComplexKey.of(typeName, name));
//
//      ViewResult result = db.queryView(query);
//      //Read each row
//      for (ViewResult.Row row : result.getRows()) {
//        // Read each name in each row
//        for (JsonNode nameNode : row.getValueAsNode()) {
//          String nextName = nameNode.asText();
//          if (!queriedFor.contains(nextName)) {
//            findAllOtherNamesForName(typeName, nextName, queriedFor); //, allUids);
//          }
//        }
//      }
//    } catch (Exception e) {
//      throw new GraphModelException("Failed to perform query", e);
//    }
    return null;
  }

  @Override
  public Iterable<JsonNode> iterateEdgesFromNodeToNodeOfType(EntityKeys<? extends Node> from, String toNodeType) throws GraphModelException {
    return null;
  }

  @Override
  public Iterable<JsonNode> iterateEdgesToNodeFromNodeOfType(EntityKeys<? extends Node> to, String fromNodeType) throws GraphModelException {
    return null;
  }

  @Override
  public Iterable<JsonNode> iterateEdgesToNode(EntityKeys to) throws GraphModelException {
    return null;
  }

  @Override
  public Iterable<JsonNode> iterateEdgesToNode(String edgeType, EntityKeys to) throws GraphModelException {
    return null;
  }

  @Override
  public boolean existsEdgeToNodeOfType(EntityKeys from, String toNodeType) throws GraphModelException {
    return false;
  }

  @Override
  public Long countEdgesFromNode(EntityKeys from) throws GraphModelException {
    return null;
  }

  @Override
  public Long countEdgesOfTypeFromNode(String edgeType, EntityKeys from) throws GraphModelException {
    return null;
  }

  @Override
  public Long countEdgesToNode(EntityKeys to) throws GraphModelException {
    return null;
  }

  @Override
  public Long countEdgesOfTypeToNode(String edgeType, EntityKeys to) throws GraphModelException {
    return null;
  }

  @Override
  public Long countEdgesOfTypeBetweenNodes(String edgeType, EntityKeys from, EntityKeys to) throws GraphModelException {
    return null;
  }

  @Override
  public Map<String, Long> countEdgesByTypeFromNode(EntityKeys from) throws GraphModelException {
    return null;
  }

  @Override
  public Map<String, Long> countEdgesByTypeToNode(EntityKeys to) throws GraphModelException {
    return null;
  }


  /*
   * Internal methods
   */

  private <C extends Content, F extends Content, T extends Content> Edge<C, F, T> mergeRevisions(
      List<EdgeModificationView> mods) throws GraphModelException {
    Collections.sort(mods, new EdgeModificationViewByTimestampComparator());

    EdgeMerger<C, F, T> merger = new EdgeMerger<>();
    Edge<C, F, T> merged = null;
    for (EdgeModificationView<C, F, T> mod : mods) {
      if (merged == null) {
        merged = mod.getEdge();
      } else {
        Edge<C, F, T> newEdge = mod.getEdge();
        merged = merger.merge(mod.getMergePol(), merged, newEdge);
      }
    }
    return merged;
  }


  private List<EdgeModificationView> findUpdatesByUid(String uid) {
    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
    ViewQuery query = createQuery("all_edges_by_uid");

    //Next, specify a key or key range on to return from the view.
    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
    // Here, we specify the Node's UID only, and want to accept any timestamp
    query = query
        .startKey(ComplexKey.of(uid))
        .endKey(ComplexKey.of(uid, ComplexKey.emptyObject()));

    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
    List<EdgeModificationView> updates = db.queryView(query, EdgeModificationView.class);


    System.out.println("Found "+updates.size()+" modification entries for the edge with UID: "+uid);

    return updates;
  }

  private List<EdgeModificationView> findUpdatesByName(String typeName, String name) {
    // Creates a ViewQuery with the 'standard' design doc name + the specified view name
    ViewQuery query = createQuery("all_edges_by_name");

    //Next, specify a key or key range on to return from the view.
    // If you want to use a wild card in your key, often used in date ranges, add a ComplexKey.emptyObject()
    // Here, we specify the Node's type and name as the key, and want to accept any timestamp
    query = query
        .startKey(ComplexKey.of(typeName, name))
        .endKey(ComplexKey.of(typeName, name, ComplexKey.emptyObject()));

    // Pull back all matching docs as an in-memory List. This should be fine since we're querying for a single node.
    List<EdgeModificationView> updates = db.queryView(query, EdgeModificationView.class);

    System.out.println("Found "+updates.size()+" modification entries for the edge with type: "+typeName+", name: "+name);

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
  private void findAllOtherUidsForUid(String uid, Set<String> queriedFor) {
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
