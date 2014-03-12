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

import com.entanglementgraph.cursor.GraphCursorRegistry;
import com.entanglementgraph.graph.couchdb.CouchGraphConnectionFactory;
import com.entanglementgraph.irc.data.GraphConnectionDetails;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.graph.GraphConnectionFactoryException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.BotLoggerFactory;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.Arrays;
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

//  private final BotState<EntanglementRuntime> state; // The channel state for which *this* is the user object.
  private final ClassLoader classLoader;
  private final HazelcastInstance hzInstance;

  private final IMap<String, GraphConnectionDetails> graphConnectionDetails;

  private final GraphCursorRegistry cursorRegistry;

  private final Map<Class, String> classJsonMappings;


  /**
   * Creates a new EntanglementRuntime object. This constructor is typically used for processes that may share some
   * of their configuration items (such as graph connection information, or graph cursors) with other, distributed
   * processes.
`  *
   * @param classLoader
   */
  public EntanglementRuntime(BotLogger botLogger,
                              ClassLoader classLoader, HazelcastInstance hzInstance) {
    this.classJsonMappings = new HashMap<>();
    this.classLoader = classLoader;
    this.hzInstance = hzInstance;
    this.graphConnectionDetails = hzInstance.getMap(HZ_CONN_DETAILS);;

    // Currently, this listener simply logs updates to <code>graphConnectionDetails</code>
    this.graphConnectionDetails.addEntryListener(new GraphConnectionListenerLogger(
        BotLoggerFactory.createNewLogger(botLogger, GraphConnectionListenerLogger.class.getSimpleName())),
        true);
//

//    this.graphCursors.addEntryListener(new GraphCursorPositionListenerLogger(bot, channel), true);

    this.cursorRegistry = new GraphCursorRegistry(hzInstance);
    /*
     * This is a Hazelcast listener on graphCursors that gets informed when a GraphCursor is added or updated
     * on local or remote processes.
     */
//    this.cursorRegistry.getCurrentPositions().addEntryListener(
//        new GraphCursorPositionListenerLogger(bot, channel, hzInstance), true);
  }

  /**
   * In order to correctly deserialise data classes (such as node and edge content types), the JSON serializer used by
   * Entanglement encodes the Java class type of beans into the JSON serialization.
   * This method allows you to specify such mappings
   * @param clazz java bean type, such as a Node or Edge <code>Content</code> bean.
   * @param jsonName a String name to associate with <code>clazz</code> serialised JSON. This may be an obvious name
   *                 such as the Java classname, or a different name, for example, a short name to reduce the size of
   *                 the JSON output.
   */
  public void addClassToJsonMapping(Class clazz, String jsonName) {
    classJsonMappings.put(clazz, jsonName);
  }

  /**
   * Adds a named graph connection details object. This object is added directly to the <code>graphConnectionDetails</code>,
   * which in a distributed system is a Hazelcast distributed data structure.
   * @param connName
   * @param connDetails
   */
  public void registerGraphConnectionDetails(String connName, GraphConnectionDetails connDetails) throws UserException {
    if (connDetails.getDbType() == null) {
      throw new UserException("No database type was selected. Supported types are: "
          + Arrays.asList(GraphConnectionDetails.DbType.values()));
    }
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

    if (details.getDbType() == GraphConnectionDetails.DbType.COUCH_DB) {
      try {
        CouchGraphConnectionFactory gcf = new CouchGraphConnectionFactory(
            details.getClusterName(), details.getDatabase(), classJsonMappings);
        return gcf.connect(details.getGraphName());
      } catch (GraphConnectionFactoryException e) {
        throw new BotCommandException("Failed to connect to graph via connection: "+connName+", "+details);
      }
    } else {
      throw new BotCommandException("Unsupported database type: "+details.getDbType());
    }
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public IMap<String, GraphConnectionDetails> getGraphConnectionDetails() {
    return graphConnectionDetails;
  }

  public HazelcastInstance getHzInstance() {
    return hzInstance;
  }

  public GraphCursorRegistry getCursorRegistry() {
    return cursorRegistry;
  }
}
