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

import com.entanglementgraph.irc.EntanglementIRCBotConfigNames;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.graph.GraphConnectionFactoryException;
import com.entanglementgraph.graph.mongodb.TmpGraphConnectionFactory;
import com.hazelcast.core.HazelcastInstance;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.HashSet;
import java.util.Set;

/**
 * A partial command implementation that is useful for many Entanglement-based IRC commands. Includes support for
 * parsing graph connection names and cursor names from command lines. Graph connections can either be acquired
 * from the command line (if present), or whatever is currently the default specified in this channel's EntanglementRuntime.
 *
 * @author Keith Flanagan
 */
abstract public class AbstractEntanglementCommand extends AbstractCommand {

  private final TmpGraphConnectionFactory tmpConnFact = new TmpGraphConnectionFactory();
  private final Set<GraphConnection> temporaryConnections;

  protected final IrcEntanglementFormat entFormat;

  protected EntanglementRuntime entRuntime;
  protected HazelcastInstance hazelcast;

  protected AbstractEntanglementCommand() {
    this.temporaryConnections = new HashSet<>();
    entFormat = new IrcEntanglementFormat();
  }



  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();

    try {
      entRuntime = (EntanglementRuntime) bot.getGlobalState().getUserObjs()
          .get(EntanglementIRCBotConfigNames.STATE_PROP_ENTANGLEMENT);
      hazelcast = (HazelcastInstance) bot.getGlobalState().getUserObjs()
          .get(EntanglementIRCBotConfigNames.STATE_PROP_HAZELCAST);
    } catch (Exception e) {
      throw new BotCommandException("Failed to obtain required state objects", e);
    }

    if (entRuntime == null) {
      throw new UserException("No Entanglement runtime object could be found");
    }
    if (hazelcast == null) {
      throw new UserException("No HazelcastInstance object could be found");
    }
  }

  @Override
  protected void postProcessLine() throws UserException, BotCommandException {
    super.postProcessLine();

    // Tidy up any temporary connections that were requested
    for (GraphConnection tmpConn : temporaryConnections) {
      disposeOfTempGraph(tmpConn);
    }
  }

  /**
   * Creates a graph connection intended for local, temporary use. Usages might include temporarily extracting
   * a subset of nodes/edges for display or export purposes.
   * This method creates temporary graph collections within the default MongoDB database used for such graphs
   * (usually, 'temp'),
   *
   * @param tempClusterName the name of a MongoDB cluster to use for storing the graph.
   * @param disposeOnCommandCompletion if set <code>true</code>, then the this connection will be disposed of (MongoDB
   *                                   collections will be deleted) once the command has finished executing.
   * @return a graph connection on the specified database cluster.
   * @throws GraphConnectionFactoryException
   */
  protected GraphConnection createTemporaryGraph(String tempClusterName, boolean disposeOnCommandCompletion)
      throws GraphConnectionFactoryException {
    if (tempClusterName == null) {
      throw new GraphConnectionFactoryException("No temporary cluster name was specified");
    }

    GraphConnection conn = tmpConnFact.createTemporaryGraph(tempClusterName);

    if (disposeOnCommandCompletion) {
      temporaryConnections.add(conn);
    }

    logger.infoln("Created temporary graph: %s", conn.getGraphName());
    return conn;
  }

  /**
   * Same as <code>createTemporaryGraph(String)</code>, except that we use the MongoDB cluster named
   * on the command line by property 'temp-cluster'. To use this method, you must have specified
   * <code>TEMP_CLUSTER_NAME_NEEDED</code> in the constructor of this <code>AbstractEntanglementCommand</code>.
   *
   * Any GraphConnection created by this method will be destroyed (including the database content) after the IRC
   * command implementation has finished executing.
   *
   * @return a short-term GraphConnection whose database-backed content will be deleted once the IRC command has
   * finished executing.
   * @throws GraphConnectionFactoryException
   */
//  protected GraphConnection createTemporaryGraph()
//      throws GraphConnectionFactoryException {
//    return createTemporaryGraph(tempClusterName);
//  }

  /**
   * Creates a graph connection intended for local, temporary use. Usages might include temporarily extracting
   * a subset of nodes/edges for display or export purposes.
   * This method creates temporary graph collections within the default MongoDB database used for such graphs
   * (usually, 'temp'),
   *
   * Any GraphConnection created by this method will be destroyed (including the database content) after the IRC
   * command implementation has finished executing.
   *
   * @param tempClusterName the name of a MongoDB cluster to use for storing the graph.
   * @return a short-term GraphConnection whose database-backed content will be deleted once the IRC command has
   * finished executing.
   * @throws GraphConnectionFactoryException
   */
  protected GraphConnection createTemporaryGraph(String tempClusterName)
      throws GraphConnectionFactoryException {
    return createTemporaryGraph(tempClusterName, true);
  }

  /**
   * Given a graph connection, deletes all MongoDB collections relating to the connection.  This is a destructive
   * operation - as a failsafe, only connections with graph names beginning with 'tmp_' will be deleted.
   * @param tmpConnection
   * @throws GraphConnectionFactoryException
   */
  protected void disposeOfTempGraph(GraphConnection tmpConnection) {
    if (tmpConnection == null) {
      return;
    }
    logger.infoln("Attempting to drop datastructures relating to temporary graph: %s", tmpConnection.getGraphName());
    try {
      tmpConnFact.disposeOfTempGraph(tmpConnection);
    } catch (Exception e) {
      logger.printException("Failed to dispose of one or more temporary graph collections.", e);
    }

  }

}
