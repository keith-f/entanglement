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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 10/03/2013
 * Time: 21:09
 * To change this template use File | Settings | File Templates.
 */
public class EntityKeys<E extends Content>
  implements Serializable, Cloneable {

  public static Iterable<DBObject> buildKeyIndexes(String prefix) {
    List<DBObject> indexes = new ArrayList<>(3);
    indexes.add(new BasicDBObject(String.format("%s.uids", prefix), 1));
    indexes.add(new BasicDBObject(String.format("%s.names", prefix), 1));
    indexes.add(new BasicDBObject(String.format("%s.type", prefix), 1));

    //Note that if both of the following indexes are present, MongoDB queries run *really* slowly (lots of scans)
    //indexes.add(new BasicDBObject(String.format("%s.type", prefix), 1).append(String.format("%s.names", prefix), 1));
    //indexes.add(new BasicDBObject(String.format("%s.names", prefix), 1).append(String.format("%s.type", prefix), 1));
    /*
     * ^^^ Disabling the above for the moment, since a compound index doesn't really give us much advantage here anyway.
     * For current datasets, names are sufficiently unique that only a few objects are scanned. We can make do with
     * just the 'names' index only.
     */
    return indexes;
  }

  public static EntityKeys createWithType(String type) {
    EntityKeys keyset = new EntityKeys();
    keyset.setType(type);
    return keyset;
  }

  public static boolean containsAtLeastOneName(EntityKeys keyset) {
    return !keyset.names.isEmpty();
  }

  public static boolean containsAtLeastOneUid(EntityKeys keyset) {
    return !keyset.uids.isEmpty();
  }

  public static boolean containsAtLeastOneKey(EntityKeys keyset) {
    return !keyset.names.isEmpty() || !keyset.uids.isEmpty();
  }

  /**
   * Returns true if two keysets are of the same type, and have identical UID and name content.
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
    return first.getUids().equals(second.getUids())
        && first.getNames().equals(second.getNames());
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
    if (!uids.isEmpty()) return true;

    Set<String> names = new HashSet<String>(first.getNames());
    names.retainAll(second.getNames());
    return !names.isEmpty();
  }


  protected String type;
  protected Set<String> uids;
  protected Set<String> names;

  public EntityKeys() {
    uids = new HashSet<>();
    names = new HashSet<>();
  }

  public EntityKeys(String uid) {
    this();
    uids.add(uid);
  }

  public EntityKeys(String type, String name) {
    this();
    this.type = type;
    names.add(name);
  }

  public EntityKeys(String type, Collection<String> names) {
    this();
    this.type = type;
    names.addAll(names);
  }

  public EntityKeys(String type, String uid, String name) {
    this();
    this.type = type;
    uids.add(uid);
    names.add(name);
  }

  @Override
  public EntityKeys<E> clone() {
    EntityKeys<E> clone = new EntityKeys<>();
    clone.setType(getType());
    clone.getUids().addAll(getUids());
    clone.getNames().addAll(getNames());
    return clone;
  }

  @Override
  public String toString() {
    return String.format("EntityKeys{type='%s', uids=%s, names=%s}", type, uids, names);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EntityKeys that = (EntityKeys) o;

    if (names != null ? !names.equals(that.names) : that.names != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (uids != null ? !uids.equals(that.uids) : that.uids != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (uids != null ? uids.hashCode() : 0);
    result = 31 * result + (names != null ? names.hashCode() : 0);
    return result;
  }

  public void importIdentity(EntityKeys toImport)
    throws GraphModelException {
    if (!type.equals(toImport.type)) {
      throw new GraphModelException("Cannot merge graph identities where entities are of different types: "
          + type + " vs " + toImport.type);
    }
    uids.addAll(toImport.uids);
    names.addAll(toImport.names);
  }

  public void addName(String name)
  {
    if (name != null) {
      names.add(name);
    }
  }

  public void addNames(Collection<String> newNames)
  {
    names.addAll(newNames);
  }

  public void addNames(String... newNames)
  {
    names.addAll(Arrays.asList(newNames));
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

  public Set<String> getNames() {
    return names;
  }

  public void setNames(Set<String> names) {
    this.names = names;
  }
}
