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

import com.entanglementgraph.irc.data.GraphConnectionDetails;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.GenericIrcBot;

import java.util.logging.Logger;

/**
 * A simple logger that prints out when an Entanglement bot acknowledges a new GraphConnectionDetails object being
 * added to a runtime. This may happen on a local or remote machine.
 *
 * User: keith
 * Date: 17/07/13; 12:15
 *
 * @author Keith Flanagan
 */
public class GraphConnectionListenerLogger implements EntryListener<String, GraphConnectionDetails> {
  private static final Logger logger = Logger.getLogger(GraphConnectionListenerLogger.class.getName());
  private static final String LOGGER_PREFIX = "Graph connection listener";

  private final BotLogger botLogger;
  public GraphConnectionListenerLogger(GenericIrcBot<EntanglementRuntime> bot, String channel) {
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
  public void entryAdded(EntryEvent<String, GraphConnectionDetails> event) {
    log(String.format("Acknowledging new GraphConnectionDetails object: %s => %s (added by host: %s)",
        event.getName(), event.getValue().toString(), event.getMember()));
  }

  @Override
  public void entryRemoved(EntryEvent<String, GraphConnectionDetails> event) {
    log(String.format("Acknowledging removal of GraphConnectionDetails object: %s (removed by host: %s)",
        event.getName(), event.getMember()));
  }

  @Override
  public void entryUpdated(EntryEvent<String, GraphConnectionDetails> event) {
    log(String.format("Acknowledging revised GraphConnectionDetails object: %s => %s (updated by host: %s)",
        event.getName(), event.getValue().toString(), event.getMember()));
  }

  @Override
  public void entryEvicted(EntryEvent<String, GraphConnectionDetails> event) {
    log(String.format("Acknowledging eviction GraphConnectionDetails object: %s (evicted by host: %s)",
        event.getName(), event.getMember()));
  }
}
