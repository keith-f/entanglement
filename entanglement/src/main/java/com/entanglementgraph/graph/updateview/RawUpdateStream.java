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
import com.entanglementgraph.graph.couchdb.*;
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
 * An implementation of <code>UpdateStream</code> that returns a set of updates by key.
 *
 * For keys of type: <code>{type, [UID, ...] }</code>, returns a stream of updates <i>for the specified keys only</i>.
 * If a partial keyset is specified (i.e., the user requests fewer keys than the total set that the entity is
 * known by), then the extra key names are returned, but no additional updates are returned.
 *
 * No integration is performed at this stage. The methods defined here only return a set of raw updates from
 * the database. Updates may be filtered by graph name(s) if necessary.
 *
 * @author Keith Flanagan
 */
public class RawUpdateStream implements UpdateStream {

  private final CouchDbConnector db;
  private final ObjectMapper om;

  public RawUpdateStream(CouchDbConnector db, ObjectMapper om) {
    this.db = db;
    this.om = om;
  }

  private List<NodeUpdateView> queryNodeByKey(String type, String identifier) throws GraphModelException {
    ViewQuery query = ViewQueryFactory.createNodesAndEdgesQuery(db)
        .startKey(ComplexKey.of(type, identifier, NodesAndEdgesViewRowParser.RowType.NODE.getDbTypeIdx()))
        .endKey(ComplexKey.of(type, identifier, NodesAndEdgesViewRowParser.RowType.NODE.getDbTypeIdx(), ComplexKey.emptyObject()));

    ViewResult result = db.queryView(query);
    List<NodeUpdateView> foundUpdates = new ArrayList<>();
    for (ViewResult.Row row : result.getRows()) {
      JsonNode value = row.getValueAsNode();
      try {
        NodeUpdateView update = om.treeToValue(value, NodeUpdateView.class);
        foundUpdates.add(update);
      } catch(IOException e) {
        throw new GraphModelException("Failed to decode NodeUpdateView. Raw text was: "+value, e);
      }
    }

    return foundUpdates;
  }

  private List<EdgeUpdateView> queryEdgeByKey(String type, String identifier) throws GraphModelException {
    ViewQuery query = ViewQueryFactory.createEdgesQuery(db)
        .startKey(ComplexKey.of(type, identifier))
        .endKey(ComplexKey.of(type, identifier, ComplexKey.emptyObject()));

    ViewResult result = db.queryView(query);
    List<EdgeUpdateView> foundUpdates = new ArrayList<>();
    for (ViewResult.Row row : result.getRows()) {
      JsonNode value = row.getValueAsNode();
      try {
        EdgeUpdateView update = om.treeToValue(value, EdgeUpdateView.class);
        foundUpdates.add(update);
      } catch(IOException e) {
        throw new GraphModelException("Failed to decode NodeUpdateView. Raw text was: "+value, e);
      }
    }

    return foundUpdates;
  }

  private <U extends UpdateView> List<U> filterDuplicates(List<U> allUpdates) {
    // Sort updates by timestamp, document patch set, then index within the patchset.
    Collections.sort(allUpdates, NodeUpdateView.TIMESTAMP_PATCH_COMPARATOR);

    List<U> filtered = new ArrayList<>();
    U last = null;
    for (U update : allUpdates) {
      if (last != null
          && last.getPatchUid().equals(update.getPatchUid())
          && last.getUpdateIdx() == update.getUpdateIdx()) {
        continue;
      } else {
        filtered.add(update);
      }
      last = update;
    }
    return filtered;
  }

  @Override
  public List<NodeUpdateView> findNodeUpdatesForKey(EntityKeys keys) throws GraphModelException {
    List<NodeUpdateView> allUpdates = new ArrayList<>();
    for (String identifier : keys.getUids()) {
      allUpdates.addAll(queryNodeByKey(keys.getType(), identifier));
    }

    // Now filter out duplicate updates
    return filterDuplicates(allUpdates);
  }

  @Override
  public List<NodeUpdateView> findNodeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames) throws GraphModelException {
    List<NodeUpdateView> allUpdates = new ArrayList<>();
    for (String identifier : keys.getUids()) {
      for (NodeUpdateView update : queryNodeByKey(keys.getType(), identifier)) {
        if (graphNames.contains(update.getGraphUid())) {
          allUpdates.add(update);
        }
      }
    }

    return filterDuplicates(allUpdates);
  }

  @Override
  public List<EdgeUpdateView> findEdgeUpdatesForKey(EntityKeys keys) throws GraphModelException {
    List<EdgeUpdateView> allUpdates = new ArrayList<>();
    for (String identifier : keys.getUids()) {
      allUpdates.addAll(queryEdgeByKey(keys.getType(), identifier));
    }
    return filterDuplicates(allUpdates);
  }

  @Override
  public List<EdgeUpdateView> findEdgeUpdatesForKeyInGraphs(EntityKeys keys, Set<String> graphNames) throws GraphModelException {
    List<EdgeUpdateView> allUpdates = new ArrayList<>();
    for (String identifier : keys.getUids()) {
      for (EdgeUpdateView update : queryEdgeByKey(keys.getType(), identifier)) {
        if (graphNames.contains(update.getGraphUid())) {
          allUpdates.add(update);
        }
      }
    }

    return filterDuplicates(allUpdates);
  }
}
