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
package com.entanglementgraph.util;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.EntityKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of <code>EntityKeyElementCache</code> that stores elements in RAM.
 *
 * User: keith
 * Date: 13/08/13; 16:36
 *
 * @author Keith Flanagan
 */
public class InMemoryEntityKeyElementCacheWithLookups<T extends Content, U> implements EntityKeyElementCacheWithLookups<T, U> {
//  private final Map<String, U> seenUids;               // A map of UID to user object
  private final Map<String, Map<String, U>> seenUids;    // A map of type -> UID -> user object

  public InMemoryEntityKeyElementCacheWithLookups() {
    seenUids = new HashMap<>();
//    seenNames = new HashMap<>();
  }

  @Override
  public void cacheElementsAndAssociateWithObject(EntityKeys<T> key, U userObject) {
//    for (String uid : key.getUids()) {
//      seenUids.put(uid, userObject);
//    }

    Map<String, U> uidToUserObject = seenUids.get(key.getType());
    if (uidToUserObject == null) {
      uidToUserObject = new HashMap<>();
      seenUids.put(key.getType(), uidToUserObject);
    }
    for (String uid : key.getUids()) {
      uidToUserObject.put(uid, userObject);
    }
  }

  @Override
  public boolean seenElementOf(EntityKeys<T> key) {
    for (String uid : key.getUids()) {
      if (seenUids.containsKey(uid)) {
        return true;
      }
    }

    Map<String, U> uids = seenUids.get(key.getType());
    if (uids == null) {
      return false;
    }
    for (String uid : key.getUids()) {
      if (uids.containsKey(uid)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public U getUserObjectFor(EntityKeys<T> key) {
    Map<String, U> uidToUserObject = seenUids.get(key.getType());
    if (uidToUserObject == null) {
      return null;
    }
    for (String uid : key.getUids()) {
      if (uidToUserObject.containsKey(uid)) {
        return uidToUserObject.get(uid);
      }
    }
    return null;
  }
}
