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

import com.entanglementgraph.examples.biggraph.BigGraphBuilder;
import com.entanglementgraph.irc.commands.AbstractEntanglementGraphCommand;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 *
 *
 * @author Keith Flanagan
 */
public class RunBigRandomGraphBenchmarkCommand extends AbstractEntanglementGraphCommand {
  private String connName;
  private File logFile;
  private int commitThreads;
  private int maxUpdatesPerDocument;
  private int numRootNodes;
  private double probabilityOfGeneratingChildNode;
  private int maxChildNodesPerRoot;

  @Override
  public String getDescription() {
    return "A simple benchmark that times progress of building a large graph.";
  }


  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("conn", String.class, "Name of the graph connection to use"));
    params.add(new OptionalParam("log-file", String.class, "big-random-graph.csv", "Name of a file to write benchmark timing data to."));
    params.add(new OptionalParam("num-root-nodes", Integer.class, "500000", "The number of root nodes to generate."));
    params.add(new OptionalParam("max-child-nodes-per-root", Integer.class, "15", "The maximum number of child nodes that will be generated for each root node."));
    params.add(new OptionalParam("probability-of-child-node", Double.class, "0.1", "The probability that each child node generation operation will succeed."));

    params.add(new OptionalParam("commit-threads", Integer.class, "1", "The maximum number of threads used to process the generated graph operations."));
    params.add(new OptionalParam("max-items-per-doc", Integer.class, "1000", "The maximum number of graph operations per document."));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    connName = parsedArgs.get("conn").getStringValue();
    logFile = new File(parsedArgs.get("log-file").getStringValue());
    numRootNodes = parsedArgs.get("num-root-nodes").parseValueAsInteger();
    maxChildNodesPerRoot = parsedArgs.get("max-child-nodes-per-root").parseValueAsInteger();
    probabilityOfGeneratingChildNode = parsedArgs.get("probability-of-child-node").parseValueAsDouble();

    commitThreads = parsedArgs.get("commit-threads").parseValueAsInteger();
    maxUpdatesPerDocument = parsedArgs.get("max-items-per-doc").parseValueAsInteger();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      GraphConnection graphConn = entRuntime.createGraphConnectionFor(connName);
      logger.infoln("Running 'big random graph' benchmarks ... ");
      BufferedWriter bw = new BufferedWriter(new FileWriter(logFile));
      BigGraphBuilder builder = new BigGraphBuilder(logger, graphConn, bw,
          commitThreads, maxUpdatesPerDocument,
          numRootNodes, probabilityOfGeneratingChildNode, maxChildNodesPerRoot);
      builder.run();
      bw.flush();
      bw.close();
      logger.println("Done.");
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
