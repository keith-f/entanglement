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

package com.entanglementgraph.graph.commands;

/**
 * Determines what happens when a new graph update command needs to perform an
 * update to an existing graph entity.
 * 
 * @author Keith Flanagan
 */
public enum MergePolicy
{
  /**
   * In case of a UID or name clash, take no action; i.e., the first entity
   * to be committed wins. No action will be taken as a result of this
   * graph operation.
   */
  NONE,
  /**
   * In case of a UID or name clash, take no action in the database and report
   * an error.
   */
  ERR,

  
  /**
   * If the UID or type/name matches an existing entity, then leave existing
   * properties are they are, but add new properties present in this command but
   * currently absent from the current graph entity.
   */
  APPEND_NEW__LEAVE_EXISTING,
  
  /**
   * If the UID or type/name matches an existing entity, then:
   * <ul>
   * <li>new properties specified in this command will be added;</li>
   * <li>existing properties on the graph node will be overwritten if they are
   * also specified in this graph modification command;</li>
   * <li>existing properties on the graph node that are NOT also specified in
   * this graph modification command will be left untouched.</li>
   * </ul>
   */
  APPEND_NEW__OVERWRITE_EXSITING,
  
  /**
   * If the UID or type/name matches an existing entity, then:
   * <ul>
   * <li>Any existing data properties on the graph entity will be removed.</li>
   * <li>Data properties specified by this graph modification command will be
   * set on the graph entity.</li>
   * </ul>
   */
  OVERWRITE_ALL;
}
