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
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.entanglementgraph.graph.EdgeQueries.*;

/**
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


  public EdgeDAOSeparateDocImpl(ClassLoader classLoader, Mongo m, DB db,
                                DBCollection edgeCol) {
    super(classLoader, m, db, edgeCol);

    //Create indexes
    MongoUtils.createIndexes(edgeCol, buildFromIndexes());
    MongoUtils.createIndexes(edgeCol, buildToIndexes());
  }

  @Override
  public DBCursor iterateEdgesBetweenNodes(EntityKeys from, EntityKeys to)
      throws GraphModelException {
    DBObject query = buildBidirectionalFromToNodeQuery(from, to);
    logger.log(Level.FINE, "Iterating edges between nodes: {0} --> {1}.\nQuery: {2}", new Object[]{from, to, query});
    try {
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public DBCursor iterateEdgesBetweenNodes(String edgeType, EntityKeys from, EntityKeys to)
      throws GraphModelException {
    // Build a query to find edges between nodes, regardless of whether UID or type+name is used.
    DBObject query = buildBidirectionalFromToNodeQuery(from, to);
    // Also limit by edge type.
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.FINE, "Iterating edges of type {0} between nodes: {1} --> {2}.\nQuery: {3}",
        new Object[]{edgeType, from, to, query});
    try {
      return col.find(query); // this method returns a DBCursor
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public DBCursor iterateEdgesFromNode(EntityKeys from)
      throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges starting from node: {0}", new Object[]{from});
    DBObject query = buildFromNodeQuery(from);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public DBCursor iterateEdgesFromNode(String edgeType, EntityKeys from)
      throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges of type: {0} starting from node: {1}", new Object[]{edgeType, from});
    DBObject query = buildFromNodeQuery(from);
    //Add a restriction on the type of edge returned
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public DBCursor iterateEdgesFromNode(String edgeType, EntityKeys<? extends Node> from,
                                                 Integer offset, Integer limit, DBObject customQuery, DBObject sort)
      throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges of type: {0} starting from node: {1}", new Object[]{edgeType, from});
    DBObject query = buildFromNodeQuery(from);
    //Add a restriction on the type of edge returned
    query.put(FIELD_KEYS_TYPE, edgeType);
    if (customQuery != null) {
      query.putAll(customQuery);
    }
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
      if (sort != null) {
        return col.find(query).skip(offset).limit(limit).sort(sort);
      } else {
        return col.find(query).skip(offset).limit(limit);
      }
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public DBCursor iterateEdgesFromNodeToNodeOfType(EntityKeys<? extends Node> from, String toNodeType) throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges from node: {0}, to node of type: {1}", new Object[]{from, toNodeType});
    DBObject query = buildFromNodeQuery(from);
    query.put(FIELD_TO_KEYS_TYPE, toNodeType);
//    query.putAll(customQuery);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
//      return col.find(query).skip(offset).limit(limit).sort(sort);
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public DBCursor iterateEdgesToNodeFromNodeOfType(EntityKeys<? extends Node> to, String fromNodeType) throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges to node: {0}, from a node of type: {1}", new Object[]{to, fromNodeType});
    DBObject query = buildToNodeQuery(to);
    query.put(FIELD_FROM_KEYS_TYPE, fromNodeType);
//    query.putAll(customQuery);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
//      return col.find(query).skip(offset).limit(limit).sort(sort);
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }


  @Override
  public DBCursor iterateEdgesToNode(EntityKeys to)
      throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges ending at node: {0}", new Object[]{to});
    DBObject query = buildToNodeQuery(to);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    try {
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public DBCursor iterateEdgesToNode(String edgeType, EntityKeys to)
      throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges of type {0} ending at node: {1}", new Object[]{edgeType, to});
    DBObject query = buildToNodeQuery(to);
    //Add a restriction on the type of edge returned
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    try {
      return col.find(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public DBCursor iterateEdgesToNode(String edgeType, EntityKeys<? extends Node> to, Integer offset, Integer limit, DBObject customQuery, DBObject sort) throws GraphModelException {
    logger.log(Level.FINE, "Iterating edges of type: {0} ending at node: {1}", new Object[]{edgeType, to});
    DBObject query = buildToNodeQuery(to);
    //Add a restriction on the type of edge returned
    query.put(FIELD_KEYS_TYPE, edgeType);
    if (customQuery != null) {
      query.putAll(customQuery);
    }
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
      if (sort != null) {
        return col.find(query).skip(offset).limit(limit).sort(sort);
      } else {
        return col.find(query).skip(offset).limit(limit);
      }
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: " + query, e);
    }
  }

  @Override
  public boolean existsEdgeToNodeOfType(EntityKeys from, String toNodeType)
      throws GraphModelException {
    logger.log(Level.FINE, "Finding edges from node: {0}, to any node of type {1}", new Object[]{from, toNodeType});
    DBObject query = buildFromNodeQuery(from);
    //Add a restriction on 'to' node type
    query.put(FIELD_TO_KEYS_TYPE, toNodeType);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

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
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});
    try {
      return col.count(query);
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
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    try {
      return col.count(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Long countEdgesToNode(EntityKeys to)
      throws GraphModelException {
    DBObject query = buildToNodeQuery(to);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    try {
      return col.count(query);
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
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    try {
      return col.count(query);
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

    DBObject query = buildBidirectionalFromToNodeQuery(from, to);
    //Add a restriction on the edge type
    query.put(FIELD_KEYS_TYPE, edgeType);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    try {
      return col.count(query);
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }


  @Override
  public Map<String, Long> countEdgesByTypeFromNode(EntityKeys from)
      throws GraphModelException {
    DBObject query = buildFromNodeQuery(from);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});


    DBObject fields = new BasicDBObject();
    fields.put(FIELD_KEYS_TYPE, 1);
    try (DBCursor cursor = col.find(query, fields)){
      Map<String, Long> edgeTypeToCount = new HashMap<>();
      for (DBObject edgeObj : cursor) {
        String nextType = (String) ((BasicDBObject) edgeObj.get("keys")).get("type");
        Long count = edgeTypeToCount.get(nextType);
        if (count == null) {
          count = 0l;
        }
        edgeTypeToCount.put(nextType, count + 1);
      }

      return edgeTypeToCount;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }



  @Override
  public Map<String, Long> countEdgesByTypeToNode(EntityKeys to)
      throws GraphModelException {
    DBObject query = buildToNodeQuery(to);
    logger.log(Level.FINE, "Query: {0}", new Object[]{query});

    DBObject fields = new BasicDBObject();
    fields.put(FIELD_KEYS_TYPE, 1);
    try (DBCursor cursor = col.find(query, fields)){
      Map<String, Long> edgeTypeToCount = new HashMap<>();
      for (DBObject edgeObj : cursor) {
        String nextType = (String) ((BasicDBObject) edgeObj.get("keys")).get("type");
        Long count = edgeTypeToCount.get(nextType);
        if (count == null) {
          count = 0l;
        }
        edgeTypeToCount.put(nextType, count + 1);
      }
      return edgeTypeToCount;
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: " + query, e);
    }
  }

}
