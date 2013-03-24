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

package com.entanglementgraph.graph.data;

import com.entanglementgraph.graph.GraphModelException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 10/03/2013
 * Time: 21:09
 * To change this template use File | Settings | File Templates.
 */
public class EntityKeys {

  public static Iterable<DBObject> buildKeyIndexes(String prefix) {
    List<DBObject> indexes = new ArrayList<>(3);
    indexes.add(new BasicDBObject(String.format("%s.uids", prefix), 1));
    indexes.add(new BasicDBObject(String.format("%s.names", prefix), 1));
    indexes.add(new BasicDBObject(String.format("%s.type", prefix), 1).append(String.format("%s.names", prefix), 1));
    return indexes;
  }

  public static EntityKeys createWithType(String type) {
    EntityKeys keyset = new EntityKeys();
    keyset.setType(type);
    return keyset;
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

  public EntityKeys(String type, String uid, String name) {
    this();
    this.type = type;
    uids.add(uid);
    names.add(name);
  }

  @Override
  public String toString() {
    return "EntityRef{" +
        "type='" + type + '\'' +
        ", uids=" + uids +
        ", names=" + names +
        '}';
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
    names.add(name);
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
