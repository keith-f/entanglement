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
  private static final String LOGGER_NAME = GraphConnectionListenerLogger.class.getSimpleName();

  private final BotLogger logger;
  public GraphConnectionListenerLogger(BotLogger botLogger) {
    this.logger = botLogger;
  }


  @Override
  public void entryAdded(EntryEvent<String, GraphConnectionDetails> event) {
    logger.infoln("Acknowledging new GraphConnectionDetails object: %s => %s (added by host: %s)",
        event.getName(), event.getValue().toString(), event.getMember());
  }

  @Override
  public void entryRemoved(EntryEvent<String, GraphConnectionDetails> event) {
    logger.infoln("Acknowledging removal of GraphConnectionDetails object: %s (removed by host: %s)",
        event.getName(), event.getMember());
  }

  @Override
  public void entryUpdated(EntryEvent<String, GraphConnectionDetails> event) {
    logger.infoln("Acknowledging revised GraphConnectionDetails object: %s => %s (updated by host: %s)",
        event.getName(), event.getValue().toString(), event.getMember());
  }

  @Override
  public void entryEvicted(EntryEvent<String, GraphConnectionDetails> event) {
    logger.infoln("Acknowledging eviction GraphConnectionDetails object: %s (evicted by host: %s)",
        event.getName(), event.getMember());
  }
}
