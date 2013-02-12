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

package uk.ac.ncl.aries.entanglement.graph;

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
  private static final DBObject IDX_FROM_UID_TO_UID = 
          new BasicDBObject(FIELD_FROM_NODE_UID, 1).append(FIELD_TO_NODE_UID, 1);
  private static final DBObject IDX_TO_UID = new BasicDBObject(FIELD_TO_NODE_UID, 1);

  private static final DBObject IDX_FROM_UID_TO_NODE_TYPE = 
          new BasicDBObject(FIELD_FROM_NODE_UID, 1).append(FIELD_TO_NODE_TYPE, 1);
  
  private static final DBObject IDX_TYPE_FROM_NODE_UID = 
          new BasicDBObject(FIELD_TYPE, 1).append(FIELD_FROM_NODE_UID, 1);
  private static final DBObject IDX_TYPE_TO_NODE_UID = 
          new BasicDBObject(FIELD_TYPE, 1).append(FIELD_TO_NODE_UID, 1);
  
  private static final DBObject IDX_FROM_NODE_UID = new BasicDBObject(FIELD_FROM_NODE_UID, 1);
  private static final DBObject IDX_TO_NODE_UID = new BasicDBObject(FIELD_TO_NODE_UID, 1);

  
  private static final DBObject IDX_TYPE__FROM_NODE_UID__TO_NODE_UID = 
          new BasicDBObject(FIELD_TYPE, 1).append(FIELD_FROM_NODE_UID, 1).append(FIELD_TO_NODE_UID, 1);
  
  private final DBCollection nodeCol;
  
  ////////// DEBUG / TEST - Performance info stuff (end)
  
  public EdgeDAOSeparateDocImpl(ClassLoader classLoader, Mongo m, DB db, 
          DBCollection nodeCol, DBCollection edgeCol)
  {
    super(classLoader, m, db, edgeCol);
    
    this.nodeCol = nodeCol;
    
    //Create indexes
    edgeCol.ensureIndex(IDX_FROM_UID_TO_UID);
    edgeCol.ensureIndex(IDX_TO_UID);
    edgeCol.ensureIndex(IDX_FROM_UID_TO_NODE_TYPE);
    edgeCol.ensureIndex(IDX_TYPE_FROM_NODE_UID);
    edgeCol.ensureIndex(IDX_TYPE_TO_NODE_UID);
    
    edgeCol.ensureIndex(IDX_FROM_NODE_UID);
    edgeCol.ensureIndex(IDX_TO_NODE_UID);
    
    edgeCol.ensureIndex(IDX_TYPE__FROM_NODE_UID__TO_NODE_UID);
  }

  
  @Override
  public DBCollection getNodeCol() {
    return nodeCol;
  }
    
  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(String fromNodeUid, String toNodeUid)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges between nodes: {0} --> {1}", 
              new Object[]{fromNodeUid, toNodeUid});
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      query.put(FIELD_TO_NODE_UID, toNodeUid);

      final DBCursor cursor = col.find(query);

//      return new DeserialisingIterable<>(cursor, marshaller, Edge.class);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(
          String edgeType, String fromNodeUid, String toNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges of type {0} between nodes: {1} --> {2}", 
              new Object[]{edgeType, fromNodeUid, toNodeUid});
      query = new BasicDBObject();
      query.put(FIELD_TYPE, edgeType);
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      query.put(FIELD_TO_NODE_UID, toNodeUid);

      final DBCursor cursor = col.find(query);

      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }

  @Override
  public Iterable<DBObject> iterateEdgesFromNode(String fromNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges starting from node: {0}", 
              new Object[]{fromNodeUid});
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);

      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Iterable<DBObject> iterateEdgesToNode(String toNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges ending at node: {0}", 
              new Object[]{toNodeUid});
      query = new BasicDBObject();
      query.put(FIELD_TO_NODE_UID, toNodeUid);

      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }

  @Override
  public boolean existsEdgeToNodeOfType(String fromNodeUid, String toNodeType)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Finding edges from node: {0}, to any node of type {1}", 
              new Object[]{fromNodeUid, toNodeType});
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      query.put(FIELD_TO_NODE_TYPE, toNodeType);

      long count = col.count(query);
      return count > 0;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  
  @Override
  public Long countEdgesFromNode(String fromNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    } 
  }
  
  @Override
  public Long countEdgesOfTypeFromNode(String edgeType, String fromNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, edgeType);
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Long countEdgesToNode(String toNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TO_NODE_TYPE, toNodeUid);
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Long countEdgesOfTypeToNode(String edgeType, String toNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, edgeType);
      query.put(FIELD_TO_NODE_UID, toNodeUid);
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
          String edgeType, String fromNodeUid, String toNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, edgeType);
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      query.put(FIELD_TO_NODE_UID, toNodeUid);
      long count = col.count(query);
      return count;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Map<String, Long> countEdgesByTypeFromNode(String fromNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      
      List<String> types = (List<String>) col.distinct(FIELD_TYPE, query);
      Map<String, Long> edgeTypeToCount = new HashMap<>();
      for (String edgeType : types) {
        long count = countEdgesOfTypeFromNode(edgeType, fromNodeUid);
        edgeTypeToCount.put(edgeType, count);
      }
      
      return edgeTypeToCount;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Map<String, Long> countEdgesByTypeToNode(String toNodeUid)
          throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TO_NODE_UID, toNodeUid);
      
      List<String> types = (List<String>) col.distinct(FIELD_TYPE, query);
      Map<String, Long> edgeTypeToCount = new HashMap<>();
      for (String edgeType : types) {
        long count = countEdgesOfTypeFromNode(edgeType, toNodeUid);
        edgeTypeToCount.put(edgeType, count);
      }
      
      return edgeTypeToCount;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
}
