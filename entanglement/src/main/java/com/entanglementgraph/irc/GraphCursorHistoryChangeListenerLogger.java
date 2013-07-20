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
import com.hazelcast.core.*;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.GenericIrcBot;

import java.util.logging.Logger;

/**
 * A simple logger that prints out when an Entanglement bot acknowledges changes to the location history of a
 * <code>GraphCursor</code>.
 *
 * User: keith
 * Date: 17/07/13; 12:15
 *
 * @author Keith Flanagan
 */
public class GraphCursorHistoryChangeListenerLogger implements ItemListener<GraphCursor.HistoryItem> {
  private static final Logger logger = Logger.getLogger(GraphCursorHistoryChangeListenerLogger.class.getName());
  private static final String LOGGER_PREFIX = "Graph cursor listener";

  private final BotLogger botLogger;

  public GraphCursorHistoryChangeListenerLogger(GenericIrcBot<EntanglementRuntime> bot, String channel) {
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
  public void itemAdded(ItemEvent<GraphCursor.HistoryItem> event) {
    GraphCursor.HistoryItem historyItem = event.getItem();
    GraphCursor cursor = historyItem.getAssociatedCursor();

    if (cursor.getCursorHistoryIdx() > 0) {
      GraphCursor previousCursor = cursor.getHistory().get(cursor.getCursorHistoryIdx()-1).getAssociatedCursor();
      log(String.format("Acknowledging GraphCursor %s moved from %s ==> %s (added by host: %s)",
          cursor.getName(), previousCursor.getCurrentNode(), cursor.getCurrentNode(),
          event.getMember()));
    } else {
      log(String.format("Acknowledging GraphCursor %s moved to %s (added by host: %s)",
          cursor.getName(), cursor.getCurrentNode(), event.getMember()));
    }
  }


  @Override
  public void itemRemoved(ItemEvent<GraphCursor.HistoryItem> event) {
    GraphCursor.HistoryItem historyItem = event.getItem();
    GraphCursor cursor = historyItem.getAssociatedCursor();
    log(String.format("Acknowledging GraphCursor %s moved to %s (added by host: %s)",
        cursor.getName(), cursor.getCurrentNode(), event.getMember()));
  }
}
