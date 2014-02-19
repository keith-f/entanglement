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

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.util.GraphConnection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.scalesinformatics.util.UidGenerator;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This factory class creates <code>GraphConnection</code> objects for accessing your graphs stored in CouchDB.
 * There are two aspects to consider:
 * <ul>
 *   <li>The <code>GraphConnection</code> contains a reference to a database connection, plus the necessary utilities
 *   that are associated with object stored in that database (such as ClassLoaders for object marshalling, etc). Each
 *   <code>GraphConnection</code> should be used by a single thread.</li>
 *   <li>CouchDB clusters - these are the physical machines that store some or all of a graph. By default, a single
 *   cluster named 'localhost' is defined for convenience. If you wish to create a <code>GraphConnection</code> for
 *   a graph stored on a different server/cluster, you'll need to register a new cluster first using the static
 *   methods below.</li>
 * </ul>
 *
 * @author Keith Flanagan
 */
public class CouchGraphConnectionFactory implements GraphConnectionFactory{
  private static final Logger logger = Logger.getLogger(CouchGraphConnectionFactory.class.getName());

  /**
   * A set of database cluster names --> access URLs (eg, http://localhost:5984).
   * TODO currently, only a single URL is supported. We may want to support BigCouch with multiple URLs later.
   * http://bigcouch.cloudant.com/use
   * According to this, we can use any URL of any machine in the cluster.
   */
  private static final Map<String, String> couchClusters = new HashMap<>();

  public static void registerNamedCluster(String clusterName, String couchUrl)
      throws GraphConnectionFactoryException {
    synchronized (couchClusters) {
      if (couchClusters.containsKey(clusterName)) {
        logger.info(String.format("A CouchDB cluster with the name: %s already exists. " +
            "Reconfiguring with new server set: %s", clusterName, Arrays.asList(couchUrl)));
      }

      couchClusters.put(clusterName, couchUrl);
    }
  }

  public static String getNamedClusterUrl(String clusterName) {
    synchronized (couchClusters) {
      return couchClusters.get(clusterName);
    }
  }

  public static boolean containsNamedCluster(String clusterName) {
    synchronized (couchClusters) {
      return couchClusters.containsKey(clusterName);
    }
  }

  private final String clusterName;
  private final String clusterUrl;
  private final String databaseName;
  private final Map<Class, String> classJsonMappings;

  public CouchGraphConnectionFactory(String clusterName, String databaseName, Map<Class, String> classJsonMappings) {
    this.clusterName = clusterName;
    this.clusterUrl = getNamedClusterUrl(clusterName);
    this.databaseName = databaseName;
    this.classJsonMappings = classJsonMappings;
  }

  public GraphConnection connect(String graphName) throws GraphConnectionFactoryException {
    if (clusterUrl == null) {
      throw new GraphConnectionFactoryException("Unknown CouchDB cluster name: "+clusterName);
    }
    try {
      logger.info("Connecting to: " + clusterUrl + ", database: "+ databaseName + ", graph: " + graphName);

      System.setProperty("org.ektorp.support.AutoUpdateViewOnChange", "true");


      HttpClient httpClient = new StdHttpClient.Builder()
          .url("http://localhost:5984")
//        .username("admin")
//        .password("secret")
          .build();

      ExtStdObjectMapperFactory omFactory = new ExtStdObjectMapperFactory(classJsonMappings);
      CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient, omFactory);
// if the second parameter is true, the database will be created if it doesn't exists
      CouchDbConnector db = dbInstance.createConnector(databaseName, true);
      ObjectMapper om = omFactory.getLastCreatedObjectMapper();

      CouchGraphConnection connection = new CouchGraphConnection();

      connection.setClusterName(clusterName);
      connection.setDatabaseName(databaseName);
      connection.setGraphName(graphName);
      connection.setDb(db);
      connection.setOm(om);

//      //objectMapper.registerSubtypes(new NamedType(Sofa.class, "Sfa"));
//      for (Map.Entry<Class, String> mapping : classJsonMappings.entrySet()) {
//        logger.info("Adding mapping "+mapping.getKey()+" --> "+mapping.getValue());
//        om.registerSubtypes(new NamedType(mapping.getKey(), mapping.getValue()));
//      }

      RevisionLog revLog = new RevisionLogCouchDBImpl(db);
      NodeDAO nodeDao = new NodeDAOCouchDbImpl(db, om);
      EdgeDAO edgeDao = new EdgeDAOCouchDbImpl(db, om);

      connection.setRevisionLog(revLog);
      connection.setNodeDao(nodeDao);
      connection.setEdgeDao(edgeDao);


      return connection;
    } catch (Exception e) {
      throw new GraphConnectionFactoryException("Failed to connect to Entanglement", e);
    }
  }

}
