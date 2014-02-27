/*
 * Copyright 2012 Keith Flanagan
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
 * File created: 08-Nov-2012, 13:49:25
 */

package com.entanglementgraph.examples.biggraph;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.graph.commands.EdgeUpdate;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.commands.NodeUpdate;
import com.entanglementgraph.graph.couchdb.CouchGraphConnection;
import com.entanglementgraph.graph.couchdb.CouchGraphConnectionFactory;
import com.entanglementgraph.graph.couchdb.RevisionLogCouchDBImpl;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.util.UidGenerator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A simple program to generate random graphs. Useful for testing raw throughput of databases.
 *
 * @author Keith Flanagan
 */
public class BigGraphBuilder
{
  private final BotLogger logger;
  private final GraphConnection conn;
  private final BufferedWriter logFileOutput;

  // Graph construction properties
  private final int numRootNodes;
  private final double probabilityOfGeneratingChildNode;
  private final int maxChildNodesPerRoot;

  // Used for benchmarks
  private long benchmarkStartTimestamp;
  private long lastCommitTimestamp;
  private long totalOps = 0;
  private long parentNodes = 0;
  private long childNodes = 0;
  private long edgeCount = 0;
  private int maxPatchSetSize = 10000;


  public BigGraphBuilder(BotLogger logger, GraphConnection conn, BufferedWriter logFileOutput, int numRootNodes,
                         double probabilityOfGeneratingChildNode, int maxChildNodesPerRoot) {
    this.logger = logger;
    this.conn = conn;
    this.logFileOutput = logFileOutput;
    this.numRootNodes = numRootNodes;
    this.probabilityOfGeneratingChildNode = probabilityOfGeneratingChildNode;
    this.maxChildNodesPerRoot = maxChildNodesPerRoot;
  }

  private String createLogHeader() {

    return "Time since start"
        +"\tTime since last commit (s)"
        +"\t"+"Total docs"
        +"\t"+"Total ops"
        +"\t"+"Total node updates"
        +"\t"+"Total edge updates"
        +"\t"+"Submit time (s)"
        +"\t"+"Index time total (s)"
        +"\t"+"Index time nodes (s)"
        +"\t"+"Index time edges (s)"

        +"\t"+"Avg ops per second"
        +"\t"+"Avg nodes per second"
        +"\t"+"Avg edges per second"
    ;
  }

  private void printCouchSpecificBenchmarkInfo(int numOpsCommittedLastTime) throws IOException {
    if (!(conn instanceof CouchGraphConnection)) {
      return;
    }

    CouchGraphConnection couchConn = (CouchGraphConnection) conn;
    RevisionLogCouchDBImpl revLog = (RevisionLogCouchDBImpl) conn.getRevisionLog();
    long now = System.currentTimeMillis();
    double timeSinceLastCommit = (now - lastCommitTimestamp) / 1000d;
    double timeSinceStart = (now - benchmarkStartTimestamp) / 1000d;
    lastCommitTimestamp = now;


    double totalSecondsSinceStart = (now - benchmarkStartTimestamp) / 1000d;
    double opsPerSec = revLog.getTotalOpsSubmitted() / totalSecondsSinceStart;
    double nodeUpdatesPerSec = revLog.getTotalNodeUpdates() / totalSecondsSinceStart;
    double edgeUpdatesPerSec = revLog.getTotalEdgeUpdates() / totalSecondsSinceStart;

    String benchLine =
        timeSinceStart
        +"\t"+timeSinceLastCommit
        +"\t"+revLog.getTotalDocsSubmitted()
        +"\t"+revLog.getTotalOpsSubmitted()
        +"\t"+revLog.getTotalNodeUpdates()
        +"\t"+revLog.getTotalEdgeUpdates()
        +"\t"+revLog.getTotalMsSpentSubmitting() / 1000d
        +"\t" +couchConn.getIndexer().getTotalMsSpentIndexing() / 1000d
        +"\t" +couchConn.getIndexer().getTotalMsSpentIndexingNodes() / 1000d
        +"\t" +couchConn.getIndexer().getTotalMsSpentIndexingEdges() / 1000d

        +"\t"+opsPerSec
        +"\t"+nodeUpdatesPerSec
        +"\t"+edgeUpdatesPerSec
    ;

    logger.println(benchLine);
    logFileOutput.append(benchLine).append("\n");
    logFileOutput.flush();
  }

  private void doCommit(List<GraphOperation> ops) throws RevisionLogException, IOException {
    int opsThisTime = ops.size();
    totalOps = totalOps + ops.size();
    TxnUtils.submitAsTxn(conn, ops);
    ops.clear();

    printCouchSpecificBenchmarkInfo(opsThisTime);
  }

  public void run() {
    try {
      logger.println("Starting execution.");
      String headerText = createLogHeader();
      logger.println(headerText);
      logFileOutput.append(headerText).append("\n");

      List<GraphOperation> ops = new LinkedList<>();
      EntityKeys lastRootNode = null;
      benchmarkStartTimestamp = System.currentTimeMillis();
      lastCommitTimestamp = benchmarkStartTimestamp;
      for (int i=0; i<numRootNodes; i++) {
        if (i % 100000 == 0) {
          logger.println("\nGenerated " + i + " of " + numRootNodes + " root nodes so far (" + totalOps + " total graph operations)");
        }
        if (ops.size() >= maxPatchSetSize) {
          doCommit(ops);
        }
        ParentNodeData newRootData = new ParentNodeData();
        newRootData.setDescription(String.valueOf(Math.random()));
        Node newRoot = new Node(new EntityKeys(ParentNodeData.getTypeName(), UidGenerator.generateUid()), newRootData);
        ops.add(new NodeUpdate(newRoot));
        parentNodes++;

        // Generate a link to the previous root node
        if (lastRootNode != null) {
          //Create edge
          SomeEdgeData edgeData = new SomeEdgeData();
          edgeData.setDescription(String.valueOf(Math.random()));

          Edge edge = new Edge(new EntityKeys(SomeEdgeData.getTypeName(), UidGenerator.generateUid()), edgeData);
          edge.setFrom(lastRootNode);
          edge.setTo(newRoot.getKeys());

          ops.add(new EdgeUpdate(edge));
          edgeCount++;
        }
        lastRootNode = newRoot.getKeys();

        //Randomly generate child node(s) for the current root.
        for (int c=0; c<maxChildNodesPerRoot; c++) {
          if (Math.random() < probabilityOfGeneratingChildNode) {
            ChildNodeData childNodeData = new ChildNodeData();
            childNodeData.setDescription(String.valueOf(Math.random()));
            Node childNode = new Node(new EntityKeys(ChildNodeData.getTypeName(), UidGenerator.generateUid()), childNodeData);
            ops.add(new NodeUpdate(childNode));
            childNodes++;

            //Create link to parent
            SomeEdgeData parentToChildData = new SomeEdgeData();
            parentToChildData.setDescription(String.valueOf(Math.random()));

            Edge parentToChild = new Edge(new EntityKeys(HasChildData.getTypeName(), UidGenerator.generateUid()), parentToChildData);
            parentToChild.setFrom(newRoot.getKeys());
            parentToChild.setTo(childNode.getKeys());

            ops.add(new EdgeUpdate(parentToChild));
            edgeCount++;
          }
        }
      }
      logger.println("Done dataset generation. Committing final "+ops.size()+" graph operations.");
      doCommit(ops);

      long benchmarkEndedTimestamp = System.currentTimeMillis();
      double secs = (benchmarkEndedTimestamp - benchmarkStartTimestamp) / 1000d;
      double opsPerSec = totalOps / secs;

      logger.println("Done graph creation. Processed "+totalOps+" operations in "+secs+" seconds.");
      logger.println("Created "+parentNodes+" parent nodes, "+childNodes+" child nodes, "+edgeCount+" edges.");
      logger.println("Ops per second: "+opsPerSec);
    } catch (Exception e) {
      logger.warnln("Benchmark failed: "+e.getMessage());
      logger.printException("Benchmark failed", e);
    }
  }

}
