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

package com.entanglementgraph.shell;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 27/03/2013
 * Time: 11:36
 * To change this template use File | Settings | File Templates.
 */
public class PerformanceLogger
    implements  Runnable {
  private static final Logger logger = Logger.getLogger(PerformanceLogger.class.getName());

  private final Mongo mongo;
  private final DB db;
  private final BufferedWriter log;

  public PerformanceLogger(Mongo mongo, DB db, BufferedWriter log) {
    this.mongo = mongo;
    this.db = db;
    this.log = log;
  }


  @Override
  public void run() {
    String time = new Date(System.currentTimeMillis()).toString();
    int servers = mongo.getServerAddressList().size();
    Set<String> collections = db.getCollectionNames();

    long revisionsItemCount = 0;
    long edgeCount = 0;
    long nodeCount = 0;
    int graphCount = 0;
    for (String colName : collections) {
      DBCollection col = db.getCollection(colName);
      if (colName.equals("revisions")) {
        revisionsItemCount = revisionsItemCount + col.count();
      } else if (colName.endsWith("_edges")) {
        edgeCount = edgeCount + col.count();
      } else if (colName.endsWith("_nodes")) {
        nodeCount = nodeCount + col.count();
        graphCount++;
      } else {
        logger.info("Unknown collection type: "+colName);
      }
      logger.info("Col stats: " + col.getStats().toMap());
    }

    StringBuilder logLine = new StringBuilder();

//    logger.info("DB stats: " + db.getStats().toMap());
    /*
     * Looks like this:
     * INFO: DB stats: {serverUsed=localhost/127.0.0.1:27017, db=aries, collections=39, objects=20842268,
     * avgObjSize=867.1648799449273, dataSize=1.8073682828E10, storageSize=2.0321509344E10, numExtents=453, indexes=256,
     * indexSize=1.0176397392E10, fileSize=3.6423335936E10, nsSizeMB=16, dataFileVersion={ "major" : 4 , "minor" : 5},
     * ok=1.0}
     */
    Map<String, Object> dbStats = db.getStats().toMap();

    logLine.append(time).append("\t");
    logLine.append(servers).append("\t");
    logLine.append(mongo.getDatabaseNames().size()).append("\t");
    logLine.append(collections.size()).append("\t");
    logLine.append(dbStats.get("indexes")).append("\t");
    logLine.append(dbStats.get("objects")).append("\t");
    logLine.append(dbStats.get("avgObjSize")).append("\t");
    logLine.append(dbStats.get("fileSize")).append("\t");
    logLine.append(graphCount).append("\t");
    logLine.append(nodeCount).append("\t");
    logLine.append(edgeCount).append("\t");
    logLine.append("\n");

    try {
      log.write(logLine.toString());
      log.flush();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

  }

  public static String getHeader()
  {
    StringBuilder logLine = new StringBuilder();
    logLine.append("Time").append("\t");
    logLine.append("Num servers").append("\t");
    logLine.append("Num DBs").append("\t");
    logLine.append("Num collections").append("\t");
    logLine.append("Num indexes").append("\t");
    logLine.append("Num objects").append("\t");
    logLine.append("Average object size (bytes)").append("\t");
    logLine.append("File size (bytes)").append("\t");
    logLine.append("Num graphs").append("\t");
    logLine.append("Total nodes").append("\t");
    logLine.append("Total edges").append("\t");
    logLine.append("\n");

    return logLine.toString();
  }
}
