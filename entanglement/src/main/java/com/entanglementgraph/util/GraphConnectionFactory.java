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
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.torrenttamer.mongodb.MongoDbFactory;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphDAOFactory;
import com.entanglementgraph.graph.InsertMode;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.player.GraphCheckoutNamingScheme;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.RevisionLogDirectToMongoDbImpl;

import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 24/02/2013
 * Time: 13:10
 * To change this template use File | Settings | File Templates.
 */
public class GraphConnectionFactory {
  private static final Logger logger = Logger.getLogger(GraphConnectionFactory.class.getName());

  private final ClassLoader classLoader;
  private final String hostname;
  private final String database;

  private InsertMode insertMode;

  private DbObjectMarshaller marshaller;

  public GraphConnectionFactory(String hostname, String database) {
    this(GraphConnectionFactory.class.getClassLoader(), hostname, database);
  }

  public GraphConnectionFactory(ClassLoader classLoader, String hostname, String database) {
    this.classLoader = classLoader;
    this.hostname = hostname;
    this.database = database;
    this.insertMode = InsertMode.INSERT_CONSISTENCY;
    ObjectMarshallerFactory.create(classLoader);
  }

  public GraphConnection connect(String graphName, String graphBranch) throws GraphConnectionFactoryException {
    try {
      logger.info("Connecting to: "+hostname+"/"+database+", graph: "+graphName+"/"+graphBranch);

      MongoDbFactory dbFactory = new MongoDbFactory(hostname, database);
      Mongo mongo = dbFactory.createMongoConnection();
      DB db = mongo.getDB(database);

      RevisionLog revLog = new RevisionLogDirectToMongoDbImpl(classLoader, mongo, db);

      GraphCheckoutNamingScheme collectionNamer = new GraphCheckoutNamingScheme(graphName, graphBranch);
      DBCollection nodeCol = db.getCollection(collectionNamer.getNodeCollectionName());
      DBCollection edgeCol = db.getCollection(collectionNamer.getEdgeCollectionName());
      NodeDAO nodeDao = GraphDAOFactory.createDefaultNodeDAO(classLoader, mongo, db, nodeCol, edgeCol);
      EdgeDAO edgeDao = GraphDAOFactory.createDefaultEdgeDAO(classLoader, mongo, db, nodeCol, edgeCol);

      System.out.println("Setting DAO insert mode to: "+insertMode);
      nodeDao.setInsertModeHint(InsertMode.INSERT_CONSISTENCY);
      edgeDao.setInsertModeHint(InsertMode.INSERT_CONSISTENCY);

      GraphConnection connection = new GraphConnection();
      connection.setClassLoader(classLoader);
      connection.setDb(db);
      connection.setEdgeDao(edgeDao);
      connection.setGraphBranch(graphBranch);
      connection.setGraphName(graphName);
      connection.setMarshaller(marshaller);
      connection.setMongo(mongo);
      connection.setNodeDao(nodeDao);
      connection.setRevisionLog(revLog);

      return connection;
    } catch (Exception e) {
      throw new GraphConnectionFactoryException("Failed to connect to Entanglement", e);
    }
  }

  public InsertMode getInsertMode() {
    return insertMode;
  }

  public void setInsertMode(InsertMode insertMode) {
    this.insertMode = insertMode;
  }
}
