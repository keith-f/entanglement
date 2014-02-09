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

package com.entanglementgraph.graph;

import java.util.List;

/**
 * @author Keith Flanagan
 */
public interface NodeDAO {
  public static final String FIELD_KEYS = "keys";

  /**
   * Given a partial keyset (a keyset suspected of containing less then the complete number of UIDs or names for a
   * given node), queries the database and returns a fully populated keyset.
   * @param partial
   * @return
   */
  public <T> EntityKeys<T> populateFullKeyset(EntityKeys<T> partial)
      throws GraphModelException;

  public <C extends Content> Node<C> getByKey(EntityKeys<C> keyset)
      throws GraphModelException;

  public <C extends Content> boolean existsByKey(EntityKeys<C> keyset)
      throws GraphModelException;

  //TODO implement these later
//  public Iterable<Node<? extends Content>> iterateAll()
//      throws GraphModelException;
//  public long countAll()
//      throws GraphModelException;
//
//  public <C extends Content> Iterable<Node<C>> iterateByType(String typeName)
//      throws GraphModelException;
//  public long countByType(String typeName)
//      throws GraphModelException;
//  public List<String> listTypes()
//      throws GraphModelException;





  // TODO do we still need these?
//  public Iterable<EntityKeys> iterateKeys(int offset, int limit)
//      throws GraphModelException;
//  public Iterable<EntityKeys> iterateKeysByType(String typeName, int offset, int limit)
//      throws GraphModelException;

}
