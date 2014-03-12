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

import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.BotLogger;

/**
 * @author Keith Flanagan
 */
public class IterateByTypeBenchmark extends AbstractBenchmark {
  private String nodeType;
  private GraphConnection graphConn;

  public IterateByTypeBenchmark(BotLogger logger, GraphConnection graphConn, String nodeType) {
    super(logger);
    this.graphConn = graphConn;
    this.nodeType = nodeType;
  }



  @Override
  protected void runBenchmark() throws Exception {
    //TODO reimplement...
//    long start = System.currentTimeMillis();
//    for (DBObject doc : graphConn.getNodeDao().iterateByType(nodeType)) {
//
//      final EntityKeys<? extends Node> pos = MongoUtils.parseKeyset(graphConn.getMarshaller(), doc);
//      long stop = System.currentTimeMillis();
//      if (pos != null) {
//        recordIterationComplete("default", stop-start);
//        start = System.currentTimeMillis();
//      }
//    }
  }


}
