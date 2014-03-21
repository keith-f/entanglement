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

package com.entanglementgraph.graph.updateview;

import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.couchdb.EdgeUpdateView;
import com.entanglementgraph.graph.couchdb.NodeUpdateView;
import com.entanglementgraph.graph.couchdb.ViewQueryFactory;
import com.entanglementgraph.graph.couchdb.viewparsers.NodesAndEdgesViewRowParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An implementation of <code>UpdateStream</code> that returns a set of updates by key <i>set</i>.
 *
 * For keys of type: <code>{type, [UID, ...] }</code>, returns a stream of updates for the specified keys
 * <i>as well as</i> any additional keys found along the way. i.e., this implementation of <code>UpdateStream</code>
 * performs key integration on the UIDs. If a partial keyset is specified (i.e., the user requests fewer keys than the total set that the entity is
 * known by), then the extra key names are both found and updates for them are also returned.
 *
 * @author Keith Flanagan
 */
public class IntegratedKeysetUpdateStream implements UpdateStream {

  private final CouchDbConnector db;
  private final ObjectMapper om;

  public IntegratedKeysetUpdateStream(CouchDbConnector db, ObjectMapper om) {
    this.db = db;
    this.om = om;
  }

  /**
   * Query for the set of updates for a specified node. Recursively query if 'new' UIDs are found.
   *
   * @param typeName
   * @param identifier
   * @param fullKeyset
   * @param foundUpdates
   * @throws GraphModelException
   */
  private void findAllIdentifiersAndUpdatesFor(String typeName, String identifier,
                                               EntityKeys fullKeyset, List<NodeUpdateView> foundUpdates)
      throws GraphModelException {
    ViewQuery query = ViewQueryFactory.createNodesAndEdgesQuery(db);

    fullKeyset.addUid(identifier);
    query = query
        .startKey(ComplexKey.of(typeName, identifier))
        .endKey(ComplexKey.of(typeName, identifier, ComplexKey.emptyObject()));

    ViewResult result = db.queryView(query);
    for (ViewResult.Row row : result.getRows()) {
      NodesAndEdgesViewRowParser parsedRow = new NodesAndEdgesViewRowParser(row);
      final int rowType = parsedRow.getRowType();
      final JsonNode otherNodeUids = parsedRow.getOtherNodeUids();

      JsonNode value = row.getValueAsNode();

      // If this result row represents a node, then append NodeUpdate (value) to the list of updates
      if (rowType == NodesAndEdgesViewRowParser.RowType.NODE.getDbTypeIdx()) {
        try {
          NodeUpdateView update = om.treeToValue(value, NodeUpdateView.class);
          foundUpdates.add(update);
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

      // Extract the 'nodeUids' from the value. This should be a list of strings.
      // Check to see if any of these identifiers are 'new' to us.
      for (JsonNode uidJsonNode :otherNodeUids) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getUids().contains(uidJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(typeName, uidJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
    }
  }

  /**
   * Query for the set of updates for a specified node. Recursively query if 'new' UIDs are found.
   * Also filter by graph name.
   *
   * @param typeName
   * @param identifier
   * @param fullKeyset
   * @param foundUpdates
   * @throws GraphModelException
   */
  private void findAllIdentifiersAndUpdatesFor(String typeName, String identifier, Set<String> includeGraphs,
                                               EntityKeys fullKeyset, List<NodeUpdateView> foundUpdates)
      throws GraphModelException {
    ViewQuery query = ViewQueryFactory.createNodesAndEdgesQuery(db);

    fullKeyset.addUid(identifier);
    query = query
        .startKey(ComplexKey.of(typeName, identifier))
        .endKey(ComplexKey.of(typeName, identifier, ComplexKey.emptyObject()));

    ViewResult result = db.queryView(query);
    for (ViewResult.Row row : result.getRows()) {
      NodesAndEdgesViewRowParser parsedRow = new NodesAndEdgesViewRowParser(row);
      final int rowType = parsedRow.getRowType();
      final JsonNode otherNodeUids = parsedRow.getOtherNodeUids();

      JsonNode value = row.getValueAsNode();

      // If this result row represents a node, then append NodeUpdate (value) to the list of updates
      if (rowType == NodesAndEdgesViewRowParser.RowType.NODE.getDbTypeIdx()) {
        try {
          NodeUpdateView update = om.treeToValue(value, NodeUpdateView.class);
          if (!includeGraphs.contains(update.getGraphUid())) {
            continue;
          }
          foundUpdates.add(update);
        } catch(IOException e) {
          throw new GraphModelException("Failed to decode NodeUpdateView. Raw text was: "+value, e);
        }
      } else {
        try {
          EdgeUpdateView update = om.treeToValue(value, EdgeUpdateView.class);
          if (!includeGraphs.contains(update.getGraphUid())) {
            continue;
          }
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

      // Extract the 'nodeUids' from the value. This should be a list of strings.
      // Check to see if any of these identifiers are 'new' to us.
      for (JsonNode uidJsonNode :otherNodeUids) {
        // If this is the first time we've encountered the identifier, perform another query
        if (!fullKeyset.getUids().contains(uidJsonNode.asText())) {
          findAllIdentifiersAndUpdatesFor(typeName, uidJsonNode.asText(), fullKeyset, foundUpdates);
        }
      }
    }
  }


  @Override
  public List<NodeUpdateView> findNodeUpdatesForKey(EntityKeys keys) throws GraphModelException {
    EntityKeys fullKeyset = new EntityKeys(); // Keeps track of which identifiers we've already queried for.
    List<NodeUpdateView> allUpdates = new ArrayList<>();    //List of update documents found so far.

    for (String uid : keys.getUids()) {
      findAllIdentifiersAndUpdatesFor(keys.getType(), uid, fullKeyset, allUpdates);
    }

    Collections.sort(allUpdates, NodeUpdateView.TIMESTAMP_COMPARATOR);
    return allUpdates;
  }

  @Override
  public List<NodeUpdateView> findNodeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames) {
    return null;
  }

  @Override
  public List<EdgeUpdateView> findEdgeUpdatesForKey(EntityKeys keys) {
    return null;
  }

  @Override
  public List<EdgeUpdateView> findEdgeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames) {
    return null;
  }
}
