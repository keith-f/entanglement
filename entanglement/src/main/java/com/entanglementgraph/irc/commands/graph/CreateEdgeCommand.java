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

import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.commands.AbstractEntanglementGraphCommand;
import com.entanglementgraph.graph.commands.EdgeModification;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.*;

/**
 * @author Keith Flanagan
 *
 * //TODO not yet fully implemented
 */
public class CreateEdgeCommand extends AbstractEntanglementGraphCommand {
  private String type;
  private String entityName;
  private Map<String, String> attributes;

  @Override
  public String getDescription() {
    return "Creates or updates an edge in the currently active graph.";
  }


  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();

    params.add(new RequiredParam("type", String.class, "The type name of the edge to create/modify"));
    params.add(new RequiredParam("entityName", String.class, "A unique name for the edge to create/modify"));
    params.add(new RequiredParam("fromNodeType", String.class, "The type name of the 'from' node that this edge should connect to"));
    params.add(new RequiredParam("fromNodeName", String.class, "The type-unique name of the 'from' node that this edge should connect to"));
    params.add(new RequiredParam("toNodeType", String.class, "The type name of the 'to' node that this edge should connect to"));
    params.add(new RequiredParam("toNodeName", String.class, "The type-unique name of the 'to' node that this edge should connect to"));
    params.add(new OptionalParam("{ key=value pairs }", null, "A set of key=value pairs that will be added to the edge as attributes"));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    type = parsedArgs.get("type").getStringValue();
    entityName = parsedArgs.get("entityName").getStringValue();
    //TODO from/to

    // Parse annotations
    attributes = parseAttributes(args);
    //FIXME do this properly - remove entries that are used as part of the command.
    attributes.remove("type");
    attributes.remove("entityName");
    //TODO from/to
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    try {
      bot.infoln(channel, "Going to create an edge: %s/%s with properties: %s", type, entityName, attributes);

      Edge edge = new Edge();
      edge.getKeys().setType(type);
      edge.getKeys().addName(entityName);
      //TODO from/to

      // Serialise the basic Node object ot a MongoDB object.
      BasicDBObject edgeObj = entRuntime.getMarshaller().serialize(edge);
      // Add further custom properties
      for (Map.Entry<String, String> attr : attributes.entrySet()) {
        edgeObj.append(attr.getKey(), attr.getValue());
      }

      EdgeModification edgeUpdateCommand = new EdgeModification();
      edgeUpdateCommand.setEdge(edgeObj);
      edgeUpdateCommand.setMergePol(MergePolicy.APPEND_NEW__LEAVE_EXISTING); //FIXME policy should be user-configurable

      writeOperation(graphConn, edgeUpdateCommand);
      logger.println("Edge created/updated: %s", entityName);
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

  private Map<String, String> parseAttributes(String[] args) {
    Map<String, String> attributes = new HashMap<>();
    for (String arg : args) {
      if (arg.contains("=")) {
        try {
          StringTokenizer st = new StringTokenizer(arg, "=");
          String name = st.nextToken();
          String value = st.nextToken();
          attributes.put(name, value);
        } catch (Exception e) {
          bot.errln(channel, "Incorrectly formatted key=value pair. Ignoring: %s", arg);
        }
      }
    }
    return attributes;
  }

  private void writeOperation(GraphConnection conn, GraphOperation graphOp) throws EntanglementBotException {
    List<GraphOperation> ops = new ArrayList<>(1);
    ops.add(graphOp);
    writeOperations(conn, ops);
  }

  private void writeOperations(GraphConnection conn, List<GraphOperation> ops) throws EntanglementBotException {
    bot.debugln(channel, "Committing %d revisions to graph: %s/%s", ops.size(), conn.getGraphName(), conn.getGraphBranch());
    String txnId = null;
    try {
      txnId = TxnUtils.beginNewTransaction(conn);
      conn.getRevisionLog().submitRevisions(conn.getGraphName(), conn.getGraphBranch(), txnId, 1, ops);
      ops.clear();
      TxnUtils.commitTransaction(conn, txnId);
    } catch (Exception e) {
      TxnUtils.silentRollbackTransaction(conn, txnId);
      throw new EntanglementBotException("Failed to perform graph operations on "
          +conn.getGraphBranch()+"/"+conn.getGraphBranch()
          +". Exception attached.", e);
    }
  }

}
