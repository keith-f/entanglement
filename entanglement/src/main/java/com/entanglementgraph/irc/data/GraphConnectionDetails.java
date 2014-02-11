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
 * stored within a named MongoDB cluster/connection pool.
 *
 * @author Keith Flanagan
 */
public class GraphConnectionDetails implements Serializable {

  public static enum DbType {
    MONGO_DB,
    COUCH_DB;
  }

  private String clusterName;
  private String database;
  private String username;
  private String password;

  private String graphName;

  private DbType dbType;

  public GraphConnectionDetails() {
  }

  public GraphConnectionDetails(String clusterName, String database, String graphName) {
    this.clusterName = clusterName;
    this.database = database;
    this.graphName = graphName;
  }

  @Override
  public String toString() {
    return "GraphConnectionDetails{" +
        "dbType='" + clusterName + '\'' +
        "clusterName='" + dbType + '\'' +
        ", database='" + database + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        ", graphName='" + graphName + '\'' +
        '}';
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
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

  public DbType getDbType() {
    return dbType;
  }

  public void setDbType(DbType dbType) {
    this.dbType = dbType;
  }
}
