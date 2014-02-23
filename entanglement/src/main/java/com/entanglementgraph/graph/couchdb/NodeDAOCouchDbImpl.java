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
import com.entanglementgraph.graph.couchdb.viewparsers.NodesAndEdgesViewRowParser;
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
//    @View(name = "nameToNames", map = "classpath:nodeNameToNamesMap.js"),
//    @View(name = "uidToUids", map = "classpath:nodeUidToUidsMap.js"),
//    @View(name = "all_nodes_by_name", map = "classpath:nodesByName.js"),
//    @View(name = "all_nodes_by_uid", map = "classpath:nodesByUid.js"),
    @View(name = "nodes_and_edges", map = "classpath:nodesAndEdgesMap.js") //, reduce = "classpath:nodesAndEdgesReduce.js")
})
public class NodeDAOCouchDbImpl<C extends Content> extends CouchDbRepositorySupport<Node> implements NodeDAO<C> {
  private static final Logger logger = Logger.getLogger(NodeDAOCouchDbImpl.class.getSimpleName());

  public static final String DESIGN_DOC_ID = "_design/"+Node.class.getSimpleName();


  public static class NodeLookupResult {
    private final EntityKeys queryKeyset;
    private final EntityKeys fullKeyset;
    private final List<NodeUpdateView> allUpdates;

    private NodeLookupResult(EntityKeys queryKeyset, EntityKeys fullKeyset, List<NodeUpdateView> allUpdates) {
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

  private static class NodeModificationViewByTimestampComparator implements Comparator<NodeUpdateView> {
    @Override
    public int compare(NodeUpdateView o1, NodeUpdateView o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
  }


  private final ObjectMapper om;

  public NodeDAOCouchDbImpl(CouchDbConnector db, ObjectMapper om)  {
    super(Node.class, db);
//    CouchDbRepositorySupport support = new CouchDbRepositorySupport(null, null, null);

    initStandardDesignDocument(); // Causes Ektorp to create the views listed above
    this.om = om;
  }


  public EntityKeys<C> populateFullKeyset(EntityKeys<C> partial) throws GraphModelException {
    return findAllIdentifiersAndUpdatesFor(partial).getFullKeyset();
  }

  @Override
  public Node<C> getByKey(EntityKeys<C> partialOrFullKeyset) throws GraphModelException {
    NodeLookupResult result = findAllIdentifiersAndUpdatesFor(partialOrFullKeyset);

    List<NodeUpdateView> updates = result.getAllUpdates();
//    logger.info("Found updates "+updates.size()+" for query keyset: "+partialOrFullKeyset);

    // Create a 'virtual' node if there's no database entry.
    // Otherwise, find all revisions and construct a 'merged' node.
    Node<C> node;
    if (updates.isEmpty()) {
      node = new Node<>();
      node.setKeys(partialOrFullKeyset);
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
    IteratorForStreamingAllNodes itr = new IteratorForStreamingAllNodes(db, this);
    return itr;
  }

  @Override
  public Iterable<Node<C>> iterateByType(String typeName) throws GraphModelException {
    /*
     * WARNING: this implementation is suitable only for small graphs.
     * Due to the nature of data integration, the number of graph elements is only known after iterating.
     * This method must keep track of all 'seen' node identifiers, and is therefore only suitable for datasets whose
     * identifiers fit into RAM.
     */
    IteratorForStreamingAllNodes itr = new IteratorForStreamingAllNodes(db, this, typeName);
    return itr;
  }




  /*
   * Internal methods
   */

  private <C extends Content> Node<C> mergeRevisions(List<NodeUpdateView> mods) throws GraphModelException {
    Collections.sort(mods, new NodeModificationViewByTimestampComparator());

    NodeMerger<C> merger = new NodeMerger<>();
    Node<C> merged = null;
    //for (NodeUpdateView<C> mod : mods) {
    for (NodeUpdateView mod : mods) {
      if (merged == null) {
        merged = mod.getNode();
      } else {
        Node<C> newNode = mod.getNode();
        merger.merge(mod.getMergePol(), merged, newNode);
      }
    }
    return merged;
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
  private NodeLookupResult findAllIdentifiersAndUpdatesFor(EntityKeys<C> queryKeyset)
      throws GraphModelException {
    EntityKeys fullKeyset = new EntityKeys(); // Keeps track of which identifiers we've already queried for.
    List<NodeUpdateView> foundUpdates = new ArrayList<>();    //List of update documents found so far.

    for (String uid : queryKeyset.getUids()) {
      findAllIdentifiersAndUpdatesFor(IdentifierType.UID, queryKeyset.getType(), uid, fullKeyset, foundUpdates);
    }
    for (String name : queryKeyset.getNames()) {
      findAllIdentifiersAndUpdatesFor(IdentifierType.NAME, queryKeyset.getType(), name, fullKeyset, foundUpdates);
    }


    NodeLookupResult result = new NodeLookupResult(queryKeyset, fullKeyset, foundUpdates);
    return result;
  }

  private void findAllIdentifiersAndUpdatesFor(IdentifierType keyType, String typeName, String identifier,
                                               EntityKeys fullKeyset, List<NodeUpdateView> foundUpdates)
      throws GraphModelException {
    ViewQuery query = ViewQueryFactory.createNodesAndEdgesQuery(db);
    switch (keyType) {
      case NAME:
        fullKeyset.addName(identifier);
        query = query
            .startKey(ComplexKey.of(typeName, IdentifierType.NAME.getDbString(), identifier))
            .endKey(ComplexKey.of(typeName, IdentifierType.NAME.getDbString(), identifier, ComplexKey.emptyObject()));
        break;
      case UID:
        fullKeyset.addUid(identifier);
        query = query
            .startKey(ComplexKey.of(typeName, IdentifierType.UID.getDbString(), identifier))
            .endKey(ComplexKey.of(typeName, IdentifierType.UID.getDbString(), identifier, ComplexKey.emptyObject()));
        break;
      default:
        throw new GraphModelException("Unsupported identifier type: "+keyType);
    }
//    ViewResult result = db.queryView(query.reduce(true).group(true).groupLevel(4));
    ViewResult result = db.queryView(query);
    //Read each row
    for (ViewResult.Row row : result.getRows()) {
      NodesAndEdgesViewRowParser parsedRow = new NodesAndEdgesViewRowParser(row);
      final int rowType = parsedRow.getRowType();
      final JsonNode otherNodeUids = parsedRow.getOtherNodeUids();
      final JsonNode otherNodeNames = parsedRow.getOtherNodeNames();
//      Iterator<JsonNode> keyItr = row.getKeyAsNode().iterator();
//      String nodeTypeName = keyItr.next().asText(); // Eg: "Gene". Should be equal to <code>typeName</code>
//      String uidOrName = keyItr.next().asText();    // Either 'U' or 'N'
//      String identifier2 = keyItr.next().asText();  // Should be equal to <code>identifier</code>
//      int rowType =  keyItr.next().asInt();         //0=node; 1=edgeFrom; ...
//      JsonNode otherNodeUids = keyItr.next();       // (some) of the other UIDs this node is known by
//      JsonNode otherNodeNames = keyItr.next();       // (some) of the other names this node is known by

      JsonNode value = row.getValueAsNode();

      // If this result row represents a node, then append NodeUpdate (value) to the list of updates
      if (rowType == NodesAndEdgesViewRowParser.RowType.NODE.getDbTypeIdx()) {
        try {
          NodeUpdateView update2 = om.treeToValue(value, NodeUpdateView.class);
          foundUpdates.add(update2);
        } catch(IOException e) {
          throw new GraphModelException("Failed to decode NodeUpdateView. Raw text was: "+value, e);
        }
      }

      /*
       * Regardless of the row type, we should extract all other node identifiers from the row key (see 'otherNodeUids'
       * and 'otherNodeNames').
       * Find out if we've encountered them before, and perform a recursive query for each 'new' identifier if we
       * haven't previously seen them.
       */

      // Extract the 'nodeNames' from the value. This should be a list of strings.
      // Check to see if any of these names are 'new' to us.
      for (JsonNode nameJsonNode : otherNodeNames) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getNames().contains(nameJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(IdentifierType.NAME, typeName, nameJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
      for (JsonNode uidJsonNode :otherNodeUids) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getUids().contains(uidJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(IdentifierType.UID, typeName, uidJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
    }
  }

}
