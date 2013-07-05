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
import com.entanglementgraph.util.GraphConnection;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;

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
  private final ClassLoader classLoader;
  private final DbObjectMarshaller marshaller;

  private final Map<String, GraphConnection> graphConnections;
  private GraphConnection currentConnection;

  private final Map<String, GraphCursor> graphCursors;
  private GraphCursor currentCursor;

  public EntanglementRuntime(ClassLoader classLoader, DbObjectMarshaller marshaller) {
    this.classLoader = classLoader;
    this.marshaller = marshaller;
    this.graphConnections = new HashMap<>();
    this.graphCursors = new HashMap<>();
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
