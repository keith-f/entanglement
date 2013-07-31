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
import com.mongodb.*;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This factory class creates <code>GraphConnection</code> objects for accessing your graphs. There are two
 * aspects to consider:
 * <ul>
 *   <li>The <code>GraphConnection</code> contains a reference to a database connection, plus the necessary utilities
 *   that are associated with object stored in that database (such as ClassLoaders for object marshalling, etc). Each
 *   <code>GraphConnection</code> should be used by a single thread.</li>
 *   <li>MongoDB clusters - these are the physical machines that store some or all of a graph. By default, a single
 *   cluster named 'localhost' is defined for convenience. If you wish to create a <code>GraphConnection</code> for
 *   a graph stored on a different server/cluster, you'll need to register a new cluster first using the static
 *   methods below. Registering a cluster will provide you with a <code>MongoClient</code>. These are thread-safe and
 *   represent a pool of connections. There should be one <code>MongoClient</code> instance per database per JVM.</li>
 * </ul>
 *
 * See the following URLs for more information on creating MongoDB connections:
 * http://docs.mongodb.org/ecosystem/tutorial/getting-started-with-java-driver/
 * and
 * http://api.mongodb.org/java/current/com/mongodb/MongoClient.html
 * and
 * http://docs.mongodb.org/ecosystem/drivers/java-concurrency/
 *
 * @author Keith Flanagan
 */
public class GraphConnectionFactory {
  private static final Logger logger = Logger.getLogger(GraphConnectionFactory.class.getName());

  private static final Map<String, MongoClient> mongoPools = new HashMap<>();

  public static MongoClient registerNamedPool(String poolName, ServerAddress... replicaServers)
      throws GraphConnectionFactoryException {
    synchronized (mongoPools) {
      if (mongoPools.containsKey(poolName)) {
        logger.info(String.format("A MongoDB cluster with the name: %s already exists. Closing existing pool, " +
            "and reconfiguring with new server set: %s", poolName, Arrays.asList(replicaServers)));
        MongoClient pool = mongoPools.get(poolName);
        pool.close();
      }

      try {
        MongoClient pool = new MongoClient(Arrays.asList(replicaServers));
        pool.setWriteConcern(WriteConcern.SAFE);
        mongoPools.put(poolName, pool);
        return pool;
      }
      catch(Exception e) {
        throw new GraphConnectionFactoryException("Failed to create MongoDB connection", e);
      }
    }
  }

  public static MongoClient getNamedPool(String poolName) {
    synchronized (mongoPools) {
      return mongoPools.get(poolName);
    }
  }

  private final ClassLoader classLoader;
  private final MongoClient connectionPool;
  private final String poolName;
  private final String databaseName;

  private DbObjectMarshaller marshaller;

  public GraphConnectionFactory(String clusterName, String databaseName) {
    this(GraphConnectionFactory.class.getClassLoader(), clusterName, databaseName);
  }

  public GraphConnectionFactory(ClassLoader classLoader, String poolName, String databaseName) {
    this.classLoader = classLoader;
    this.marshaller = ObjectMarshallerFactory.create(classLoader);
    this.connectionPool = getNamedPool(poolName);
    this.poolName = poolName;
    this.databaseName = databaseName;
  }

  public GraphConnection connect(String graphName, String graphBranch) throws GraphConnectionFactoryException {
    try {
      if (connectionPool == null) {
        throw new GraphConnectionFactoryException("Connection pool: "+poolName+" could not be found.");
      }
      logger.info("Connecting to: " + connectionPool.getServerAddressList() + ", graph: " + graphName + "/" + graphBranch);

      GraphConnection connection = new GraphConnection();
      connection.setPoolName(poolName);
      connection.setDatabaseName(databaseName);

      // Underlying connection to MongoDB
//      MongoDbFactory dbFactory = new MongoDbFactory(hostname, database);
//      Mongo mongo = dbFactory.createMongoConnection();
//      DB db = mongo.getDB(database);
      DB db = connectionPool.getDB(databaseName);

      connection.setClassLoader(classLoader);
      connection.setPool(connectionPool);
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
      NodeDAO nodeDao = GraphDAOFactory.createDefaultNodeDAO(classLoader, connectionPool, db, nodeCol);
      EdgeDAO edgeDao = GraphDAOFactory.createDefaultEdgeDAO(classLoader, connectionPool, db, edgeCol);

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

//  @SuppressWarnings("UnusedDeclaration")
//  public static void silentClose(GraphConnection conn) {
//    if (conn == null) {
//      return;
//    }
//    try {
//      conn.getMongo().close();
//    } catch (Exception e) {
//      logger.info("Failed to close connection. Nothing we can do here. Ignoring this problem, but stack trace follows");
//      e.printStackTrace();
//    }
//  }

}
