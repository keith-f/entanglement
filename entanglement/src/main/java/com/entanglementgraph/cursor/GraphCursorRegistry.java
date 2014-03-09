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

import com.hazelcast.core.*;
import com.scalesinformatics.util.UidGenerator;

import java.util.HashMap;
import java.util.Map;

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

  /**
   * A Hazelcast event listener that can be used to forward events to Entanglement <code>CursorListener</code>
   * implementations
   */
  private static final class CursorListenerHazelcastForwarder implements EntryListener<String, GraphCursor> {
    private final CursorListener cursorListener;

    public CursorListenerHazelcastForwarder(CursorListener cursorListener) {
      this.cursorListener = cursorListener;
    }
    @Override
    public void entryAdded(EntryEvent<String, GraphCursor> event) {
    }

    @Override
    public void entryRemoved(EntryEvent<String, GraphCursor> event) {
    }

    @Override
    public void entryUpdated(EntryEvent<String, GraphCursor> event) {
      cursorListener.notifyNewPosition(event.getValue());
    }

    @Override
    public void entryEvicted(EntryEvent<String, GraphCursor> event) {
    }
  }

  private final HazelcastInstance hzInstance;
  private final IMap<String, GraphCursor> currentPositions;
  private final MultiMap<String, GraphCursor> cursorHistories;

  public GraphCursorRegistry(HazelcastInstance hzInstance) {
    this.hzInstance = hzInstance;
    currentPositions = hzInstance.getMap(HZ_CURSOR_POSITIONS_MAP);
    cursorHistories = hzInstance.getMultiMap(HZ_CURSOR_HISTORIES_MULTIMAP);
  }

  public void addCursor(GraphCursor cursor) {
//    currentPositions.put(cursor.getName(), cursor);
//    cursorHistories.put(cursor.getName(), cursor);
  }

  public void removeCursor(GraphCursor cursor) {
//    removeCursorByName(cursor.getName());
  }

  public void removeCursorByName(String cursorName) {
    currentPositions.remove(cursorName);
    cursorHistories.remove(cursorName);
  }

  /**
   * Registers a <code>CursorListener</code> so that it starts receiving updates on the movements of the specified
   * cursor. The same <code>CursorListener</code> instance may be registered with multiple cursors, if required.
   * @param cursorName the name of the cursor to attach the listener to.
   * @param listener the listener instance to associate.
   * @return an identifier for the association between <code>listener</code> and <code>cursorName</code>. This UID can
   * be used to detatch the listener from this cursor at a future time.
   */
  public String addCursorListener(String cursorName, CursorListener listener) {
    CursorListenerHazelcastForwarder hzForwarder = new CursorListenerHazelcastForwarder(listener);
    return getCurrentPositions().addEntryListener(hzForwarder , cursorName, true);
  }

  /**
   * Removes a previously registered listener so that it no longer receives events from a particular graph cursor.
   *
   * @param registrationUid the registration UID as previously returned by <code>addCursorListener</code>.
   */
  public void removeCursorListener(String registrationUid) {
    getCurrentPositions().removeEntryListener(registrationUid);
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
