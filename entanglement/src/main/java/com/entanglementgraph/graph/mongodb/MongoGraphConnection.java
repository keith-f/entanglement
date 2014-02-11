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

package com.entanglementgraph.graph.mongodb;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.RevisionLog;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 24/02/2013
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class MongoGraphConnection extends GraphConnection {


  private ClassLoader classLoader;
  private DbObjectMarshaller marshaller;


  private MongoClient pool;
  private DB db;

  public MongoGraphConnection() {
  }

  public MongoClient getPool() {
    return pool;
  }

  public void setPool(MongoClient pool) {
    this.pool = pool;
  }

  public DB getDb() {
    return db;
  }

  public void setDb(DB db) {
    this.db = db;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public DbObjectMarshaller getMarshaller() {
    return marshaller;
  }

  public void setMarshaller(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }
}
