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

package com.entanglementgraph.irc;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.irc.data.GraphConnectionDetails;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.HashMap;
import java.util.Map;

/**
 * A per-channel Entanglement IRC bot-specific configuration object for storing configuration information such
 * as graph connection details, and current graph cursors. Typically, there is an instance of this class for each
 * IRC channel the bot has joined. There may also be a 'global' EntanglementRuntime for the bot.
 *
 * There are two factory methods to create an EntanglementRuntime - one for cases where the IRC bot should run in
 * isolation, and another for situations where you want this bot to co-operate with other bots. In the second case,
 * certain configuration objects are stored in a Hazelcast data grid and are accessible and modifiable by other
 * Entanglement bot instances.
 *
 * @author Keith Flanagan
 */
public class EntanglementRuntime {
  private static final String HZ_CONN_DETAILS = EntanglementRuntime.class.getSimpleName()+".GraphConnectionDetails";
  private static final String HZ_GRAPH_CURSORS = EntanglementRuntime.class.getSimpleName()+".GraphCursors";


  /**
   * Creates an EntanglementRuntime instance where some of the data structures are stored in a distributed memory,
   * accessible by other bots that join the same Hazelcast cluster.
   *
   * @param classLoader
   * @param marshaller
   * @param hzInstance
   * @return
   */
  public static EntanglementRuntime createRuntime(
      EntanglementBot bot, String channel, ClassLoader classLoader, DbObjectMarshaller marshaller, HazelcastInstance hzInstance) {

    // TODO we probably need to configure these datastructures properly - size limits, retention policy, etc.
    IMap<String, GraphConnectionDetails> graphConnectionDetails = hzInstance.getMap(HZ_CONN_DETAILS);
    graphConnectionDetails.addEntryListener(new GraphConnectionListenerLogger(bot, channel), true);
    IMap<String, GraphCursor > graphCursors = hzInstance.getMap(HZ_GRAPH_CURSORS);
    graphCursors.addEntryListener(new GraphCursorListenerLogger(bot, channel), true);
    EntanglementRuntime runtime = new EntanglementRuntime(bot, channel, classLoader, marshaller, graphConnectionDetails, graphCursors);
    return runtime;
  }


  private final ClassLoader classLoader;
  private final DbObjectMarshaller marshaller;

  /**
   * Holds objects with actual graph connection objects - real MongoDB objects with open connections to the database.
   */
//  private final Map<String, GraphConnection> graphConnections;
//  private GraphConnection currentConnection;
  private String currentConnectionName;

  private final IMap<String, GraphConnectionDetails> graphConnectionDetails;

  private final IMap<String, GraphCursor> graphCursors;
  private GraphCursor currentCursor;


  /**
   * Creates a new EntanglementRuntime object. This constructor is typically used for processes that may share some
   * of their configuration items (such as graph connection information, or graph cursors) with other, distributed
   * processes.
   *
   * @param bot the IRC bot that this EntanglementRuntime instance exists in
   * @param channel the channel that this EntanglementRuntime instance holds information for, or NULL if this is the
   *                global state object for this bot.
   * @param classLoader
   * @param marshaller
   * @param graphConnectionDetails
   * @param graphCursors
   */
  protected EntanglementRuntime(EntanglementBot bot, String channel,
                              ClassLoader classLoader, DbObjectMarshaller marshaller,
                              IMap<String, GraphConnectionDetails> graphConnectionDetails, IMap<String, GraphCursor> graphCursors) {
    this.classLoader = classLoader;
    this.marshaller = marshaller;
    this.graphConnectionDetails = graphConnectionDetails;
    this.graphCursors = graphCursors;
//    this.graphConnections = new HashMap<>();
  }

  /**
   * Adds a named graph connection details object. This object is added directly to the <code>graphConnectionDetails</code>,
   * which in a distributed system is a Hazelcast distributed data structure.
   * @param connName
   * @param connDetails
   */
  public void registerGraphConnectionDetails(String connName, GraphConnectionDetails connDetails) {
    graphConnectionDetails.put(connName, connDetails);
  }

  /**
   * Creates (or returns an unused but cached) GraphConnection object for the specified connection name. Before you
   * can call this method, you must have first registered appropriate connection information (hostname, database ,etc)
   * with <code>registerGraphConnectionDetails</code>.
   * @param connName
   */
  public GraphConnection createGraphConnectionFor(String connName) throws UserException, BotCommandException {
    GraphConnectionDetails details = graphConnectionDetails.get(connName);
    if (details == null) {
      throw new UserException("Unknown connection name: "+connName);
    }
    GraphConnectionFactory gcf = new GraphConnectionFactory(classLoader, details.getHostname(), details.getDatabase());
    try {
      return gcf.connect(details.getGraphName(), details.getGraphBranch());
    } catch (GraphConnectionFactoryException e) {
      throw new BotCommandException("Failed to connect to graph via connection: "+connName+", "+details);
    }
  }

  public GraphConnection createGraphConnectionForCurrentConnection() throws UserException, BotCommandException {
    if (currentConnectionName == null) {
      throw new UserException("No 'current' connection was set!");
    }
    return createGraphConnectionFor(currentConnectionName);
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public DbObjectMarshaller getMarshaller() {
    return marshaller;
  }

  public String getCurrentConnectionName() {
    return currentConnectionName;
  }

  public void setCurrentConnectionName(String currentConnectionName) {
    this.currentConnectionName = currentConnectionName;
  }

  public Map<String, GraphCursor> getGraphCursors() {
    return graphCursors;
  }

  public GraphCursor getCurrentCursor() {
    return currentCursor;
  }

  public void setCurrentCursor(GraphCursor currentCursor) {
    this.currentCursor = currentCursor;
  }

  public IMap<String, GraphConnectionDetails> getGraphConnectionDetails() {
    return graphConnectionDetails;
  }
}
