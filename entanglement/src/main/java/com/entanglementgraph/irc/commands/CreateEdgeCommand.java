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

package com.entanglementgraph.irc.commands;

import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.revlog.commands.EdgeModification;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.halfspinsoftware.uibot.*;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.halfspinsoftware.uibot.commands.BotCommandException;
import com.halfspinsoftware.uibot.commands.UserException;
import com.mongodb.BasicDBObject;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class CreateEdgeCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Creates or updates an edge in the currently active graph.";
  }


  @Override
  public List<Param> getParams() {
    List<Param> params = new LinkedList<>();
    params.add(new RequiredParam("type", String.class, "The type name of the edge to create/modify"));
    params.add(new RequiredParam("entityName", String.class, "A unique name for the edge to create/modify"));
    params.add(new OptionalParam("{ key=value pairs }", null, "A set of key=value pairs that will be added to the edge as attributes"));
    return params;
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {

    String type = ParamParser.findStringValueOf(args, "type");
    String entityName = ParamParser.findStringValueOf(args, "entityName");

    if (type == null) throw new UserException(sender, "You forgot to specify a entity type.");
    if (entityName == null) throw new UserException(sender, "You forgot to specify a entity name.");


    GraphConnection graphConn = userObject.getCurrentConnection();
    if (graphConn == null) throw new UserException(sender, "No graph was set as the 'current' connection.");

    // Parse annotations
    Map<String, String> attributes = parseAttributes(args);
    //FIXME do this properly - remove entries that are used as part of the command.
    attributes.remove("type");
    attributes.remove("entityName");

    try {
      bot.infoln(channel, "Going to create an edge: %s/%s with properties: %s", type, entityName, attributes);

      Edge edge = new Edge();
      edge.getKeys().setType(type);
      edge.getKeys().addName(entityName);

      // Serialise the basic Node object ot a MongoDB object.
      BasicDBObject edgeObj = userObject.getMarshaller().serialize(edge);
      // Add further custom properties
      for (Map.Entry<String, String> attr : attributes.entrySet()) {
        edgeObj.append(attr.getKey(), attr.getValue());
      }

      EdgeModification edgeUpdateCommand = new EdgeModification();
      edgeUpdateCommand.setEdge(edgeObj);
      edgeUpdateCommand.setMergePol(MergePolicy.APPEND_NEW__LEAVE_EXISTING); //FIXME policy should be user-configurable

      writeOperation(graphConn, edgeUpdateCommand);
      Message result = new Message(channel);
      result.println("Edge created/updated: %s", entityName);
      return result;
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
