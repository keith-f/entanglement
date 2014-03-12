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
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.couchdb.viewparsers.EdgesViewRowParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.*;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
@Views({
    @View(name = "edges", map = "classpath:edgesMap.js"),
    @View(name = "edges_between_nodes", map = "classpath:edgesBetweenNodesMap.js")
})
public class EdgeDAOCouchDbImpl<C extends Content, F extends Content, T extends Content>
    extends CouchDbRepositorySupport<Edge> implements EdgeDAO<C, F, T> {
  private static final Logger logger = Logger.getLogger(EdgeDAOCouchDbImpl.class.getName());

  public static final String DESIGN_DOC_ID = "_design/"+Edge.class.getSimpleName();

  public static class EdgeLookupResult {
    private final EntityKeys queryKeyset;
    private final EntityKeys fullKeyset;
    private final List<EdgeUpdateView> allUpdates;

    private EdgeLookupResult(EntityKeys queryKeyset, EntityKeys fullKeyset, List<EdgeUpdateView> allUpdates) {
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

    public List<EdgeUpdateView> getAllUpdates() {
      return allUpdates;
    }
  }

  private static class EdgeModificationViewByTimestampComparator implements Comparator<EdgeUpdateView> {
    @Override
    public int compare(EdgeUpdateView o1, EdgeUpdateView o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
  }

  private final ObjectMapper om;

  public EdgeDAOCouchDbImpl(CouchDbConnector db, ObjectMapper om)  {
    super(Edge.class, db);

    initStandardDesignDocument(); // Causes Ektorp to create the views listed above
    this.om = om;
  }

  @Override
  public <C extends Content, F extends Content, T extends Content> EdgeDAO<C, F, T> forContent(
      Class<C> contentType, Class<F> fromType, Class<T> toType) {
    return new EdgeDAOCouchDbImpl<>(db, om);
  }


  public EntityKeys<C> populateFullKeyset(EntityKeys<C> partial) throws GraphModelException {
    return findAllIdentifiersAndUpdatesFor(partial).getFullKeyset();
  }

  @Override
  public Edge<C, F, T> getByKey(EntityKeys<C> partialOrFullKeyset)
      throws GraphModelException {
    EdgeLookupResult result = findAllIdentifiersAndUpdatesFor(partialOrFullKeyset);

    List<EdgeUpdateView> updates = result.getAllUpdates();
//    logger.info("Found updates "+updates.size()+" for query keyset: "+partialOrFullKeyset);

    // Merge edge updates
    Edge<C, F, T> edge = null;
    if (!updates.isEmpty()) {
      edge = mergeRevisions(updates, MergePolicy.APPEND_NEW__LEAVE_EXISTING);
      edge.setLoaded(true);
    }
    return edge;
  }

  @Override
  public boolean existsByKey(EntityKeys<C> keyset) throws GraphModelException {
    return getByKey(keyset) != null;
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateAll() throws GraphModelException {
    /*
     * WARNING: this implementation is suitable only for small graphs.
     * Due to the nature of data integration, the number of graph elements is only known after iterating.
     * This method must keep track of all 'seen' node identifiers, and is therefore only suitable for datasets whose
     * identifiers fit into RAM.
     */
    IteratorForStreamingAllEdges itr = new IteratorForStreamingAllEdges(db, this);
    return itr;
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateByType(String typeName) throws GraphModelException {
    /*
     * WARNING: this implementation is suitable only for small graphs.
     * Due to the nature of data integration, the number of graph elements is only known after iterating.
     * This method must keep track of all 'seen' node identifiers, and is therefore only suitable for datasets whose
     * identifiers fit into RAM.
     */
    IteratorForStreamingAllEdges itr = new IteratorForStreamingAllEdges(db, this, typeName);
    return itr;
  }

  @Override
  public long countByType(String typeName) throws GraphModelException {
    /*
     * There isn't currently a sane way to do this since we don't know the number of items ahead of time.
     */
    long count = 0;
    for (Edge edge : iterateByType(typeName)) {
      count++;
    }
    return count;
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesBetweenNodes(EntityKeys<F> fromNode, EntityKeys<T> to) throws GraphModelException {
    try {
      IteratorForStreamingEdgesBetweenNodes<C, F, T> edgeItrable =
          new IteratorForStreamingEdgesBetweenNodes<>(db, this, fromNode, to);
      return edgeItrable;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesBetweenNodes(String edgeType, EntityKeys<F> from, EntityKeys<T> to) throws GraphModelException {
    try {
      IteratorForStreamingEdgesBetweenNodes<C, F, T> edgeItrable =
          new IteratorForStreamingEdgesBetweenNodes<>(db, this, from, to, edgeType);
      return edgeItrable;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesFromNode(EntityKeys<F> fromFullNodeKeyset) throws GraphModelException {
    try {
      IteratorForStreamingFromEdges<C, F, T> edgeItrable = new IteratorForStreamingFromEdges<>(db, this, fromFullNodeKeyset);
      return edgeItrable;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesFromNode(String edgeType, EntityKeys fromFullNodeKeyset) throws GraphModelException {
    try {
      IteratorForStreamingFromEdges<C, F, T> edgeItrable = new IteratorForStreamingFromEdges<>(db, this, fromFullNodeKeyset, edgeType);
      return edgeItrable;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesFromNodeToNodeOfType(EntityKeys<? extends Node> from, String toNodeType) throws GraphModelException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesToNodeFromNodeOfType(EntityKeys<? extends Node> to, String fromNodeType) throws GraphModelException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesToNode(EntityKeys toFullNodeKeyset) throws GraphModelException {
    try {
      IteratorForStreamingToEdges<C, F, T> edgeItrable = new IteratorForStreamingToEdges<>(db, this, toFullNodeKeyset);
      return edgeItrable;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public Iterable<Edge<C, F, T>> iterateEdgesToNode(String edgeType, EntityKeys toFullNodeKeyset) throws GraphModelException {
    try {
      IteratorForStreamingToEdges<C, F, T> edgeItrable = new IteratorForStreamingToEdges<>(db, this, toFullNodeKeyset, edgeType);
      return edgeItrable;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform query", e);
    }
  }

  @Override
  public boolean existsEdgeToNodeOfType(EntityKeys from, String toNodeType) throws GraphModelException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public long countEdgesFromNode(EntityKeys from) throws GraphModelException {
    /*
     * There isn't currently a sane way to do this since we don't know the number of items ahead of time.
     */
    long count = 0;
    for (Edge edge : iterateEdgesFromNode(from)) {
      count++;
    }
    return count;
  }

  @Override
  public long countEdgesOfTypeFromNode(String edgeType, EntityKeys from) throws GraphModelException {
    /*
     * There isn't currently a sane way to do this since we don't know the number of items ahead of time.
     */
    long count = 0;
    for (Edge edge : iterateEdgesFromNode(edgeType, from)) {
      count++;
    }
    return count;
  }

  @Override
  public long countEdgesToNode(EntityKeys to) throws GraphModelException {
    /*
     * There isn't currently a sane way to do this since we don't know the number of items ahead of time.
     */
    long count = 0;
    for (Edge edge : iterateEdgesToNode(to)) {
      count++;
    }
    return count;
  }

  @Override
  public long countEdgesOfTypeToNode(String edgeType, EntityKeys to) throws GraphModelException {
    /*
     * There isn't currently a sane way to do this since we don't know the number of items ahead of time.
     */
    long count = 0;
    for (Edge edge : iterateEdgesToNode(edgeType, to)) {
      count++;
    }
    return count;
  }

  @Override
  public long countEdgesOfTypeBetweenNodes(String edgeType, EntityKeys from, EntityKeys to) throws GraphModelException {
    /*
     * There isn't currently a sane way to do this since we don't know the number of items ahead of time.
     */
    long count = 0;
    for (Edge edge : iterateEdgesBetweenNodes(edgeType, from, to)) {
      count++;
    }
    return count;
  }

  @Override
  public Map<String, Long> countEdgesByTypeFromNode(EntityKeys from) throws GraphModelException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Map<String, Long> countEdgesByTypeToNode(EntityKeys to) throws GraphModelException {
    throw new UnsupportedOperationException("Not yet implemented");
  }


  /*
   * Internal methods
   */

  private <C extends Content, F extends Content, T extends Content> Edge<C, F, T> mergeRevisions(
      List<EdgeUpdateView> mods, MergePolicy mergePolicy) throws GraphModelException {
    Collections.sort(mods, new EdgeModificationViewByTimestampComparator());

    EdgeMerger<C, F, T> merger = new EdgeMerger<>();
    Edge<C, F, T> merged = null;
    for (EdgeUpdateView mod : mods) {
      if (merged == null) {
        merged = mod.getEdge();
      } else {
        Edge<C, F, T> newEdge = mod.getEdge();
        merger.merge(mergePolicy, merged, newEdge);
      }
    }
    return merged;
  }


  /**
   * Given a keyset describing a edge, performs iterative database queries to return:
   *   a) the complete keyset containing all identifiers the edge is known by
   *   b) the complete list of revision documents containing all updates to the edge over time.
   *
   * @param queryKeyset the partial, or full edge's Keyset. At least one identifier must be specified. Other identifiers
   *                    will be found and returned in the result.
   * @return an object containing the query keyset, the full keyset and the full list of modifications for the
   * specified edge.
   * @throws java.io.IOException
   */
  private EdgeLookupResult findAllIdentifiersAndUpdatesFor(EntityKeys<C> queryKeyset)
      throws GraphModelException {
    EntityKeys fullKeyset = new EntityKeys(); // Keeps track of which identifiers we've already queried for.
    List<EdgeUpdateView> foundUpdates = new ArrayList<>();    //List of update documents found so far.

    for (String uid : queryKeyset.getUids()) {
      findAllIdentifiersAndUpdatesFor(queryKeyset.getType(), uid, fullKeyset, foundUpdates);
    }

    EdgeLookupResult result = new EdgeLookupResult(queryKeyset, fullKeyset, foundUpdates);
    return result;
  }

  private void findAllIdentifiersAndUpdatesFor(String typeName, String identifier,
                                               EntityKeys fullKeyset, List<EdgeUpdateView> foundUpdates)
      throws GraphModelException {
//    System.out.println("Querying for: "+keyType+", "+typeName+", "+identifier+", "+fullKeyset);
    ViewQuery query = ViewQueryFactory.createEdgesQuery(db);
    fullKeyset.addUid(identifier);
    query = query
        .startKey(ComplexKey.of(typeName,identifier))
        .endKey(ComplexKey.of(typeName, identifier, ComplexKey.emptyObject()));


    ViewResult result = db.queryView(query);
    //Read each row
    for (ViewResult.Row row : result.getRows()) {
      EdgesViewRowParser parser = new EdgesViewRowParser(row);
      final int rowType = parser.getRowType();
      final JsonNode otherEdgeUids = parser.getOtherEdgeUids();

      JsonNode value = row.getValueAsNode();

      // If this result row represents a node, then append NodeUpdate (value) to the list of updates
      if (rowType == 0) {
        try {
          EdgeUpdateView update2 = om.treeToValue(value, EdgeUpdateView.class);
          foundUpdates.add(update2);
        } catch(IOException e) {
          throw new GraphModelException("Failed to decode EdgeUpdateView. Raw text was: "+value, e);
        }
      } else {
        throw new GraphModelException("Unexpected row type: "+rowType+". Key text was: "+row.getKey());
      }

      /*
       * Regardless of the row type, we should extract all other node identifiers from the row key (see 'otherEdgeUids')
       * Find out if we've encountered them before, and perform a recursive query for each 'new' identifier if we
       * haven't previously seen them.
       */
      for (JsonNode uidJsonNode :otherEdgeUids) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getUids().contains(uidJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(typeName, uidJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
    }
  }

}
