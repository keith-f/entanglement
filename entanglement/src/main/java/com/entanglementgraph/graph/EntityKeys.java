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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Keith Flanagan
 */
public class EntityKeys
  implements Serializable, Cloneable {

  public static boolean containsAtLeastOneUid(EntityKeys keyset) {
    return !keyset.uids.isEmpty();
  }

  public static boolean containsAtLeastOneKey(EntityKeys keyset) {
    return containsAtLeastOneUid(keyset);
  }

  /**
   * Returns true if two keysets are of the same type, and have identical UID content.
   * @param first
   * @param second
   * @param ignoreTypeField if set to true, ignores the EntityKeys 'type' field. This is useful if one of the keys
   *                        doesn't specify a type.
   * @return
   */
  public static boolean areKeysetsIdentical(EntityKeys first, EntityKeys second, boolean ignoreTypeField)
      throws IllegalArgumentException {
    if (!ignoreTypeField && !first.getType().equals(second.getType())) {
      throw new IllegalArgumentException("Attempt to compare keysets with different types: "+first+"; "+second);
    }
    return first.getUids().equals(second.getUids());
  }

  /**
   * Returns <code>true</code> if two keysets refer to the same graph entity. For this to be the case, the EntityKey's
   * types must match, and there must be at least one overlapping UID or name.
   * @param first
   * @param second
   * @param ignoreTypeField if set to true, ignores the EntityKeys 'type' field. This is useful if one of the keys
   *                        doesn't specify a type.
   * @return
   * @throws IllegalArgumentException
   */
  public static boolean doKeysetsReferToSameEntity(EntityKeys first, EntityKeys second, boolean ignoreTypeField)
      throws IllegalArgumentException {
    if (!ignoreTypeField && !first.getType().equals(second.getType())) {
      throw new IllegalArgumentException("Attempt to compare keysets with different types: "+first+"; "+second);
    }
    Set<String> uids = new HashSet<String>(first.getUids());
    uids.retainAll(second.getUids());
    return !uids.isEmpty();
  }


  protected String type;
  protected Set<String> uids;

  public EntityKeys() {
    uids = new HashSet<>();
  }


  public EntityKeys(String type, String id) {
    this();
    this.type = type;
    addUid(id);
  }


  @Override
  public String toString() {
    return String.format("EntityKeys{type='%s', uids=%s}", type, uids);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EntityKeys that = (EntityKeys) o;

    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (uids != null ? !uids.equals(that.uids) : that.uids != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (uids != null ? uids.hashCode() : 0);
    return result;
  }

  public void importIdentity(EntityKeys toImport)
    throws GraphModelException {
    if (!type.equals(toImport.type)) {
      throw new GraphModelException("Cannot merge graph identities where entities are of different types: "
          + type + " vs " + toImport.type);
    }
    uids.addAll(toImport.uids);
  }

  public void addUid(String uid)
  {
    uids.add(uid);
  }

  public void addUids(Collection<String> newUids)
  {
    uids.addAll(newUids);
  }

  public void addUids(String... newUids)
  {
    uids.addAll(Arrays.asList(newUids));
  }


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getUids() {
    return uids;
  }

  public void setUids(Set<String> uids) {
    this.uids = uids;
  }

}
