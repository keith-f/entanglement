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

import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.util.UidGenerator;

import java.util.logging.Logger;

/**
 * A convenience utility for managing short-lived graph connections (such as temporary result stores for
 * graph iteration operations).
 *
 * User: keith
 * Date: 27/09/13; 12:07
 *
 * @author Keith Flanagan
 */
public class TmpGraphConnectionFactory {
  private static final Logger logger = Logger.getLogger(TmpGraphConnectionFactory.class.getName());


  public GraphConnection createTemporaryGraph(String tempClusterName)
      throws GraphConnectionFactoryException {
    return createTemporaryGraph(tempClusterName, GraphConnectionFactory.DEFAULT_TMP_DB_NAME);
  }

  public GraphConnection createTemporaryGraph(String tempClusterName, String tempDbName)
      throws GraphConnectionFactoryException {
    GraphConnectionFactory factory = new GraphConnectionFactory(tempClusterName, tempDbName);
    GraphConnection conn = factory.connect("tmp_"+ UidGenerator.generateUid(), "trunk");

    logger.info("Created temporary graph: "+conn.getGraphName());
    return conn;
  }

  public void disposeOfTempGraph(GraphConnection tmpConnection) throws GraphConnectionFactoryException {
    if (tmpConnection == null) {
      return;
    }
    logger.info("Attempting to drop datastructures relating to temporary graph: " + tmpConnection.getGraphName());
    try {
      if (!tmpConnection.getGraphName().startsWith("tmp_")) {
        throw new GraphConnectionFactoryException("Will not dispose of graph: "+tmpConnection.getGraphName()
            + " since (based on its name), it does not appear to be a temporary connection. This is a failsafe feature.");
      }
      tmpConnection.getRevisionLog().getRevLogCol().drop();
      tmpConnection.getNodeDao().getCollection().drop();
      tmpConnection.getEdgeDao().getCollection().drop();
    } catch (Exception e) {
      throw new GraphConnectionFactoryException(
          "Failed to dispose of one or more temporary graph collections: "+e.getMessage());
    }

  }

  public void disposeOfTempGraphSilent(GraphConnection tmpConnection) {
    try {
      disposeOfTempGraph(tmpConnection);
    } catch (Exception e) {
      logger.warning("Failed to dispose of graph: "+tmpConnection.getDatabaseName()+"/"+tmpConnection.getGraphName());
      e.printStackTrace();
    }
  }
}
