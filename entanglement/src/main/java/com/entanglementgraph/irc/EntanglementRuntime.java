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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 13/05/2013
 * Time: 17:19
 * To change this template use File | Settings | File Templates.
 */
public class EntanglementRuntime {
  private static final String HZ_CONN_DETAILS = EntanglementRuntime.class.getSimpleName()+".GraphConnectionDetails";
  private static final String HZ_GRAPH_CURSORS = EntanglementRuntime.class.getSimpleName()+".GraphCursors";

  public static EntanglementRuntime createLocalRuntime(ClassLoader classLoader, DbObjectMarshaller marshaller) {
    EntanglementRuntime runtime = new EntanglementRuntime(classLoader, marshaller);
    return runtime;
  }

  public static EntanglementRuntime createDistributedRuntime(
      ClassLoader classLoader, DbObjectMarshaller marshaller, HazelcastInstance hzInstance) {


    IMap<String, GraphConnectionDetails> graphConnectionDetails = hzInstance.getMap(HZ_CONN_DETAILS);
    IMap<String, GraphCursor > graphCursors = hzInstance.getMap(HZ_GRAPH_CURSORS);
    EntanglementRuntime runtime = new EntanglementRuntime(classLoader, marshaller, graphConnectionDetails, graphCursors);
    return runtime;
  }


  private final ClassLoader classLoader;
  private final DbObjectMarshaller marshaller;

  /**
   * Holds objects with actual graph connection objects - real MongoDB objects with open connections to the database.
   */
  private final Map<String, GraphConnection> graphConnections;
  private GraphConnection currentConnection;

  private final Map<String, GraphConnectionDetails> graphConnectionDetails;

  private final Map<String, GraphCursor> graphCursors;
  private GraphCursor currentCursor;

  /**
   * Creates a new EntanglementRuntime object. This constructor is typically used for 'local' processes that do not
   * need to interact with other processes.
   *
   * @param classLoader
   * @param marshaller
   */
  private EntanglementRuntime(ClassLoader classLoader, DbObjectMarshaller marshaller) {
    this.classLoader = classLoader;
    this.marshaller = marshaller;
    this.graphConnections = new HashMap<>();
    this.graphCursors = new HashMap<>();
    this.graphConnectionDetails = new HashMap<>();
  }

  /**
   * Creates a new EntanglementRuntime object. This constructor is typically used for processes that may share some
   * of their configuration items (such as graph connection information, or graph cursors) with other, distributed
   * processes.
   *
   * @param classLoader
   * @param marshaller
   * @param graphConnectionDetails
   * @param graphCursors
   */
  private EntanglementRuntime(ClassLoader classLoader, DbObjectMarshaller marshaller,
                             Map<String, GraphConnectionDetails> graphConnectionDetails, Map<String, GraphCursor> graphCursors) {
    this.classLoader = classLoader;
    this.marshaller = marshaller;
    this.graphConnectionDetails = graphConnectionDetails;
    this.graphCursors = graphCursors;
    this.graphConnections = new HashMap<>();
  }

  public void addGraphConnection(String connName, GraphConnection conn) {
    graphConnections.put(connName, conn);
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public DbObjectMarshaller getMarshaller() {
    return marshaller;
  }

  public Map<String, GraphConnection> getGraphConnections() {
    return graphConnections;
  }

  public GraphConnection getCurrentConnection() {
    return currentConnection;
  }

  public void setCurrentConnection(GraphConnection currentConnection) {
    this.currentConnection = currentConnection;
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
}
