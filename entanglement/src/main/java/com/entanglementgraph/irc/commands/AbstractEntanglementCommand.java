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

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.AbstractCommand;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import com.scalesinformatics.util.UidGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A partial command implementation that is useful for many Entanglement-based IRC commands. Includes support for
 * parsing graph connection names and cursor names from command lines. Graph connections can either be acquired
 * from the command line (if present), or whatever is currently the default specified in this channel's EntanglementRuntime.
 *
 * @author Keith Flanagan
 */
abstract public class AbstractEntanglementCommand<T extends EntanglementRuntime> extends AbstractCommand<T> {

  private boolean graphConnNeeded = false;
  private boolean graphCursorNeeded = false;
  private boolean tempClusterNameNeeded = false;


  protected String graphConnName;
  protected GraphConnection graphConn;
  protected String cursorName;
  protected GraphCursor cursor;
  protected GraphCursor.CursorContext cursorContext;

  private String tempClusterName;
  private final Set<GraphConnection> temporaryConnections;


  protected final IrcEntanglementFormat entFormat;


  protected static enum Requirements {
    GRAPH_CONN_NEEDED,
    CURSOR_NEEDED,
    TEMP_CLUSTER_NAME_NEEDED
  };


  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    if (graphConnNeeded) {
      params.add(new OptionalParam("conn", String.class, "Graph connection to use. If no connection name is specified, the 'current' connection will be used."));
    }
    if (graphCursorNeeded) {
      params.add(new OptionalParam("cursor", String.class, "The name of the cursor to use. If not specified, the default cursor will be used"));
    }
    if (tempClusterNameNeeded) {
      params.add(new RequiredParam("temp-cluster", String.class, "The name of a configured MongoDB cluster to use for storing temporary graphs."));
    }
    return params;
  }


  protected AbstractEntanglementCommand(Requirements... requirements)
  {
    this.temporaryConnections = new HashSet<>();
    entFormat = new IrcEntanglementFormat();
    for (Requirements req : requirements) {
      switch (req) {
        case GRAPH_CONN_NEEDED:
          graphConnNeeded = true;
          break;
        case CURSOR_NEEDED:
          graphCursorNeeded = true;
          break;
        case TEMP_CLUSTER_NAME_NEEDED:
          tempClusterNameNeeded = true;
          break;
      }
    }
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    if (graphConnNeeded) {
      graphConnName = parsedArgs.get("conn").getStringValue();
      graphConn = EntanglementIrcCommandUtils.getSpecifiedGraphOrDefault(state.getUserObject(), graphConnName);
      // Make sure that graphConnName reflects the chosen connection, even if no name was specified by the user
      if (graphConn != null) {
        graphConnName = state.getUserObject().getCurrentConnectionName();
      }
    }
    if (graphCursorNeeded) {
      cursorName = parsedArgs.get("cursor").getStringValue();
      cursor = EntanglementIrcCommandUtils.getSpecifiedCursorOrDefault(state.getUserObject(), cursorName);
      // Make sure that cursorName reflects the chosen cursor, even if no name was specified by the user
      if (cursor != null) {
        cursorName = cursor.getName();
      }
      cursorContext = new GraphCursor.CursorContext(graphConn, state.getUserObject().getHzInstance());
    }
    if (tempClusterNameNeeded) {
      tempClusterName = parsedArgs.get("temp-cluster").getStringValue();
    }
  }

  @Override
  protected void postProcessLine() throws UserException, BotCommandException {
    super.postProcessLine();
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
    GraphConnectionFactory factory = new GraphConnectionFactory(tempClusterName, GraphConnectionFactory.DEFAULT_TMP_DB_NAME);
    GraphConnection conn = factory.connect("tmp_"+ UidGenerator.generateUid(), "trunk");

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
  protected GraphConnection createTemporaryGraph()
      throws GraphConnectionFactoryException {
    return createTemporaryGraph(tempClusterName);
  }

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
      if (!tmpConnection.getGraphName().startsWith("tmp_")) {
        throw new GraphConnectionFactoryException("Will not dispose of graph: "+tmpConnection.getGraphName()
          + " since (based on its name), it does not appear to be a temporary connection. This is a failsafe feature.");
      }
      tmpConnection.getRevisionLog().getRevLogCol().drop();
      tmpConnection.getNodeDao().getCollection().drop();
      tmpConnection.getEdgeDao().getCollection().drop();
    } catch (Exception e) {
      logger.printException("Failed to dispose of one or more temporary graph collections.", e);
    }

  }

}
