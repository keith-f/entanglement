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

import com.entanglementgraph.graph.data.EntityKeys;

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
public class InMemoryEntityKeyElementCache<T> implements EntityKeyElementCache<T> {
  private final Set<String> seenUids;
  private final Map<String, Set<String>> seenNames;

  public InMemoryEntityKeyElementCache() {
    seenUids = new HashSet<>();
    seenNames = new HashMap<>();
  }

  @Override
  public void cacheElementsOf(EntityKeys<T> key) {
    seenUids.addAll(key.getUids());

    if (key.getNames().isEmpty()) {
      return;
    }

    Set<String> names = seenNames.get(key.getType());
    if (names == null) {
      names = new HashSet<>();
      seenNames.put(key.getType(), names);
    }
    names.addAll(key.getNames());
  }

  @Override
  public boolean seenElementOf(EntityKeys<T> key) {
    for (String uid : key.getUids()) {
      if (seenUids.contains(uid)) {
        return true;
      }
    }

    Set<String> names = seenNames.get(key.getType());
    if (names == null) {
      return false;
    }
    for (String name : key.getNames()) {
      if (names.contains(name)) {
        return true;
      }
    }
    return false;
  }
}