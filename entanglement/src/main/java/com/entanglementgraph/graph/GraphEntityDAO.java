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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Query methods that are common to both Nodes and Edges.
 * 
 * @author Keith Flanagan
 */
public interface GraphEntityDAO 
{
  public static final String FIELD_KEYS = "keys";
  public static final String FIELD_KEYS_TYPE = FIELD_KEYS+".type";
  public static final String FIELD_KEYS_UIDS = FIELD_KEYS+".uids";
  public static final String FIELD_KEYS_NAMES = FIELD_KEYS+".names";
  public static final String FIELD_VIRTUAL = "entanglement_virtual_node";

  public DBCollection getCollection();
  
  /**
   * Stores a new graph entity to the database. If the <code>InsertMode</code>
   * hint is set to <code>INSERT_CONSISTENCY</code> then additional consistency
   * checks are performed to ensure that no entity currently exists with the 
   * same UID or type/name as the entity that you are attempting to insert.
   * 
   * If the <code>InsertMode</code> is set to <code>PERFORMANCE</code>, then
   * no ID checks are performed and we assume that you not attempting to
   * insert objects that would ID- or name-clash with an existing entity.
   * 
   * @param entity
   * @throws GraphModelException 
   */
  public void store(BasicDBObject entity)
      throws GraphModelException;

  
  /**
   * Updates (actually, replaces) an existing graph entity. The <code>UID</code> 
   * field is used to find the document that should be replaced.
   * 
   * Replacing large documents may be expensive when only a tiny proportion of
   * the document has changed. You may wish to use one of the 'setProperty' 
   * methods if you only need to update a couple of fields.
   * 
   * @param updated the complete MongoDB document that is to replace an existing
   * MongoDB document of the same UID. Note that if the existing MongoDB document
   * contains a 'name' field, then you must ensure that the new document also
   * contains the same value for the 'name' field.
   * @throws GraphModelException 
   */
  public void update(BasicDBObject updated)
      throws GraphModelException;
  
  /**
   * Given a node's UID, sets the named property to the given value. Note that
   * any arbitrary property name can be given here, even if this node was
   * originally serialized from a Java bean that doesn't contain this property. 
   * 
   * @param uid the ID of the node to update
   * @param propertyName the property to set.
   * @param propertyValue a data bean to be JSON-serialised and converted into
   * a DBObject.
   * @throws LogPlayerException
   * @deprecated Don't use
   */
  public void setPropertyByUid(String uid, String propertyName, Object propertyValue)
      throws GraphModelException;

  /**
   * @deprecated Don't use
   * @param entityType
   * @param entityName
   * @param propertyName
   * @param propertyValue
   * @throws GraphModelException
   */
  public void setPropertyByName(String entityType, String entityName, String propertyName, Object propertyValue)
      throws GraphModelException;


  public EntityKeys getEntityKeysetForUid(String uid) throws GraphModelException;

  public EntityKeys getEntityKeysetForName(String type, String name) throws GraphModelException;

  public JsonNode getByKey(EntityKeys keyset)
      throws GraphModelException;

  /**
   * A convenience method that performs a standard <code>getByKey</code> lookup, and then attempts to deserialise
   * the raw JSON document to a Java class of the specified type.
   *
   * @param keyset the EntityKeys representing the target graph entity
   * @param castToType the Java type to attempt to deserialise to.
   * @return
   * @throws GraphModelException
   */
  public <T> T getByKey(EntityKeys keyset, Class<T> castToType)
      throws GraphModelException;

  /**
   * Returns an instance by its unique ID. If no entity with the specified ID
   * exists, then null is returned.
   * @param uid the ID of the entity to return
   * @return the entity with the unique ID, or null if no such entity exists.
   * @throws LogPlayerException 
   */
  public JsonNode getByUid(String uid)
      throws GraphModelException;

  /**
   * A convenience method that performs a standard <code>getByUid</code> lookup, and then attempts to deserialise
   * the raw JSON document to a Java class of the specified type.
   *
   * @param uid the ID of the entity to return
   * @param castToType the Java type to attempt to deserialise to.
   * @return
   * @throws GraphModelException
   */
  public <T> T getByUid(String uid, Class<T> castToType)
      throws GraphModelException;

  public JsonNode getByAnyUid(Set<String> uids)
      throws GraphModelException;

  public JsonNode getByName(String entityType, String entityName)
      throws GraphModelException;

  /**
   * A convenience method that performs a standard <code>getByName</code> lookup, and then attempts to deserialise
   * the raw JSON document to a Java class of the specified type.
   *
   * @param entityType the Entanglement entity type name
   * @param entityName the name of the entity to return
   * @param castToType the Java type to attempt to deserialise to.
   * @return
   * @throws GraphModelException
   */
  public <T> T getByName(String entityType, String entityName, Class<T> castToType)
      throws GraphModelException;
  
  /**
   * Returns the the graph entity by specified by the type <code>entityType<code>
   * and one (or more) of the names <code>entityNames<code>. This method is 
   * intended for the case where you wish to find a graph entity but are not sure
   * which 'well known name' it is known by. Therefore, all names specified in
   * <code>entityNames<code> should refer to the <b>same</b> entity.
   * The result is undefined if you populate <code>entityNames<code> with names
   * of >1 entity.
   * 
   * @param type the type name of the entity to find.
   * @param entityNames one or more alternative names for a <b>single</b> graph
   * entity.
   * @return the specified graph entity if a match to <code>entityType</code>
   * and at least one element of <code>entityNames</code> was found. Returns
   * <code>null</code> if no match was found. The result is undefined if you 
   * specify names in <code>entityNames</code> that are from multiple graph 
   * entities.
   * 
   * @throws GraphModelException 
   */
  public JsonNode getByAnyName(String type, Set<String> entityNames)
      throws GraphModelException;


  public boolean existsByKey(EntityKeys keyset)
      throws GraphModelException;

  public boolean existsByUid(String uniqueId)
      throws GraphModelException;

  public boolean existsByAnyUid(Collection<String> entityUids)
      throws GraphModelException;
  
  /**
   * Returns true if an entity of type <code>entityType<code> exists with the
   * specified <code>entityName<code>.
   * @param entityType
   * @param entityName
   * @return
   * @throws GraphModelException 
   */
  public boolean existsByName(String entityType, String entityName)
      throws GraphModelException;
  
  /**
   * Returns true if an entity of type <code>entityType<code> exists with any 
   * of the specified <code>entityNames<code>.
   * @param entityType
   * @param entityNames
   * @return
   * @throws GraphModelException 
   */
  public boolean existsByAnyName(String entityType, Collection<String> entityNames)
      throws GraphModelException;
  
  public void delete(EntityKeys keys)
      throws GraphModelException;
  
  
  
  public Iterable<JsonNode> iterateAll()
      throws GraphModelException;
  
  public List<String> listTypes()
      throws GraphModelException;

  public Iterable<EntityKeys> iterateKeys(int offset, int limit)
      throws GraphModelException;

  public Iterable<JsonNode> iterateByType(String typeName)
      throws GraphModelException;

  public Iterable<JsonNode> iterateByType(String typeName, Integer offset, Integer limit,
                                              DBObject customQuery, DBObject sort)
      throws GraphModelException;

  public Iterable<EntityKeys> iterateKeysByType(String typeName, int offset, int limit)
      throws GraphModelException;
  
  public long countByType(String typeName)
      throws GraphModelException;
  public long count()
      throws GraphModelException;
}
