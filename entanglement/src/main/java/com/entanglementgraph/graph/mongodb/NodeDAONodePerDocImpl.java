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
 */

package com.entanglementgraph.graph.mongodb;

import com.entanglementgraph.graph.*;
import com.mongodb.*;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.mongodb.dbobject.KeyExtractingIterable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class NodeDAONodePerDocImpl<C extends Content>
    implements NodeDAO<C>
{
  private static final Logger logger =
      Logger.getLogger(NodeDAO.class.getName());

  public static final String FIELD_KEYS = "keys";
  public static final String FIELD_KEYS_TYPE = FIELD_KEYS + ".type";
  public static final String FIELD_KEYS_UIDS = FIELD_KEYS + ".uids";
  public static final String FIELD_KEYS_NAMES = FIELD_KEYS + ".names";

  public static final String FIELD_VIRTUAL = "entanglement_virtual_node";

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
  public NodeDAONodePerDocImpl(ClassLoader classLoader, Mongo m, DB db, DBCollection col)
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


  private static Iterable<DBObject> buildKeysetIndexes() {
    return EntityKeys.buildKeyIndexes(FIELD_KEYS);
  }


  public DBCollection getCollection()
  {
    return col;
  }

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
  public EntityKeys<C> populateFullKeyset(EntityKeys<C> partial) throws GraphModelException {
    return null;
  }

  @Override
  public Node<C> getByKey(EntityKeys<C> keyset)
      throws GraphModelException {
    //TODO update return type
    return null;
//    try {
//      BasicDBObject doc = getByAnyUid(keyset.getUids());
//      if (doc != null) {
//        return doc;
//      }
//
//      doc = getByAnyName(keyset.getType(), keyset.getNames());
//      return doc;
//    }
//    catch(Exception e) {
//      throw new GraphModelException("Failed to perform database operation.", e);
//    }
  }


  private BasicDBObject getByAnyUid(Set<String> uids)
      throws GraphModelException
  {
    if (uids.isEmpty()) {
      return null;
    }
    DBObject query = buildAnyUidQuery(uids);
    try {
      BasicDBObject obj = (BasicDBObject)  col.findOne(query);
      return obj;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation. Query was: "+query, e);
    }
  }

  private BasicDBObject getByAnyName(String entityType, Set<String> entityNames)
      throws GraphModelException
  {
    if (entityNames.isEmpty()) {
      return null;
    }
    DBObject query = buildAnyNameQuery(entityNames);
    try {
      DBCursor result = col.find(query);
      /*
       * The 'result' object above contains any MongoDB documents that match one or more of the specified entityNames.
       * However, we don't yet know if any of these documents have a 'keys.type' value that matches 'entityType'. This
       * is because there currently appears to be a Mongo bug that means querying compound indexes containing arrays is
       * really slow.
       *
       */
      for (DBObject next : result) {
        //FIXME at some point, find out why this doesn't work (where FIELD_KEYS_TYPE == "keys.type") ...
//        String nextType = (String) next.get(FIELD_KEYS_TYPE);
        //FIXME ... and why this does.
        String nextType = (String) ((BasicDBObject)next.get("keys")).get("type");

        if (nextType == null) {
          throw new GraphModelException("When performing a query with one or more entity names, one or more of the " +
              "result items had a entity type of NULL. Your dataset therefore inconsistent, since an entity type is " +
              "required when specifying one or more entity names.\n" +
              "Query was: "+query + "\nOffending item was: "+next);
        }
        if (nextType.equals(entityType)) {
          return (BasicDBObject) next;
        }
      }
      return null;
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation:\n"
          + "Query: "+query, e);
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

  private boolean existsByAnyUid(Collection<String> entityUids)
      throws GraphModelException
  {
    DBObject query = buildAnyUidQuery(entityUids);
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

  private boolean existsByAnyName(String entityType, Collection<String> entityNames)
      throws GraphModelException
  {
    DBObject query = buildAnyNameQuery(entityNames);
    DBObject fields = new BasicDBObject(FIELD_KEYS_TYPE, 1);
    try {
      DBCursor result = col.find(query, fields);
      /*
       * The 'result' object above contains any MongoDB documents that match one or more of the specified entityNames.
       * However, we don't yet know if any of these documents have a 'keys.type' value that matches 'entityType'. This
       * is because there currently appears to be a Mongo bug that means querying compound indexes containing arrays is
       * really slow.
       *
       */
      for (DBObject next : result) {
        //FIXME at some point, find out why this doesn't work (where FIELD_KEYS_TYPE == "keys.type") ...
//        String nextType = (String) next.get(FIELD_KEYS_TYPE);
        //FIXME ... and why this does.
        String nextType = (String) ((BasicDBObject)next.get("keys")).get("type");

        if (nextType == null) {
          throw new GraphModelException("When performing a query with one or more entity names, one or more of the " +
              "result items had a entity type of NULL. Your dataset therefore inconsistent, since an entity type is " +
              "required when specifying one or more entity names.\n" +
              "Query was: "+query + "\nOffending item was: "+next);
        }
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


  private DBObject buildAnyUidQuery(Collection<String> entityUids) {
    DBObject query = new BasicDBObject();
    BasicDBList uidList = new BasicDBList();
    uidList.addAll(entityUids);
    query.put(FIELD_KEYS_UIDS, new BasicDBObject("$in", uidList));
    return query;
  }


  public void delete(EntityKeys keys)
      throws GraphModelException
  {
    try {
      //Build a query to delete by UID
      if (!keys.getUids().isEmpty()) {
        DBObject deleteByAnyUid = buildAnyUidQuery(keys.getUids());
        col.remove(deleteByAnyUid);
      }

      //Build a query to delete by type and name
      if (!keys.getNames().isEmpty()) {
        if (keys.getType() == null || keys.getType().length() == 0) {
          throw new GraphModelException("While attempting to delete an entity by name, no type field was set. " +
              "EntityKeys was: "+keys);
        }
        DBObject deleteByAnyName = buildAnyNameQuery(keys.getUids());
        //This is likely to be slow (MongoDB index bug?), but hopefully 'delete' isn't a common operation.
        deleteByAnyName.put(FIELD_KEYS_TYPE, keys.getType());
        col.remove(deleteByAnyName);
      }
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform database operation.", e);
    }
  }

  public Iterable<Node<C>> iterateAll()
      throws GraphModelException
  {
    //TODO update implementation
    return null;
//    //Empty 'query' selects all documents (nodes in this case)
//    DBObject query = new BasicDBObject();
//    try {
//      final DBCursor cursor = col.find(query);
//      return cursor;
//    }
//    catch(Exception e) {
//      throw new GraphModelException("Failed to perform database operation: \n"
//          + "Query: "+query, e);
//    }
  }


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

  public Iterable<EntityKeys> iterateKeys(int offset, int limit)
      throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    DBObject keys = new BasicDBObject(FIELD_KEYS, 1); // Return the key subdocument
    try {
      DBCursor cursor = col.find(query, keys).skip(offset).limit(limit);
      return new KeyExtractingIterable<>(cursor, marshaller, FIELD_KEYS, EntityKeys.class);
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation.\nQuery was: "+query, e);
    }
  }

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

  public Iterable<EntityKeys> iterateKeysByType(String typeName, int offset, int limit)
      throws GraphModelException
  {
    DBObject query = new BasicDBObject();
    query.put(FIELD_KEYS_TYPE, typeName);
    DBObject keys = new BasicDBObject(FIELD_KEYS, 1); // Return the entire key subdocument
    try {
      //DBCursor cursor = col.find(query, keys, offset, limit);
      DBCursor cursor = col.find(query, keys).skip(offset).limit(limit);
      return new KeyExtractingIterable<>(cursor, marshaller, FIELD_KEYS, EntityKeys.class);
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation. Type was: "+typeName+"\n"
              + "Query was: "+query, e);
    }
  }

  public Iterable<DBObject> iterateByType(String typeName, Integer offset, Integer limit,
                                          DBObject customQuery, DBObject sort)
      throws GraphModelException {
    DBObject query = new BasicDBObject();
    query.put(FIELD_KEYS_TYPE, typeName);
    query.putAll(customQuery);
    try {
      DBCursor cursor;
      if (sort != null) {
        cursor = col.find(query).skip(offset).limit(limit).sort(sort);
      } else {
        cursor = col.find(query).skip(offset).limit(limit);
      }
      return cursor;
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform database operation. Type was: "+typeName+"\nQuery was: "+query, e);
    }
  }

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
