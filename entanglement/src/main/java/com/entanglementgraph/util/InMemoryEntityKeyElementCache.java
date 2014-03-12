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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of <code>EntityKeyElementCache</code> that stores elements in RAM.
 *
 * User: keith
 * Date: 13/08/13; 16:36
 *
 * @author Keith Flanagan
 */
public class InMemoryEntityKeyElementCache<T extends Content> implements EntityKeyElementCache<T> {
  private final Map<String, Set<String>> seenUids;

  public InMemoryEntityKeyElementCache() {
    seenUids = new HashMap<>();
  }

  @Override
  public void cacheElementsOf(EntityKeys<T> key) {
    Set<String> uids = seenUids.get(key.getType());
    if (uids == null) {
      uids = new HashSet<>();
      seenUids.put(key.getType(), uids);
    }
    uids.addAll(key.getUids());
  }

  @Override
  public boolean seenElementOf(EntityKeys<T> key) {
    Set<String> uids = seenUids.get(key.getType());
    if (uids == null) {
      return false;
    }
    for (String uid : key.getUids()) {
      if (uids.contains(uid)) {
        return true;
      }
    }
    return false;
  }
}
