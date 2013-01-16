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

package uk.ac.ncl.aries.entanglement.graph;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.KeyExtractingIterable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.ObjectMarshallerFactory;

/**
 *
 * @author Keith Flanagan
 */
abstract public class AbstractGraphEntityDAO
    implements GraphEntityDAO
{
  private static final Logger logger =
      Logger.getLogger(AbstractGraphEntityDAO.class.getName());
  
  protected final Mongo m;
  protected final DB db;
  
  protected InsertMode insertModeHint;
  
//  private final JsonUtils json;
  protected final DbObjectMarshaller marshaller;
  
  ////////// DEBUG / TEST - Performance info stuff 
  private static final int PRINT_PERF_INFO_EVERY = 100000;
  private long timestampOfFirstInsert = -1;
  private long timestampOfLastPerformanceMessage = -1;
  private boolean printPeriodicPerformanceInfo;
  private int insertCount;
  ////////// DEBUG / TEST - Performance info stuff (end)
  
//  private final String graphUid;
//  private final String branchUid;
  
  /**
   * The MongoDB collection used for storing data about this graph checkout - 
   * we make the assumption that there is one graph checkout per collection.
   */
  protected final DBCollection col;
  
   public AbstractGraphEntityDAO(Mongo m, DB db, DBCollection col)
  {
    this.m = m;
    this.db = db;
    this.col = col;
    marshaller = ObjectMarshallerFactory.create();
//    this.json = new JsonUtils();
//    json.setExcludeNullAndEmptyValues(true);
//    json.setIgnoreUnknownProperties(true);
    
    printPeriodicPerformanceInfo = true;
    insertCount = 0;
    insertModeHint = InsertMode.INSERT_CONSISTENCY;
  }
  
  @Override
  public InsertMode getInsertModeHint()
  {
    return insertModeHint;
  }
  @Override
  public void setInsertModeHint(InsertMode insertMode)
  {
    this.insertModeHint = insertMode;
  }
  
  @Override
  public DBCollection getCollection()
  {
    return col;
  }

  @Override
  public void store(BasicDBObject item)
      throws GraphModelException
  {
    try {
//      logger.log(Level.INFO, "Storing node: {0}", node);
      if (insertModeHint == InsertMode.INSERT_CONSISTENCY) {
        String uid = item.getString(FIELD_UID);
        String name = item.getString(FIELD_NAME);
        String type = item.getString(FIELD_TYPE);
//        logger.info("Running in consistency mode");
        if (existsByUid(uid)) {
          throw new GraphModelException(
              "Failed to store item - an entity with this unique ID already exists: "+uid);
        }
        if (name != null && existsByName(type, name)) {
          throw new GraphModelException(
              "Failed to store item - an entity with the same 'well known' name already exists: "+name);        
        }
      }

      col.insert(item);
      
      /////// DEBUG (Performance info)
      if (printPeriodicPerformanceInfo) {  
        insertCount++;
        if (timestampOfLastPerformanceMessage < 0) {
          //First ever insert
          long now = System.currentTimeMillis();
          timestampOfLastPerformanceMessage = now;
          timestampOfFirstInsert = now;
          return;
        }
        if (insertCount % PRINT_PERF_INFO_EVERY == 0) {
          long now = System.currentTimeMillis();
          double secondsPerBlock = (now - timestampOfLastPerformanceMessage);
          secondsPerBlock = secondsPerBlock / 1000;
          double totalSeconds = (now - timestampOfFirstInsert);
          totalSeconds = totalSeconds / 1000;
          logger.log(Level.INFO,
                  "Inserted a total of\t{0}\t"+getClass().getSimpleName()+" documents. "
                  + "Total time\t{1}\t seconds. Seconds since last block: {2}",
                  new Object[]{insertCount, totalSeconds, secondsPerBlock});
          timestampOfLastPerformanceMessage = now;
        }
      }
      /////// DEBUG (Performance info) (end)
    }
    catch(Exception e)
    {
      throw new GraphModelException("Failed to store item: "+item, e);
    }
  }
  

  @Override
  public void setPropertyByUid(String uid, String propertyName, Object propertyValue)
      throws GraphModelException
  {
//    logger.log(Level.INFO, "Storing edge: {0}", edge);
    
    DBObject criteria = null;
    DBObject fieldsToReturn = null;
    DBObject sort = null;
    DBObject update = null;
    boolean remove = false;
    boolean returnNew = false;
    boolean upsert = false;
    
    try {
      criteria = new BasicDBObject(FIELD_UID, uid);
      
      fieldsToReturn = new BasicDBObject();
      sort = new BasicDBObject();
      
      DBObject valDbObj = marshaller.serialize(propertyValue);
      //Use of '$' operators causes an update, rather than full doc replacement
      update = new BasicDBObject(
          "$set", new BasicDBObject(propertyName, valDbObj));
      
      //Perform atomic update of a single document
      col.findAndModify(criteria, fieldsToReturn, sort, remove, update, returnNew, upsert);
    }
    catch(Exception e)
    {
      throw new GraphModelException("Failed to store item: "+propertyName 
              + " on: "+uid, e);
    }
  
  }
  
  @Override
  public void setPropertyByName(String entityType, String entityName, String propertyName, Object propertyValue)
      throws GraphModelException
  {
//    logger.log(Level.INFO, "Storing edge: {0}", edge);
    
    DBObject criteria = null;
    DBObject fieldsToReturn = null;
    DBObject sort = null;
    DBObject update = null;
    boolean remove = false;
    boolean returnNew = false;
    boolean upsert = false;
    
    try {
      criteria = new BasicDBObject(FIELD_TYPE, entityType).append(FIELD_NAME, entityName);
      
      fieldsToReturn = new BasicDBObject();
      sort = new BasicDBObject();
      
      DBObject valDbObj = marshaller.serialize(propertyValue);
      //Use of '$' operators causes an update, rather than full doc replacement
      update = new BasicDBObject(
          "$set", new BasicDBObject(propertyName, valDbObj));
      
      //Perform atomic update of a single document
      col.findAndModify(criteria, fieldsToReturn, sort, remove, update, returnNew, upsert);
    }
    catch(Exception e)
    {
      throw new GraphModelException("Failed to store item: "+propertyName 
              + " on entity with type: "+entityType+", name: "+entityType, e);
    }
  }
  
  
  @Override
  public String lookupUniqueIdForName(String type, String name)
      throws GraphModelException
  {
    DBObject query = null;
    DBObject fields = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, type);
      query.put(FIELD_NAME, name);
      
      fields = new BasicDBObject();
      fields.put(FIELD_UID, 1);

      DBObject result = col.findOne(query);
      if (result == null) {
        //There is no node with this name
        return null;
      }
      String nodeUniqueId = (String) result.get(FIELD_UID);

      return nodeUniqueId;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  
  @Override
  public DBObject getByUid(String nodeUid)
      throws GraphModelException
  {
    DBObject query = null;
    try {
//      logger.log(Level.INFO, "Getting node by UID: {0}", nodeUid);
      query = new BasicDBObject();
      query.put(FIELD_UID, nodeUid);


      DBObject obj = col.findOne(query);
      if (obj == null) {
        return null;
      }
      return obj;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation: \n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public DBObject getByName(String type, String name)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, type);
      query.put(FIELD_NAME, name);

      DBObject nodeObj = col.findOne(query);
      if (nodeObj == null) {
        return null;
      }
      return nodeObj;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation: \n"
          + "Query: "+query, e);
    }
  }
  
  
  @Override
  public boolean existsByUid(String uniqueId)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_UID, uniqueId);

      long count = col.count(query);
      if (count > 1) {
        throw new GraphModelException(
                "Unique ID: "+uniqueId+" should be unique, but we found: "
                + count + " instances with that name!");
      }
      return count == 1;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation: \n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public boolean existsByName(String entityType, String entityName)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, entityType);
      query.put(FIELD_NAME, entityName);

      long count = col.count(query);
      if (count > 1) {
        throw new GraphModelException(
            "Type: "+entityType+", Name: "+entityName 
            +" should be unique, but we found: "+count
            + " instances with that name!");
      }
      return count == 1;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public DBObject deleteByUid(String uid)
      throws GraphModelException
  {
    DBObject query = null;
    try {
//      logger.log(Level.INFO, "Deleting node by UID: {0}", nodeUid);
      DBObject toDelete = getByUid(uid);
      if (toDelete == null) {
        throw new GraphModelException(
            "Attempted a delete operation, but no such entity exists: "+uid);
      }

      /*
       * Below code is commented for now, but we need to implement this at 
       * some point for consistency reasons.
       */
      // Check that this node doesn't connect to any others
//      if (!toDelete.getOutgoingEdges().isEmpty())
//      {
//        throw new GraphModelException(
//            "Attempted to delete node: "+nodeUid
//            + ". However, the node contains outgoing edges to other nodes."
//            + " Delete these first before attempting to remote this node.");
//      }
      
      // Check that this node is not connected to by others
//      if (!toDelete.getIncomingEdgeIds().isEmpty())
//      {
//        throw new GraphModelException(
//            "Attempted to delete node: "+nodeUid
//            + ". However, the node contains incoming edges from other nodes."
//            + " Delete these first before attempting to remote this node.");
//      }
      
      //Delete the specified object
      query = new BasicDBObject();
      query.put(FIELD_UID, uid);
      
      WriteResult result = col.remove(query);
//      logger.log(Level.INFO, "WriteResult: {0}", result.toString());

      return toDelete;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation: \n"
          + "Query: "+query, e);
    }    
  }
  
  
  
  @Override
  public DBCursor iterateAll()
      throws GraphModelException
  {
    DBObject query = null;
    try {
      //Empty 'query' selects all documents (nodes in this case)
      query = new BasicDBObject();

      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation: \n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public List<String> listTypes()
      throws GraphModelException
  {
    try {
      List<String> types = (List<String>) col.distinct(FIELD_TYPE);
      return types;
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation\n", e);
    }    
  }
  
  @Override
  public Iterable<DBObject> iterateByType(String typeName)
      throws GraphModelException
  {
    DBObject query = null;
    try {
//      logger.log(Level.INFO, "Getting node(s) by type: {0}", typeName);
      query = new BasicDBObject();
      query.put(FIELD_TYPE, typeName);

//      logger.log(Level.INFO, "Generated query: {0}", query);

      DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation to find nodes of type: "+typeName+"\n"
          + "Query was: "+query, e);
    }    
  }
  
  @Override
  public Iterable<String> iterateIdsByType(String typeName, int offset, int limit)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, typeName);
      DBObject keys = new BasicDBObject(FIELD_UID, 1);
      DBCursor cursor = col.find(query, keys, offset, limit);
      
      return new KeyExtractingIterable<>(cursor, FIELD_UID, String.class);
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation. Type was: "+typeName+"\n"
          + "Query was: "+query, e);
    }
  }
  
  @Override
  public Iterable<String> iterateNamesByType(String typeName, int offset, int limit)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, typeName);
      DBObject keys = new BasicDBObject(FIELD_NAME, 1);
      DBCursor cursor = col.find(query, keys, offset, limit);
      
      return new KeyExtractingIterable<>(cursor, FIELD_NAME, String.class);
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation. Type was: "+typeName+"\n"
          + "Query was: "+query, e);
    }
  }
  
  @Override
  public long countByType(String typeName)
      throws GraphModelException
  {
    DBObject query = null;
    try {
      query = new BasicDBObject();
      query.put(FIELD_TYPE, typeName);
      return col.count(query);
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation\n"
          + "Query was: "+query, e);
    }  
  }
  
  @Override
  public long count()
      throws GraphModelException
  {
    try {
      return col.count();
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation", e);
    }  
  }
}
