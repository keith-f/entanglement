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
package com.entanglementgraph.irc.commands.benchmarks;

import com.entanglementgraph.benchmarks.Benchmark;
import com.entanglementgraph.benchmarks.CreateAndDestroyCursorsBenchmark;
import com.entanglementgraph.benchmarks.IterateByTypeBenchmark;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.LinkedList;
import java.util.List;

/**
 * User: keith
 * Date: 23/08/13; 13:43
 *
 * @author Keith Flanagan
 */
public class RunBenchmarksCommand extends AbstractEntanglementCommand<EntanglementRuntime> {
//  private static final Logger logger = Logger.getLogger(GeneCentricMethylationSummaryChartCommand.class.getName());

  @Override
  public String getDescription() {
    return "A simple benchmark that times progress on iterating over nodes of a particular type. In this case, Gene nodes.";
  }


  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new OptionalParam("iterate-by-type.type", String.class, null, "Set to the node type to run with "+ IterateByTypeBenchmark.class.getSimpleName()));
    return params;
  }

  public RunBenchmarksCommand() {
    super(Requirements.GRAPH_CONN_NEEDED);//, Requirements.CURSOR_NEEDED);
  }



  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String nodeType = parsedArgs.get("iterate-by-type.type").getStringValue();

    List<Benchmark> benchmarks = new LinkedList<>();
    try {

      if (nodeType != null) {
        IterateByTypeBenchmark benchmark = new IterateByTypeBenchmark(
            new BotLoggerIrc(bot, channel, IterateByTypeBenchmark.class.getSimpleName()), graphConn, nodeType);
        benchmarks.add(benchmark);
      }
      benchmarks.add(new CreateAndDestroyCursorsBenchmark(logger, state.getUserObject(), 10000));

      logger.infoln("Running benchmarks ... ");
      for (Benchmark benchmark : benchmarks) {
        logger.infoln("Running: %s", benchmark.getClass().getName());
        benchmark.run();
        benchmark.printFinalReport();
      }

      Message msg = new Message(channel);
      msg.println("Done.");

      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
