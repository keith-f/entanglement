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

import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementBotException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.halfspinsoftware.uibot.Message;
import com.halfspinsoftware.uibot.commands.AbstractCommand;
import com.mongodb.BasicDBObject;
import com.torrenttamer.util.UidGenerator;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class CreateNodeCommand extends AbstractCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Creates or updates a node in the currently active graph.");
    return txt.toString();
  }

  @Override
  public String getHelpText() {
    StringBuilder txt = new StringBuilder();
    txt.append("USAGE:\n");
    txt.append("  * Node type name\n");
    txt.append("  * Node name\n");
    txt.append("  * List of key=value pairs\n");

    return txt.toString();
  }

  @Override
  public Message call() throws Exception {
    Message result = new Message(channel);
    String type = null;
    String name = null;
    GraphConnection graphConn = userObject.getCurrentConnection();
    try {
      if (graphConn == null) {
        bot.errln(errChannel, "No graph was set as the 'current' connection.");
        return result;
      }

      type = args[0];
      name = args[1];
      Map<String, String> attributes = parseAttributes(args);
      bot.infoln(channel, "Going to create a node: %s/%s with properties: %s", type, name, attributes);

      Node node = new Node();
      node.getKeys().setType(type);
      node.getKeys().addName(name);

      // Serialise the basic Node object ot a MongoDB object.
      BasicDBObject nodeObj = userObject.getMarshaller().serialize(node);
      // Add further custom properties
      for (Map.Entry<String, String> attr : attributes.entrySet()) {
        nodeObj.append(attr.getKey(), attr.getValue());
      }

      NodeModification nodeCommand = new NodeModification();
      nodeCommand.setNode(nodeObj);
      nodeCommand.setMergePol(MergePolicy.APPEND_NEW__LEAVE_EXISTING);

      writeOperation(graphConn, nodeCommand);

      result.println("Node created/updated: %s", name);
    } catch (Exception e) {
      bot.printException(errChannel, "WARNING: an Exception occurred while processing.", e);
    } finally {
      return result;
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
