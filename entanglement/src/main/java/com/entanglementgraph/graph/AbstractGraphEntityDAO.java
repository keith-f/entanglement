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

package com.entanglementgraph.graph;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.KeyExtractingIterable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.entanglementgraph.ObjectMarshallerFactory;

/**
 *
 * @author Keith Flanagan
 */
abstract public class AbstractGraphEntityDAO
    implements GraphEntityDAO
{
  private static final Logger logger =
      Logger.getLogger(AbstractGraphEntityDAO.class.getName());

  private static Iterable<DBObject> buildKeysetIndexes() {
    return EntityKeys.buildKeyIndexes(FIELD_KEYS);
  }
  
  protected final Mongo m;
  protected final DB db;

  
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
  
  /**
   * 
   * @param classLoader a custom classloader may be required here in order for
   * a <code>DbObjectMarshaller</code> to correctly deserialize objects. 
   * 
   * TODO If we we only use the marshaller for serialization, then we probably 
   * don't need this parameter.
   * @param m
   * @param db
   * @param col 
   */
  public AbstractGraphEntityDAO(ClassLoader classLoader, Mongo m, DB db, DBCollection col)
  {
    this.m = m;
    this.db = db;
    this.col = col;
    marshaller = ObjectMarshallerFactory.create(classLoader);
    
    printPeriodicPerformanceInfo = true;
    insertCount = 0;
    //Make sure indexes exist
    MongoUtils.createIndexes(this.col, buildKeysetIndexes());
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
      col.insert(item); //Direct to database
//      batchInserter.addItemToBatch(entitySet, item); //Add to batch
      
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
  public void update(BasicDBObject updated)
      throws GraphModelException
  {
    try {
//      logger.info("Updating object: "+updated);
      EntityKeys keys = MongoUtils.parseKeyset(marshaller, updated);
      BasicDBObject result = getByAnyUid(keys.getUids());
      if (result == null) {
        result = getByAnyName(keys.getType(), keys.getNames());
      }
      if (result == null) {
        throw new GraphModelException("Failed to find any object with: "+keys.toString());
      }

      DBObject criteria = new BasicDBObject(FIELD_KEYS, result.get(FIELD_KEYS));

      //Perform atomic update of a single document
      col.findAndModify(criteria, updated);
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to update item: "+updated, e);
    }
  }

  @Override
  public void setPropertyByUid(String uid, String propertyName, Object propertyValue)
      throws GraphModelException
  {
    DBObject criteria;
    DBObject fieldsToReturn;
    DBObject sort;
    DBObject update;
    boolean remove = false;
    boolean returnNew = false;
    boolean upsert = false;
    
    try {
      criteria = new BasicDBObject();
      BasicDBList uidList = new BasicDBList();
      uidList.add(uid);
      criteria.put(FIELD_KEYS_UIDS, new BasicDBObject("$in", uidList));
      
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
      BasicDBList nameList = new BasicDBList();
      nameList.add(entityName);
//      criteria.put(FIELD_KEYS+".type", entityType);
      criteria.put(FIELD_KEYS_NAMES,new BasicDBObject("$in", nameList));
      
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
  public EntityKeys getEntityKeysetForUid(String uid) throws GraphModelException {
    DBObject query = buildGetByAnyUid(Collections.singleton(uid));
    DBObject fields = new BasicDBObject(FIELD_KEYS, 1);
    try {
      DBObject result = col.findOne(query, fields);
      if (result == null) {
        //There is no node with this entityName
        return null;
      }

      EntityKeys keyset = MongoUtils.parseKeyset(marshaller, result);
      return keyset;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation. Query was: "+query, e);
    }
  }

  @Override
  public EntityKeys getEntityKeysetForName(String type, String entityName) throws GraphModelException {
    DBObject query = buildAnyNameQuery(Collections.singleton(entityName));
    DBObject fields = new BasicDBObject(FIELD_KEYS, 1);
    try {
      DBObject result = col.findOne(query, fields);
      if (result == null) {
        //There is no node with this entityName
        return null;
      }

      EntityKeys keyset = MongoUtils.parseKeyset(marshaller, result);
      return keyset;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation. Query was: "+query, e);
    }
  }

  @Override
  public BasicDBObject getByKey(EntityKeys keyset)
      throws GraphModelException {
    try {
      BasicDBObject doc = getByAnyUid(keyset.getUids());
      if (doc != null) {
        return doc;
      }

      doc = getByAnyName(keyset.getType(), keyset.getNames());
      return doc;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation.", e);
    }
  }


  @Override
  public BasicDBObject getByUid(String entityUid)
      throws GraphModelException
  {
    return getByAnyUid(Collections.singleton(entityUid));
  }

  @Override
  public BasicDBObject getByAnyUid(Set<String> uids)
      throws GraphModelException
  {
    DBObject query = buildGetByAnyUid(uids);
    try {
      BasicDBObject obj = (BasicDBObject)  col.findOne(query);
      return obj;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation. Query was: "+query, e);
    }
  }
  
  @Override
  public BasicDBObject getByName(String type, String entityName)
      throws GraphModelException
  {
    return getByAnyName(type, Collections.singleton(entityName));
  }
  
  @Override
  public BasicDBObject getByAnyName(String type, Set<String> entityNames)
      throws GraphModelException
  {
    DBObject query = buildAnyNameQuery(entityNames);
    try {
      BasicDBObject obj = (BasicDBObject)  col.findOne(query);
      return obj;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation. Query was: "+query, e);
    }
  }

  @Override
  public boolean existsByKey(EntityKeys keyset)
      throws GraphModelException {
    try {
      boolean exists = existsByAnyUid(keyset.getUids());
      if (exists) {
        return true;
      }

      exists = existsByAnyName(keyset.getType(), keyset.getNames());
      return exists;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation.", e);
    }
  }
 
  
  @Override
  public boolean existsByUid(String entityUid)
      throws GraphModelException
  {
    return existsByAnyUid(Collections.singleton(entityUid));
  }

  @Override
  public boolean existsByAnyUid(Collection<String> entityUids)
      throws GraphModelException
  {
    DBObject query = buildGetByAnyUid(entityUids);
    DBObject fields = new BasicDBObject("_id", 1);
    try {
      DBCursor result = col.find(query, fields).limit(1);
      return result.hasNext();
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation: \n"
          + "Query: "+query, e);
    }
  }

  private DBObject buildGetByAnyUid(Collection<String> entityUids) {
    DBObject query = new BasicDBObject();
    BasicDBList uidList = new BasicDBList();
    uidList.addAll(entityUids);
    query.put(FIELD_KEYS_UIDS, new BasicDBObject("$in", uidList));
    return query;
  }

  @Override
  public boolean existsByName(String entityType, String entityName)
      throws GraphModelException
  {
    return existsByAnyName(entityType, Collections.singleton(entityName));
  }
  
  @Override
  public boolean existsByAnyName(String entityType, Collection<String> entityNames)
      throws GraphModelException
  {
    DBObject query = buildAnyNameQuery(entityNames);
    DBObject fields = new BasicDBObject(FIELD_KEYS_TYPE, 1);
    try {
      DBCursor result = col.find(query, fields).limit(1);

      if (!result.hasNext()) {
        return false;
      }
      for (DBObject next : result) {
        String nextType = (String) next.get(FIELD_KEYS_TYPE);
        if (nextType.equals(entityType)) {
          return true;
        }
      }
      return false;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }

  /**
   * Builds a MongoDB query to fetch documents by name, ignoring type. Since names are not guaranteed to be unique,
   * you should manually check the type of the documents returned.
   * This will be fixed once MongoDB performance improves for compound indexes.
   * @param entityNames
   * @return
   */
  private DBObject buildAnyNameQuery(Collection<String> entityNames) {
    DBObject query = new BasicDBObject();
    BasicDBList nameList = new BasicDBList();
    nameList.addAll(entityNames);
    query.put(FIELD_KEYS_NAMES, new BasicDBObject("$in", nameList));
    return query;
  }


  @Override
  public BasicDBObject deleteByUid(String uid)
      throws GraphModelException
  {
    DBObject query = null;
    try {
//      logger.log(Level.INFO, "Deleting node by UID: {0}", nodeUid);
      BasicDBObject toDelete = (BasicDBObject) getByUid(uid);
      if (toDelete == null) {
        throw new GraphModelException(
            "Attempted a delete operation, but no such entity exists: "+uid);
      }

      
      //Delete the specified object
      query = new BasicDBObject();
      BasicDBList uidList = new BasicDBList();
      uidList.add(uid);
      query.put(FIELD_KEYS_UIDS,new BasicDBObject("$in", uidList));
      
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
    //Empty 'query' selects all documents (nodes in this case)
    DBObject query = new BasicDBObject();
    try {
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
      List<String> types = (List<String>) col.distinct(FIELD_KEYS_TYPE);
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
    DBObject query = new BasicDBObject();
    query.put(FIELD_KEYS_TYPE, typeName);

    try {
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
  public Iterable<EntityKeys> iterateKeysByType(String typeName, int offset, int limit)
      throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_KEYS_TYPE, typeName);
    DBObject keys = new BasicDBObject(FIELD_KEYS, 1); // Return the entire key subdocument
    try {
      DBCursor cursor = col.find(query, keys, offset, limit);
      return new KeyExtractingIterable<>(cursor, FIELD_KEYS, EntityKeys.class);
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
    DBObject query = new BasicDBObject();
    query.put(FIELD_KEYS_TYPE, typeName);
    try {

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
