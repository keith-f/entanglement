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
 * File created: 15-Nov-2012, 15:30:12
 */

package com.entanglementgraph.couchdb;

import com.entanglementgraph.graph.GraphModelException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Keith Flanagan
 */
public interface NodeDAO {
  public static final String FIELD_KEYS = "keys";


  public <C extends Content> Node<C> getByKey(EntityKeys<C> keyset, boolean loadContent)
      throws GraphModelException;


//  /**
//   * Returns an instance by its unique ID. If no entity with the specified ID
//   * exists, then null is returned.
//   * @param uid the ID of the entity to return
//   * @return the entity with the unique ID, or null if no such entity exists.
//   * @throws com.entanglementgraph.player.LogPlayerException
//   */
//  public <C extends Content> Node<C> getByUid(String uid, boolean loadContent)
//      throws GraphModelException;

//  /**
//   * A convenience method that performs a standard <code>getByUid</code> lookup, and then attempts to deserialise
//   * the raw JSON document to a Java class of the specified type.
//   *
//   * @param uid the ID of the entity to return
//   * @param castToType the Java type to attempt to deserialise to.
//   * @return
//   * @throws com.entanglementgraph.graph.GraphModelException
//   */
//  public <C extends Content> Node<C> getByUid(String uid, Class<C> castToType)
//      throws GraphModelException;

//  public JsonNode getByAnyUid(Set<String> uids)
//      throws GraphModelException;
//
//  public JsonNode getByName(String entityType, String entityName)
//      throws GraphModelException;
//
//  /**
//   * A convenience method that performs a standard <code>getByName</code> lookup, and then attempts to deserialise
//   * the raw JSON document to a Java class of the specified type.
//   *
//   * @param entityType the Entanglement entity type name
//   * @param entityName the name of the entity to return
//   * @param castToType the Java type to attempt to deserialise to.
//   * @return
//   * @throws com.entanglementgraph.graph.GraphModelException
//   */
//  public <T> T getByName(String entityType, String entityName, Class<T> castToType)
//      throws GraphModelException;
//
//  /**
//   * Returns the the graph entity by specified by the type <code>entityType<code>
//   * and one (or more) of the names <code>entityNames<code>. This method is
//   * intended for the case where you wish to find a graph entity but are not sure
//   * which 'well known name' it is known by. Therefore, all names specified in
//   * <code>entityNames<code> should refer to the <b>same</b> entity.
//   * The result is undefined if you populate <code>entityNames<code> with names
//   * of >1 entity.
//   *
//   * @param type the type name of the entity to find.
//   * @param entityNames one or more alternative names for a <b>single</b> graph
//   * entity.
//   * @return the specified graph entity if a match to <code>entityType</code>
//   * and at least one element of <code>entityNames</code> was found. Returns
//   * <code>null</code> if no match was found. The result is undefined if you
//   * specify names in <code>entityNames</code> that are from multiple graph
//   * entities.
//   *
//   * @throws com.entanglementgraph.graph.GraphModelException
//   */
//  public JsonNode getByAnyName(String type, Set<String> entityNames)
//      throws GraphModelException;
//
//
//  public boolean existsByKey(EntityKeys keyset)
//      throws GraphModelException;
//
//  public boolean existsByUid(String uniqueId)
//      throws GraphModelException;
//
//  public boolean existsByAnyUid(Collection<String> entityUids)
//      throws GraphModelException;
//
//  /**
//   * Returns true if an entity of type <code>entityType<code> exists with the
//   * specified <code>entityName<code>.
//   * @param entityType
//   * @param entityName
//   * @return
//   * @throws com.entanglementgraph.graph.GraphModelException
//   */
//  public boolean existsByName(String entityType, String entityName)
//      throws GraphModelException;
//
//  /**
//   * Returns true if an entity of type <code>entityType<code> exists with any
//   * of the specified <code>entityNames<code>.
//   * @param entityType
//   * @param entityNames
//   * @return
//   * @throws com.entanglementgraph.graph.GraphModelException
//   */
//  public boolean existsByAnyName(String entityType, Collection<String> entityNames)
//      throws GraphModelException;
//
//  public void delete(EntityKeys keys)
//      throws GraphModelException;
//
//
//
//  public Iterable<JsonNode> iterateAll()
//      throws GraphModelException;
//
//  public List<String> listTypes()
//      throws GraphModelException;
//
//  public Iterable<EntityKeys> iterateKeys(int offset, int limit)
//      throws GraphModelException;
//
//  public Iterable<JsonNode> iterateByType(String typeName)
//      throws GraphModelException;
//
//  public Iterable<JsonNode> iterateByType(String typeName, Integer offset, Integer limit,
//                                          DBObject customQuery, DBObject sort)
//      throws GraphModelException;
//
//  public Iterable<EntityKeys> iterateKeysByType(String typeName, int offset, int limit)
//      throws GraphModelException;
//
//  public long countByType(String typeName)
//      throws GraphModelException;
//  public long count()
//      throws GraphModelException;

}
