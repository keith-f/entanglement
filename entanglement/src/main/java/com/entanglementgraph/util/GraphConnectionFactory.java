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

import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphDAOFactory;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.player.GraphCheckoutNamingScheme;
import com.entanglementgraph.player.LogPlayer;
import com.entanglementgraph.player.LogPlayerMongoDbImpl;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.RevisionLogDirectToMongoDbImpl;
import com.entanglementgraph.util.experimental.GraphOpPostCommitPlayer;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.torrenttamer.mongodb.MongoDbFactory;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;

import java.util.logging.Logger;

public class GraphConnectionFactory {
  private static final Logger logger = Logger.getLogger(GraphConnectionFactory.class.getName());

  private final ClassLoader classLoader;
  private final String hostname;
  private final String database;

  private DbObjectMarshaller marshaller;

  public GraphConnectionFactory(String hostname, String database) {
    this(GraphConnectionFactory.class.getClassLoader(), hostname, database);
  }

  public GraphConnectionFactory(ClassLoader classLoader, String hostname, String database) {
    this.classLoader = classLoader;
    this.hostname = hostname;
    this.database = database;
    marshaller = ObjectMarshallerFactory.create(classLoader);
  }

  public GraphConnection connect(String graphName, String graphBranch) throws GraphConnectionFactoryException {
    try {
      logger.info("Connecting to: " + hostname + "/" + database + ", graph: " + graphName + "/" + graphBranch);

      GraphConnection connection = new GraphConnection();

      // Underlying connection to MongoDB
      MongoDbFactory dbFactory = new MongoDbFactory(hostname, database);
      Mongo mongo = dbFactory.createMongoConnection();
      DB db = mongo.getDB(database);
      connection.setClassLoader(classLoader);
      connection.setMongo(mongo);
      connection.setDb(db);
      connection.setGraphBranch(graphBranch);
      connection.setGraphName(graphName);
      connection.setMarshaller(marshaller);

      GraphCheckoutNamingScheme collectionNamer = new GraphCheckoutNamingScheme(graphName, graphBranch);
      DBCollection revCol = db.getCollection(collectionNamer.getRevCollectionName());
      DBCollection nodeCol = db.getCollection(collectionNamer.getNodeCollectionName());
      DBCollection edgeCol = db.getCollection(collectionNamer.getEdgeCollectionName());

      RevisionLog revLog = new RevisionLogDirectToMongoDbImpl(connection, revCol);
      connection.setRevisionLog(revLog);
      NodeDAO nodeDao = GraphDAOFactory.createDefaultNodeDAO(classLoader, mongo, db, nodeCol);
      EdgeDAO edgeDao = GraphDAOFactory.createDefaultEdgeDAO(classLoader, mongo, db, edgeCol);

      connection.setEdgeDao(edgeDao);
      connection.setNodeDao(nodeDao);

      // Connection object is fully populated at this point.

      //Wire up a player by default (source revision log and destination collection are for the same graph in this case)
      LogPlayer logPlayer = new LogPlayerMongoDbImpl(connection, connection);
      GraphOpPostCommitPlayer opPlayer = new GraphOpPostCommitPlayer(logPlayer);
      revLog.addListener(opPlayer);

      return connection;
    } catch (Exception e) {
      throw new GraphConnectionFactoryException("Failed to connect to Entanglement", e);
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  public static void silentClose(GraphConnection conn) {
    if (conn == null) {
      return;
    }
    try {
      conn.getMongo().close();
    } catch (Exception e) {
      logger.info("Failed to close connection. Nothing we can do here. Ignoring this problem, but stack trace follows");
      e.printStackTrace();
    }
  }

}
