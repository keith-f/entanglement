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

package com.entanglementgraph.irc.data;

import java.io.Serializable;

/**
 * Holds connection details that could be used to create a GraphConnection to a specified Entanglement graph
 * stored within a MongoDB database.
 *
 * @author Keith Flanagan
 */
public class GraphConnectionDetails implements Serializable {

  private String hostname;
  private String database;
  private String username;
  private String password;

  private String graphName;
  private String graphBranch;

  public GraphConnectionDetails() {
  }

  public GraphConnectionDetails(String hostname, String database, String graphName, String graphBranch) {
    this.hostname = hostname;
    this.database = database;
    this.graphName = graphName;
    this.graphBranch = graphBranch;
  }

  @Override
  public String toString() {
    return "GraphConnectionDetails{" +
        "hostname='" + hostname + '\'' +
        ", database='" + database + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        ", graphName='" + graphName + '\'' +
        ", graphBranch='" + graphBranch + '\'' +
        '}';
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getGraphName() {
    return graphName;
  }

  public void setGraphName(String graphName) {
    this.graphName = graphName;
  }

  public String getGraphBranch() {
    return graphBranch;
  }

  public void setGraphBranch(String graphBranch) {
    this.graphBranch = graphBranch;
  }
}
