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

package com.entanglementgraph.irc.commands.graph;

import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.graph.commands.BranchImport;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

/**
 * @author Keith Flanagan
 */
public class ImportGraphCommand extends AbstractEntanglementCommand {
  private String sourceConnName;
  private String destinationConnName;

  @Override
  public String getDescription() {
    return "Copies (plays back the entire transaction history) of a 'source' graph into a 'destination' graph. " +
        "Useful for combining different datasets for integration. Entity keysets that clash (i.e. have the same" +
        "UID and/or type+name) will be merged according to the MergePolicy they were originally committed with.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("source", String.class, "The name of the graph connection to be imported"));
    params.add(new RequiredParam("destination", String.class, "The name of the graph connection that is to recieve imported entities"));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    sourceConnName = parsedArgs.get("source").getStringValue();
    destinationConnName = parsedArgs.get("destination").getStringValue();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {

    try {
      GraphConnection sourceConn = entRuntime.createGraphConnectionFor(sourceConnName);
      GraphConnection destinationConn = entRuntime.createGraphConnectionFor(destinationConnName);
      if (sourceConn == null) throw new UserException(sender, "No graph connection exists with the name: "+sourceConnName);
      if (destinationConn == null) throw new UserException(sender, "No graph connection exists with the name: "+destinationConnName);


      BranchImport graphOp = new BranchImport(sourceConn.getGraphName());
      TxnUtils.submitAsTxn(destinationConn, graphOp);

      logger.println("Import of %s to %s complete", sourceConnName, destinationConnName);
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
