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

package com.entanglementgraph.jiti;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.jiti.NodeMerger;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * An implementation of NodeDAO that executes queries one or more specified graphs (potentially in parallel). This
 * class should be useful in situations where you need to execute the same query over multiple graphs simultaneously.
 * One example might be to produce an integrated 'view' over multiple datasets.
 *
 * Note that there are a few differences between this NodeDAO implementation and a typical implementation:
 * <ul>
 *   <li>This implementation is read-only. All write operations will fail with an UnsupportedOperationException.</li>
 *   <li>The count() operation returns the sum of the graph element documents in all of the delegate graphs. This number
 *   may be greater than the total number of expected nodes. For example, if two documents exist with the same ID, but
 *   are located in two different graphs, then count() would return '2' even though iterating over the integrated
 *   view would result in one node.</li>
 *   <li>If you pass a custom query to one of the methods that supports such queries, then any MongoDB 'sort' operation
 *   will have undefined results. It is infeasible to perform a sort operation over potentially huge result sets from
 *   multiple different graphs in our current implementation.</li>
 *   <li>In cases where a query requires two or more documents from different graphs to be merged, then those documents
 *   will be merged according to the specified <code>MergePolicy</code>, and the priority ordering of the source graphs.
 *   </li>
 * </ul>
 * Note that this implementation of NodeDAO is read-only. All write operations will fail with an UnsupportedOperationException.
 */
public class ParallelNodeDAO implements NodeDAO {
  private static final MergePolicy DEFAULT_MERGE_POLICY = MergePolicy.APPEND_NEW__OVERWRITE_EXSITING;

  private final NodeMerger nodeMerger;
  private final ScheduledThreadPoolExecutor exe;
  private final SortedSet<GraphConnection> graphs;
  private MergePolicy mergePolicy;

  public ParallelNodeDAO(DbObjectMarshaller marshaller) {
    nodeMerger = new NodeMerger(marshaller);
    exe = new ScheduledThreadPoolExecutor(1);
    this.graphs = new TreeSet<>();
    this.mergePolicy = DEFAULT_MERGE_POLICY;
  }

  @Override
  public DBCollection getCollection() {
    throw new UnsupportedOperationException("Attempt to call getCollection on a meta-DAO. This method doesn't make" +
        "sense in a multi-graph environment.");
  }

  @Override
  public void store(BasicDBObject entity) throws GraphModelException {
    throw new UnsupportedOperationException("This DAO implementation is read-only.");
  }

  @Override
  public void update(BasicDBObject updated) throws GraphModelException {
    throw new UnsupportedOperationException("This DAO implementation is read-only.");
  }

  @Override
  public void setPropertyByUid(String uid, String propertyName, Object propertyValue) throws GraphModelException {
    throw new UnsupportedOperationException("This DAO implementation is read-only.");
  }

  @Override
  public void setPropertyByName(String entityType, String entityName, String propertyName, Object propertyValue) throws GraphModelException {
    throw new UnsupportedOperationException("This DAO implementation is read-only.");
  }

  private EntityKeys mergeKeyset(Map<GraphConnection, EntityKeys> results) throws GraphModelException {
    EntityKeys merged = null;
    for (GraphConnection conn : graphs) {
      EntityKeys result = results.get(conn);
      if (result == null) {
        // No result from this graph
        continue;
      }
      if (merged == null) {
        //First result populates 'merged'
        merged = result;
      } else {
        //Further results need to be merged into the existing result.
        if (!merged.getType().equals(result.getType())) {
          throw new GraphModelException("Attempt to merge EntityKeys of type "+result.getType()
              +" with a different type: "+merged.getType());
        }
        merged.addNames(result.getNames());
        merged.addUids(result.getUids());
      }
    }
    return merged;
  }

  private BasicDBObject mergeDBObjects(Map<GraphConnection, BasicDBObject> results) throws GraphModelException {
    BasicDBObject merged = null;
    for (GraphConnection conn : graphs) {
      BasicDBObject result = results.get(conn);
      if (result == null) {
        // No result from this graph
        continue;
      }
      if (merged == null) {
        //First result populates 'merged'
        merged = result;
      } else {
        //Further results need to be merged into the existing result.
        merged = nodeMerger.mergeNodes(mergePolicy, merged, result);
      }
    }
    return merged;
  }

  @Override
  public EntityKeys getEntityKeysetForUid(String uid) throws GraphModelException {
    Map<GraphConnection, EntityKeys> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getEntityKeysetForUid(uid));
    }

    EntityKeys merged = mergeKeyset(results);
    return merged;
  }

  @Override
  public EntityKeys getEntityKeysetForName(String type, String name) throws GraphModelException {
    Map<GraphConnection, EntityKeys> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getEntityKeysetForName(type, name));
    }

    EntityKeys merged = mergeKeyset(results);
    return merged;
  }

  @Override
  public BasicDBObject getByKey(EntityKeys keyset) throws GraphModelException {
    Map<GraphConnection, BasicDBObject> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getByKey(keyset));
    }

    BasicDBObject merged = mergeDBObjects(results);
    return merged;
  }

  @Override
  public BasicDBObject getByUid(String uid) throws GraphModelException {
    Map<GraphConnection, BasicDBObject> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getByUid(uid));
    }

    BasicDBObject merged = mergeDBObjects(results);
    return merged;
  }

  @Override
  public BasicDBObject getByAnyUid(Set<String> uids) throws GraphModelException {
    Map<GraphConnection, BasicDBObject> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getByAnyUid(uids));
    }

    BasicDBObject merged = mergeDBObjects(results);
    return merged;
  }

  @Override
  public BasicDBObject getByName(String entityType, String entityName) throws GraphModelException {
    Map<GraphConnection, BasicDBObject> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getByName(entityType, entityName));
    }

    BasicDBObject merged = mergeDBObjects(results);
    return merged;
  }

  @Override
  public BasicDBObject getByAnyName(String type, Set<String> entityNames) throws GraphModelException {
    Map<GraphConnection, BasicDBObject> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().getByAnyName(type, entityNames));
    }

    BasicDBObject merged = mergeDBObjects(results);
    return merged;
  }

  @Override
  public boolean existsByKey(EntityKeys keyset) throws GraphModelException {
    Map<GraphConnection, Boolean> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().existsByKey(keyset));
    }

    for (boolean result : results.values()) {
      if (result) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean existsByUid(String uniqueId) throws GraphModelException {
    Map<GraphConnection, Boolean> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().existsByUid(uniqueId));
    }

    for (boolean result : results.values()) {
      if (result) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean existsByAnyUid(Collection<String> entityUids) throws GraphModelException {
    Map<GraphConnection, Boolean> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().existsByAnyUid(entityUids));
    }

    for (boolean result : results.values()) {
      if (result) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean existsByName(String entityType, String entityName) throws GraphModelException {
    Map<GraphConnection, Boolean> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().existsByName(entityType, entityName));
    }

    for (boolean result : results.values()) {
      if (result) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean existsByAnyName(String entityType, Collection<String> entityNames) throws GraphModelException {
    Map<GraphConnection, Boolean> results = new HashMap<>(graphs.size());
    // TODO parallelise this loop
    for (GraphConnection conn : graphs) {
      results.put(conn, conn.getNodeDao().existsByAnyName(entityType, entityNames));
    }

    for (boolean result : results.values()) {
      if (result) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void delete(EntityKeys keys) throws GraphModelException {
    throw new UnsupportedOperationException("This DAO implementation is read-only.");
  }

  @Override
  public DBCursor iterateAll() throws GraphModelException {
    DBCursor c;
//    DBCursor c2= new DBCursor();

    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<String> listTypes() throws GraphModelException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Iterable<EntityKeys> iterateKeys(int offset, int limit) throws GraphModelException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Iterable<DBObject> iterateByType(String typeName) throws GraphModelException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Iterable<DBObject> iterateByType(String typeName, Integer offset, Integer limit, DBObject customQuery, DBObject sort) throws GraphModelException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Iterable<EntityKeys> iterateKeysByType(String typeName, int offset, int limit) throws GraphModelException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public long countByType(String typeName) throws GraphModelException {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public long count() throws GraphModelException {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
