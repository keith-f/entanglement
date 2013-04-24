/*
 * Copyright 2012 Keith Flanagan
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
 * File created: 15-Nov-2012, 16:43:38
 */

package com.entanglementgraph.graph;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.entanglementgraph.util.MongoUtils.list;

/**
 *
 * @author Keith Flanagan
 */
public class EdgeDAOSeparateDocImpl
    extends AbstractGraphEntityDAO
    implements EdgeDAO {
  private static final Logger logger = Logger.getLogger(EdgeDAOSeparateDocImpl.class.getName());

  /*
   * Indexes
   */
  private static Iterable<DBObject> buildFromIndexes() {
    return EntityKeys.buildKeyIndexes(FIELD_FROM_KEYS);
  }

  private static Iterable<DBObject> buildToIndexes() {
    return EntityKeys.buildKeyIndexes(FIELD_TO_KEYS);
  }


  private final DBCollection nodeCol;


  public EdgeDAOSeparateDocImpl(ClassLoader classLoader, Mongo m, DB db,
                                DBCollection nodeCol, DBCollection edgeCol) {
    super(classLoader, m, db, edgeCol);

    this.nodeCol = nodeCol;

    //Create indexes
    MongoUtils.createIndexes(edgeCol, buildFromIndexes());
    MongoUtils.createIndexes(edgeCol, buildToIndexes());
  }

  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(EntityKeys from, EntityKeys to)
      throws GraphModelException {
    DBObject query = buildFromToNodeQuery(from, to);
    logger.log(Level.INFO, "Iterating edges between nodes: {0} --> {1}.\nQuery: {2}", new Object[]{from, to, query});
    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }

  /**
   * Graph entities can be uniquely identified either by one of their UID strings, or by their type combined with one
   * of their names. Edge queries involving both the 'from' and 'to' nodes must therefore perform several database
   * queries in order to ensure that all appropriate edge documents are found. These are:
   * <ul>
   *   <li>from (uid) + to (uid)</li>
   *   <li>from (uid) + to (type+name)</li>
   *   <li>from (type+name) + to (uid)</li>
   *   <li>from (type+name) + to (type+name)</li>
   * </ul>
   *
   * Helpfully, MongoDB provides the <code>$or</code> operator. Not only does this operator get around the usual
   * limit of one index per query, but each part of the 'or' operation executes in parallel.
   *
   * This method constructs a query along the lines of the following, which can be used to query the edges between two
   * nodes with all possible graph entity addressing combinations:
   * <pre>
   * $or: [ { from.uids: AAA, to.uids: BBB }, { from.names: YYY, from.type: ZZZ, to.names: WWW, to.type : XXX }, ..., ... ]
   * </pre>
   *
   * @return
   */
  private DBObject buildFromToNodeQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);

    or.add(buildFromUidToUidQuery(from, to));
    or.add(buildFromUidToNameQuery(from, to));
    or.add(buildFromNameToUidQuery(from, to));
    or.add(buildFromNameToNameQuery(from, to));

    return query;
  }

  /**
   * There are two ways to address a graph entity - by UID or by type+name. This method builds a query that returns
   * edges starting at a specified <code>from</code> node, using either identification method. MongoDB executes
   * both parts of this $or query in parallel.
   *
   * @param from
   * @return
   */
  private DBObject buildFromNodeQuery(EntityKeys from) {
    DBObject query = new BasicDBObject();

    DBObject uidQuery = new BasicDBObject();
    uidQuery.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));

    DBObject nameQuery = new BasicDBObject();
    nameQuery.put(FIELD_FROM_KEYS_NAMES, new BasicDBObject("$in", list(from.getNames())));
    nameQuery.put(FIELD_FROM_KEYS_TYPE, from.getType());

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);
    or.add(uidQuery);
    or.add(nameQuery);

    return query;
  }

  /**
   * There are two ways to address a graph entity - by UID or by type+name. This method builds a query that returns
   * edges ending at a specified <code>to</code> node, using either identification method. MongoDB executes
   * both parts of this $or query in parallel.
   *
   * @param to
   * @return
   */
  private DBObject buildToNodeQuery(EntityKeys to) {
    DBObject query = new BasicDBObject();

    DBObject uidQuery = new BasicDBObject();
    uidQuery.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));

    DBObject nameQuery = new BasicDBObject();
    nameQuery.put(FIELD_TO_KEYS_NAMES, new BasicDBObject("$in", list(to.getNames())));
    nameQuery.put(FIELD_TO_KEYS_TYPE, to.getType());

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);
    or.add(uidQuery);
    or.add(nameQuery);

    return query;
  }

  private DBObject buildFromUidToUidQuery(EntityKeys from, EntityKeys to)  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));
    return query;
  }

  private DBObject buildFromUidToNameQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
    query.put(FIELD_TO_KEYS_NAMES, new BasicDBObject("$in", list(to.getNames())));
    query.put(FIELD_TO_KEYS_TYPE, to.getType());
    return query;
  }

  private DBObject buildFromNameToUidQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_NAMES, new BasicDBObject("$in", list(from.getNames())));
    query.put(FIELD_FROM_KEYS_TYPE, from.getType());
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));
    return query;
  }

  private DBObject buildFromNameToNameQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_NAMES, new BasicDBObject("$in", list(from.getNames())));
    query.put(FIELD_FROM_KEYS_TYPE, from.getType());
    query.put(FIELD_TO_KEYS_NAMES, new BasicDBObject("$in", list(to.getNames())));
    query.put(FIELD_TO_KEYS_TYPE, to.getType());
    return query;
  }


  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(
      String edgeType, EntityKeys from, EntityKeys to)
      throws GraphModelException {
    // Build a query to find edges between nodes, regardless of whether UID or type+name is used.
    DBObject query = buildFromToNodeQuery(from, to);
    // Also limit by edge type.
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.INFO, "Iterating edges of type {0} between nodes: {1} --> {2}.\nQuery: {3}",
        new Object[]{edgeType, from, to, query});
    try {
      final DBCursor cursor = col.find(query);

      return cursor;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Iterable<DBObject> iterateEdgesFromNode(EntityKeys from)
      throws GraphModelException {
    logger.log(Level.INFO, "Iterating edges starting from node: {0}", new Object[]{from});
    DBObject query = buildFromNodeQuery(from);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});
    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  public Iterable<DBObject> iterateEdgesFromNode(String edgeType, EntityKeys from)
      throws GraphModelException {
    logger.log(Level.INFO, "Iterating edges of type: {0} starting from node: {1}", new Object[]{edgeType, from});
    DBObject query = buildFromNodeQuery(from);
    //Add a restriction on the type of edge returned
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});
    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }


  @Override
  public Iterable<DBObject> iterateEdgesToNode(EntityKeys to)
      throws GraphModelException {
    logger.log(Level.INFO, "Iterating edges ending at node: {0}", new Object[]{to});
    DBObject query = buildToNodeQuery(to);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});

    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public boolean existsEdgeToNodeOfType(EntityKeys from, String toNodeType)
      throws GraphModelException {
    logger.log(Level.INFO, "Finding edges from node: {0}, to any node of type {1}", new Object[]{from, toNodeType});
    DBObject query = buildFromNodeQuery(from);
    //Add a restriction on 'to' node type
    query.put(FIELD_TO_KEYS_TYPE, toNodeType);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});

//    DBObject query = new BasicDBObject();
//    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
//    query.put(FIELD_TO_KEYS_TYPE, toNodeType);
    try {
      return col.find(query).limit(1).hasNext();
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n" + "Query: " + query, e);
    }
  }


  @Override
  public Long countEdgesFromNode(EntityKeys from)
      throws GraphModelException {
//    DBObject query = new BasicDBObject();
//    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
    DBObject query = buildFromNodeQuery(from);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});
    try {
      long count = col.count(query);
      return count;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n" + "Query: " + query, e);
    }
  }


  @Override
  public Long countEdgesOfTypeFromNode(String edgeType, EntityKeys from)
      throws GraphModelException {
//    DBObject query = new BasicDBObject();
//    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
//    query.put(FIELD_KEYS_TYPE, edgeType);

    DBObject query = buildFromNodeQuery(from);
    //Add a restriction on the edge type
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});

    try {
      long count = col.count(query);
      return count;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Long countEdgesToNode(EntityKeys to)
      throws GraphModelException {
    DBObject query = buildToNodeQuery(to);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});

    try {
      long count = col.count(query);
      return count;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Long countEdgesOfTypeToNode(String edgeType, EntityKeys to)
      throws GraphModelException {
    DBObject query = buildToNodeQuery(to);
    //Add a restriction on the edge type
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});

    try {
      long count = col.count(query);
      return count;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Long countEdgesOfTypeBetweenNodes(
      String edgeType, EntityKeys from, EntityKeys to)
      throws GraphModelException {
//    DBObject query = new BasicDBObject();
//    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
//    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));
//    query.put(FIELD_KEYS_TYPE, edgeType);

    DBObject query = buildFromToNodeQuery(from, to);
    //Add a restriction on the edge type
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});

    try {
      long count = col.count(query);
      return count;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Map<String, Long> countEdgesByTypeFromNode(EntityKeys from)
      throws GraphModelException {
//    DBObject query = new BasicDBObject();
//    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));

    DBObject query = buildFromNodeQuery(from);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});


    DBObject fields = new BasicDBObject();
    fields.put(FIELD_KEYS_TYPE, 1);
    try {
      Map<DBObject, Long> counts = count(col.find(query, fields));
      Map<String, Long> edgeTypeToCount = new HashMap<>();

      for (Map.Entry<DBObject, Long> entry : counts.entrySet()) {
        edgeTypeToCount.put((String) entry.getKey().get(FIELD_KEYS_TYPE), entry.getValue());
      }

      return edgeTypeToCount;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }

  private static <T> Map<T, Long> count(Iterable<T> items) {
    Map<T, Long> counts = new HashMap<>();
    for (T item : items) {
      Long c = counts.get(item);
      if (c == null) {
        c = 0l;
      }
      counts.put(item, c + 1);
    }
    return counts;
  }


  @Override
  public Map<String, Long> countEdgesByTypeToNode(EntityKeys to)
      throws GraphModelException {
//    DBObject query = new BasicDBObject();
//    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));
    DBObject query = buildToNodeQuery(to);
    logger.log(Level.INFO, "Query: {0}", new Object[]{query});


    DBObject fields = new BasicDBObject();
    fields.put(FIELD_KEYS_TYPE, 1);
    try {
      Map<DBObject, Long> counts = count(col.find(query, fields));
      Map<String, Long> edgeTypeToCount = new HashMap<>();

      for (Map.Entry<DBObject, Long> entry : counts.entrySet()) {
        edgeTypeToCount.put((String) entry.getKey().get(FIELD_KEYS_TYPE), entry.getValue());
      }

      return edgeTypeToCount;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }

}
