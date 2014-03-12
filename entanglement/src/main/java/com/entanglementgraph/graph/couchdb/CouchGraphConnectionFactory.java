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

import java.util.*;
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
   * A set of database cluster names --> set {access URLs (eg, http://localhost:5984) }.
   * http://bigcouch.cloudant.com/use
   * According to this, we can use any URL of any machine in the cluster.
   */
  private static final Map<String, Set<String>> couchClusters = new HashMap<>();

  public static void registerNamedCluster(String clusterName, String... couchUrls)
      throws GraphConnectionFactoryException {
    Set<String> couchUrlSet = new HashSet<>(Arrays.asList(couchUrls));
    synchronized (couchClusters) {
      if (couchClusters.containsKey(clusterName)) {
        logger.info(String.format("A CouchDB cluster with the name: %s already exists. " +
            "Reconfiguring with new server set: %s", clusterName, Arrays.asList(couchUrlSet)));
      }

      couchClusters.put(clusterName, couchUrlSet);
    }
  }

  public static Set<String> getNamedClusterUrls(String clusterName) {
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
  private final Set<String> clusterUrls;
  private final String databaseName;
  private final Map<Class, String> classJsonMappings;

  public CouchGraphConnectionFactory(String clusterName, String databaseName, Map<Class, String> classJsonMappings) {
    this.clusterName = clusterName;
    this.clusterUrls = getNamedClusterUrls(clusterName);
    this.databaseName = databaseName;
    this.classJsonMappings = classJsonMappings;
  }

  public GraphConnection connect(String graphName) throws GraphConnectionFactoryException {
    if (clusterUrls == null) {
      throw new GraphConnectionFactoryException("Unknown CouchDB cluster name: "+clusterName);
    }
    try {
      List<String> urlList = new ArrayList<>(clusterUrls);
      Collections.shuffle(urlList);
      String chosenUrl = urlList.iterator().next();
      logger.info("Connecting to: " + chosenUrl + " (out of a choice of "+clusterUrls.size()+" servers), " +
          "database: "+ databaseName + ", graph: " + graphName);

      System.setProperty("org.ektorp.support.AutoUpdateViewOnChange", "true");


      HttpClient httpClient = new StdHttpClient.Builder()
          .url(chosenUrl)
//        .username("admin")
//        .password("secret")
          .socketTimeout(600000)  // Default is 10000
          .build();

      ExtStdObjectMapperFactory omFactory = new ExtStdObjectMapperFactory(classJsonMappings);
      CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient, omFactory);

      // CouchDbConnector is thread-safe and can be shared for simultaneous queries.
      // Any methods that return streaming results should ultimately be 'closed' by the caller
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

      ForceIndexAfterSubmitListener indexer = new ForceIndexAfterSubmitListener(nodeDao, edgeDao);
//      revLog.addListener(indexer);
      connection.setIndexer(indexer);


      return connection;
    } catch (Exception e) {
      throw new GraphConnectionFactoryException("Failed to connect to Entanglement", e);
    }
  }

}
