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
package com.entanglementgraph.irc;

import com.entanglementgraph.cursor.GraphCursor;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IList;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.GenericIrcBot;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A simple logger that prints out when an Entanglement bot acknowledges a new GraphCursor object being
 * added to an EntanglementRuntime instance. This may happen on a local or remote machine.
 *
 * User: keith
 * Date: 17/07/13; 12:15
 *
 * @author Keith Flanagan
 */
public class GraphCursorRegistryListenerLogger implements EntryListener<String, IList<GraphCursor.HistoryItem>> {
  private static final Logger logger = Logger.getLogger(GraphCursorRegistryListenerLogger.class.getName());
  private static final String LOGGER_PREFIX = "Graph cursor listener";

  private final GenericIrcBot<EntanglementRuntime> bot;
  private final String channel;
  private final BotLogger botLogger;
  private final Map<String, GraphCursorHistoryChangeListenerLogger> cursorMovementListeners;

  public GraphCursorRegistryListenerLogger(GenericIrcBot<EntanglementRuntime> bot, String channel) {
    this.cursorMovementListeners = new HashMap<>();
    this.bot = bot;
    this.channel = channel;
    if (channel != null) {
      botLogger = new BotLogger(bot, channel, LOGGER_PREFIX, LOGGER_PREFIX);
    } else {
      botLogger = null;
    }
  }

  private void log(String text) {
    if (botLogger == null) {
      logger.info(text);
    } else {
      botLogger.infoln(text);
    }
  }

  @Override
  public void entryAdded(EntryEvent<String, IList<GraphCursor.HistoryItem>> event) {
    log(String.format("Acknowledging new GraphCursor object: %s(%s) with %d history items (added by host: %s)",
        event.getKey(), event.getName(), event.getValue().size(), event.getMember()));

    // Add a secondary listener to log cursor movements as well
    GraphCursorHistoryChangeListenerLogger movementLogger = new GraphCursorHistoryChangeListenerLogger(bot, channel);
    event.getValue().addItemListener(movementLogger, true);
    cursorMovementListeners.put(event.getKey(), movementLogger);

  }

  @Override
  public void entryRemoved(EntryEvent<String, IList<GraphCursor.HistoryItem>> event) {
    log(String.format("Acknowledging removal of GraphCursor object: %s with %d history items (added by host: %s)",
        event.getName(), event.getValue().size(), event.getMember()));
    cursorMovementListeners.remove(event.getKey());
  }

  @Override
  public void entryUpdated(EntryEvent<String, IList<GraphCursor.HistoryItem>> event) {
    log(String.format("Acknowledging revised GraphCursor object: %s with %d history items (added by host: %s)",
        event.getName(), event.getValue().size(), event.getMember()));
  }

  @Override
  public void entryEvicted(EntryEvent<String, IList<GraphCursor.HistoryItem>> event) {
    log(String.format("Acknowledging eviction GraphCursor object: %s with %d history items (added by host: %s)",
        event.getName(), event.getValue().size(), event.getMember()));
    cursorMovementListeners.remove(event.getKey());
  }
}
