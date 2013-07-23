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
import com.hazelcast.core.HazelcastInstance;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.GenericIrcBot;

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
public class GraphCursorPositionListenerLogger implements EntryListener<String, GraphCursor> {
  private static final Logger logger = Logger.getLogger(GraphCursorPositionListenerLogger.class.getName());
  private static final String LOGGER_PREFIX = "Graph cursor listener";

  private final GenericIrcBot<EntanglementRuntime> bot;
  private final String channel;
  private final BotLogger botLogger;
  private final HazelcastInstance hzInstance;

  public GraphCursorPositionListenerLogger(GenericIrcBot<EntanglementRuntime> bot, String channel,
                                           HazelcastInstance hzInstance) {
    this.bot = bot;
    this.channel = channel;
    this.hzInstance = hzInstance;
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
  public void entryAdded(EntryEvent<String, GraphCursor> event) {
    GraphCursor cursor = event.getValue();
    log(String.format("Acknowledging new GraphCursor: %s(%s) at position %s (added by host: %s)",
        event.getKey(), event.getName(), cursor.getPosition(), event.getMember()));

    // This new GraphCursor has been added to a distributed collection. Make sure it also informs us of future changes.
    cursor.setAutoRecordContext(hzInstance);
  }

  @Override
  public void entryRemoved(EntryEvent<String, GraphCursor> event) {
    GraphCursor cursor = event.getValue();
    log(String.format("Acknowledging removal of GraphCursor: %s(%s) at position %s (by host: %s)",
        event.getKey(), event.getName(), cursor.getPosition(), event.getMember()));
  }

  @Override
  public void entryUpdated(EntryEvent<String, GraphCursor> event) {
    GraphCursor previousCursor = event.getOldValue();
    GraphCursor cursor = event.getValue();
    log(String.format("Acknowledging GraphCursor %s moved from %s ==> %s (by host: %s)",
        cursor.getName(), previousCursor.getPosition(), cursor.getPosition(),
        event.getMember()));
  }

  @Override
  public void entryEvicted(EntryEvent<String,GraphCursor> event) {
    GraphCursor cursor = event.getValue();
    log(String.format("Acknowledging removal of GraphCursor: %s(%s) at position %s (by host: %s)",
        event.getKey(), event.getName(), cursor.getPosition(), event.getMember()));
  }
}
