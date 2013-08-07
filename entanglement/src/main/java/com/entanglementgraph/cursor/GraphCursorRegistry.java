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
package com.entanglementgraph.cursor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;

/**
 * A node-local object that stores graph cursor information in distributed Hazelcast data structures.
 *
 * User: keith
 * Date: 23/07/13; 11:57
 *
 * @author Keith Flanagan
 */
public class GraphCursorRegistry {
  static final String HZ_CURSOR_POSITIONS_MAP = GraphCursorRegistry.class.getName()+".current_cursors_positions";
  static final String HZ_CURSOR_HISTORIES_MULTIMAP = GraphCursorRegistry.class.getName()+".cursor_histories";

  private final HazelcastInstance hzInstance;
  private final IMap<String, GraphCursor> currentPositions;
  private final MultiMap<String, GraphCursor> cursorHistories;

  public GraphCursorRegistry(HazelcastInstance hzInstance) {
    this.hzInstance = hzInstance;
    currentPositions = hzInstance.getMap(HZ_CURSOR_POSITIONS_MAP);
    cursorHistories = hzInstance.getMultiMap(HZ_CURSOR_HISTORIES_MULTIMAP);
  }

  public void addCursor(GraphCursor cursor) {
    currentPositions.put(cursor.getName(), cursor);
    cursorHistories.put(cursor.getName(), cursor);
  }

  public void removeCursor(GraphCursor cursor) {
    removeCursorByName(cursor.getName());
  }

  public void removeCursorByName(String cursorName) {
    currentPositions.remove(cursorName);
    cursorHistories.remove(cursorName);
  }

  public GraphCursor getCursorCurrentPosition(String cursorName) {
    return currentPositions.get(cursorName);
  }

  public IMap<String, GraphCursor> getCurrentPositions() {
    return currentPositions;
  }

  public MultiMap<String, GraphCursor> getCursorHistories() {
    return cursorHistories;
  }
}
