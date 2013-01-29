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

package uk.ac.ncl.aries.entanglement.revlog.commands;

/**
 *
 * @author Keith Flanagan
 */
public enum ModificationPolicy
{
  /**
   * Causes a new graph entity to be created only if no graph entity with the 
   * same UID already exists. If an entity with the same UID does exist, then
   * the <code>MergePolicy</code> will be checked to determine what action
   * must be taken, if any.
   * 
   * For this creation policy type, the following graph entity properties are
   * required:
   * <ul>
   * <li><code>UID</code></li>
   * <li><code>Entity type</code></li>
   * </ul>
   * 
   * The following properties are optional:
   * <ul>
   * <li><code>name</code></li>
   * <li>... any other domain/application-specific data properties ...</li>
   * </ul>
   * 
   * Note that a given UID and type/name pair (if specified) are
   * supposed to uniquely identify the same graph entity. You cannot submit
   * two graph entity creation commands with the same UID, but different
   * type/name combination. This case is checked for, and an error will result
   * if attempted. <b>In short: either don't specify a 'name' for one or more 
   * commands relating to a specific graph entity, or specify the same
   * UID/type/name with each command.</b>
   */
  CREATE_OR_MODIFY_BY_UID,

  /**
   * Causes a new graph entity to be created only if no graph entity with the 
   * same type and name already exists. If an entity with the same type/name
   * combination does exist, then the <code>MergePolicy</code> will be checked
   * to determine what action must be taken, if any.
   * 
   * For this creation policy type, the following graph entity properties are
   * required:
   * <ul>
   * <li><code>Entity type</code></li>
   * <li><code>name</code></li>
   * </ul>
   * 
   * The following properties are optional:
   * <ul>
   * <li><code>UID</code></li>
   * <li>... any other domain/application-specific data properties ...</li>
   * </ul>
   * 
   * Graph entities must have a UID, so although specification of one 
   * here is optional, if you don't specify a UID, one will be created for you
   * when the transaction is played.
   * 
   * Note that a given UID and type/name pair are
   * supposed to uniquely identify the same graph entity. You cannot submit
   * two graph entity creation commands with the same UID, but different
   * type/name combination. This case is checked for, and an error will result
   * if attempted. <b>In short: either don't specify a UID for commands 
   * relating to a specific graph entity, or specify the same UID/type/name 
   * with each command.</b>
   */
  CREATE_OR_MODIFY_BY_NAME,

}
