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

import static com.entanglementgraph.util.MongoUtils.singleton;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class EdgeDAOSeparateDocImpl
    extends AbstractGraphEntityDAO
    implements EdgeDAO
{
  private static final Logger logger =
      Logger.getLogger(EdgeDAOSeparateDocImpl.class.getName());

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
  
  ////////// DEBUG / TEST - Performance info stuff (end)
  
  public EdgeDAOSeparateDocImpl(ClassLoader classLoader, Mongo m, DB db, 
          DBCollection nodeCol, DBCollection edgeCol)
  {
    super(classLoader, m, db, edgeCol);
    
    this.nodeCol = nodeCol;
    
    //Create indexes
    MongoUtils.createIndexes(edgeCol, buildFromIndexes());
    MongoUtils.createIndexes(edgeCol, buildToIndexes());
  }
    
  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(String fromUid, String toUid)
      throws GraphModelException
  {
    logger.log(Level.INFO, "Iterating edges between nodes: {0} --> {1}", new Object[]{fromUid, toUid});
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(
          String edgeType, String fromUid, String toUid)
          throws GraphModelException
  {
    logger.log(Level.INFO, "Iterating edges of type {0} between nodes: {1} --> {2}",
        new Object[]{edgeType, fromUid, toUid});
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    query.put(FIELD_KEYS_TYPE, edgeType);
    try {
      final DBCursor cursor = col.find(query);

      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }

  @Override
  public Iterable<DBObject> iterateEdgesFromNode(String fromUid)
          throws GraphModelException
  {
    logger.log(Level.INFO, "Iterating edges starting from node: {0}",  new Object[]{fromUid});
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: "+query, e);
    }
  }
  
  @Override
  public Iterable<DBObject> iterateEdgesToNode(String toUid)
          throws GraphModelException
  {
    logger.log(Level.INFO, "Iterating edges ending at node: {0}",  new Object[]{toUid});
    DBObject query = new BasicDBObject();
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    try {
      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\nQuery: "+query, e);
    }
  }

  @Override
  public boolean existsEdgeToNodeOfType(String fromUid, String toNodeType)
          throws GraphModelException
  {
    logger.log(Level.INFO, "Finding edges from node: {0}, to any node of type {1}",
        new Object[]{fromUid, toNodeType});
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    query.put(FIELD_TO_KEYS_TYPE, toNodeType);
    try {
      return col.find(query).limit(1).hasNext();
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  
  @Override
  public Long countEdgesFromNode(String fromUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    try {
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    } 
  }
  
  @Override
  public Long countEdgesOfTypeFromNode(String edgeType, String fromUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    query.put(FIELD_KEYS_TYPE, edgeType);
    try {
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Long countEdgesToNode(String toUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    try {
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Long countEdgesOfTypeToNode(String edgeType, String toUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    query.put(FIELD_KEYS_TYPE, edgeType);
    try {
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Long countEdgesOfTypeBetweenNodes(
          String edgeType, String fromUid, String toUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    query.put(FIELD_KEYS_TYPE, edgeType);
    try {
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Map<String, Long> countEdgesByTypeFromNode(String fromUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", singleton(fromUid)));
    DBObject fields = new BasicDBObject();
    fields.put(FIELD_KEYS_TYPE, 1);
    try {
      Map<DBObject, Long> counts =  count(col.find(query, fields));
      Map<String, Long> edgeTypeToCount = new HashMap<>();

      for (Map.Entry<DBObject, Long> entry : counts.entrySet()) {
        edgeTypeToCount.put((String) entry.getKey().get(FIELD_KEYS_TYPE), entry.getValue());
      }
      
      return edgeTypeToCount;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }

  private static <T> Map<T, Long> count(Iterable<T> items) {
    Map<T, Long> counts = new HashMap<>();
    for (T item : items) {
      Long c = counts.get(item);
      if (c == null) {
        c = 0l;
      }
      counts.put(item, c+1);
    }
    return counts;
  }
  
  @Override
  public Map<String, Long> countEdgesByTypeToNode(String toUid)
          throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", singleton(toUid)));
    DBObject fields = new BasicDBObject();
    fields.put(FIELD_KEYS_TYPE, 1);
    try {
      Map<DBObject, Long> counts =  count(col.find(query, fields));
      Map<String, Long> edgeTypeToCount = new HashMap<>();

      for (Map.Entry<DBObject, Long> entry : counts.entrySet()) {
        edgeTypeToCount.put((String) entry.getKey().get(FIELD_KEYS_TYPE), entry.getValue());
      }

      return edgeTypeToCount;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
}
