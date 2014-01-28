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

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.util.UidGenerator;

/**
 * This benchmark simply repeatedly creates and destroys graph cursors.
 *
 * @author Keith Flanagan
 */
public class CreateAndDestroyCursorsBenchmark extends AbstractBenchmark {
  private EntanglementRuntime runtime;
  private int iterations;
//  private String nodeType;
//  private GraphConnection graphConn;

  public CreateAndDestroyCursorsBenchmark(BotLogger logger, EntanglementRuntime runtime,
                                          int iterations) {
    super(logger);
    this.runtime = runtime;
    this.iterations = iterations;
  }



  @Override
  protected void runBenchmark() throws Exception {
    long start = System.currentTimeMillis();
    for (int i=0; i<iterations; i++) {
      //Create temporary graph cursor
      GraphCursor tmpCursor = new GraphCursor(UidGenerator.generateUid(),
          new EntityKeys<Node>(UidGenerator.generateUid()));
      runtime.getCursorRegistry().addCursor(tmpCursor);
      //Remove temporary graph cursor
      runtime.getCursorRegistry().removeCursor(tmpCursor);

      long stop = System.currentTimeMillis();
      recordIterationComplete("default", stop-start);
      start = System.currentTimeMillis();
    }
  }


}
