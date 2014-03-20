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

package com.entanglementgraph.util;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.RevisionLog;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 24/02/2013
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
abstract public class GraphConnection {

  private String clusterName;
  private String databaseName;

  private String graphName;

  private RevisionLog revisionLog;
  private NodeDAO<Content> nodeDao;
  private EdgeDAO<Content, Content, Content> edgeDao;

  public GraphConnection() {
  }

  public String getGraphName() {
    return graphName;
  }

  public void setGraphName(String graphName) {
    this.graphName = graphName;
  }

  public RevisionLog getRevisionLog() {
    return revisionLog;
  }

  public void setRevisionLog(RevisionLog revisionLog) {
    this.revisionLog = revisionLog;
  }

  public NodeDAO<Content> getNodeDao() {
    return nodeDao;
  }

  public void setNodeDao(NodeDAO<Content> nodeDao) {
    this.nodeDao = nodeDao;
  }

  public EdgeDAO<Content, Content, Content> getEdgeDao() {
    return edgeDao;
  }

  public void setEdgeDao(EdgeDAO<Content, Content, Content> edgeDao) {
    this.edgeDao = edgeDao;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
}
