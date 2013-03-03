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

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.revlog.RevisionLog;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 24/02/2013
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class GraphConnection {
  private Mongo mongo;
  private DB db;

  private String graphName;
  private String graphBranch;

  private ClassLoader classLoader;
  private RevisionLog revisionLog;
  private NodeDAO nodeDao;
  private EdgeDAO edgeDao;

  private DbObjectMarshaller marshaller;

  public GraphConnection() {
  }

  public Mongo getMongo() {
    return mongo;
  }

  public void setMongo(Mongo mongo) {
    this.mongo = mongo;
  }

  public DB getDb() {
    return db;
  }

  public void setDb(DB db) {
    this.db = db;
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

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public RevisionLog getRevisionLog() {
    return revisionLog;
  }

  public void setRevisionLog(RevisionLog revisionLog) {
    this.revisionLog = revisionLog;
  }

  public NodeDAO getNodeDao() {
    return nodeDao;
  }

  public void setNodeDao(NodeDAO nodeDao) {
    this.nodeDao = nodeDao;
  }

  public EdgeDAO getEdgeDao() {
    return edgeDao;
  }

  public void setEdgeDao(EdgeDAO edgeDao) {
    this.edgeDao = edgeDao;
  }

  public DbObjectMarshaller getMarshaller() {
    return marshaller;
  }

  public void setMarshaller(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }
}
