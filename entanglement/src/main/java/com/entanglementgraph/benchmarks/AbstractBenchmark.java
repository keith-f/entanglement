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

package com.entanglementgraph.benchmarks;

import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.scalesinformatics.uibot.BotLogger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
abstract public class AbstractBenchmark implements Benchmark {

  private static final int DEFAULT_REPORT_EVERY = 1000;

  protected final BotLogger logger;
  private final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();

  private int totalIterationCount;
  private final Map<String, Long> categoryToTime;
  private final Map<String, Integer> categoryToCount;

  private long startTimestamp;
  private long endTimestamp;
  private long totalTimeMs;

  private int reportEvery;

  private Exception error;

  public AbstractBenchmark(BotLogger logger) {
    totalIterationCount = 0;
    totalTimeMs = 0;
    categoryToTime = new HashMap<>();
    categoryToCount = new HashMap<>();
    reportEvery = DEFAULT_REPORT_EVERY;
    this.logger = logger;
  }

  public void recordIterationComplete(String category, long itrDurationMs) {
    totalIterationCount++;
    Long timeMs = categoryToTime.get(category);
    if (timeMs == null) {
      timeMs = 0l;
    }
    timeMs = timeMs + itrDurationMs;
    categoryToTime.put(category, timeMs);

    Integer count = categoryToCount.get(category);
    if (count == null) {
      count = 0;
    }
    count++;
    categoryToCount.put(category, count);

    if (totalIterationCount % reportEvery == 0) {
      reportOngoingStatus();
    }
  }

  private void reportOngoingStatus() {
    logger.println(
        entFormat.append("Iteration: ").format(totalIterationCount).toString()
    );
  }

  public void printFinalReport() {
    double seconds = totalTimeMs / 1000d;

    logger.println("Benchmark: %s --> %s == %s total iterations took %s seconds.",
        new Date(startTimestamp).toString(), new Date(endTimestamp).toString(),
        entFormat.format(totalIterationCount).toString(), entFormat.format(seconds).toString());
  }

  @Override
  public void run() {
    startTimestamp = System.currentTimeMillis();
    try {
      runBenchmark();
    } catch (Exception e) {
      e.printStackTrace();
      error = e;
    }
    endTimestamp = System.currentTimeMillis();
    totalTimeMs = endTimestamp - startTimestamp;
  }

  abstract protected void runBenchmark() throws Exception;

  public int getTotalIterationCount() {
    return totalIterationCount;
  }

  public void setTotalIterationCount(int totalIterationCount) {
    this.totalIterationCount = totalIterationCount;
  }

  public Map<String, Long> getCategoryToTime() {
    return categoryToTime;
  }

  public Map<String, Integer> getCategoryToCount() {
    return categoryToCount;
  }

  public long getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(long startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  public long getEndTimestamp() {
    return endTimestamp;
  }

  public void setEndTimestamp(long endTimestamp) {
    this.endTimestamp = endTimestamp;
  }

  public long getTotalTimeMs() {
    return totalTimeMs;
  }

  public void setTotalTimeMs(long totalTimeMs) {
    this.totalTimeMs = totalTimeMs;
  }

  public Exception getError() {
    return error;
  }

  public void setError(Exception error) {
    this.error = error;
  }
}
