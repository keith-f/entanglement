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
import com.scalesinformatics.util.UidGenerator;

import java.net.UnknownHostException;
import java.util.*;

/**
 * A simple program to generate random graphs. Useful for testing raw throughput of databases.
 *
 * @author Keith Flanagan
 */
public class BigGraph
{

  public static void main(String[] args) throws UnknownHostException, RevisionLogException, GraphConnectionFactoryException, DbObjectMarshallerException, GraphModelException {
    String clusterName = "local";
    String databaseName = "biggraph";
    CouchGraphConnectionFactory.registerNamedCluster(clusterName, "http://localhost:5984");

    // Define mappings between our content beans and names used by JSON encodings
    Map<Class, String> mappings = new HashMap<>();
    mappings.put(ParentNodeData.class, ParentNodeData.class.getSimpleName());
    mappings.put(ChildNodeData.class, ChildNodeData.class.getSimpleName());
    mappings.put(SomeEdgeData.class, SomeEdgeData.class.getSimpleName());

    CouchGraphConnectionFactory connFact = new CouchGraphConnectionFactory(clusterName, databaseName, mappings);
    GraphConnection graphConn1 = connFact.connect("graph1");

    // Graph construction properties
    int numRootNodes = 5000000;
    double probabilityOfGeneratingChildNode = 0.05;
    int maxChildNodesPerRoot = 5;

    // Used for benchmarks
    long totalOps = 0;
    long parentNodes = 0;
    long childNodes = 0;
    long edgeCount = 0;
    long started = System.currentTimeMillis();
    int maxPatchSetSize = 10000;

    // Populate
    System.out.println("Generating nodes/edge to an in-memory patch set.");
    List<GraphOperation> ops = new LinkedList<>();

    System.out.println("Time since start\tTime since last commit (s)\t"+"Current ops"
        +"\t"+"Total ops"
        +"\t"+"Total docs"
        +"\t"+"Total ops (revlog)"
        +"\t"+"Nodes"
        +"\t"+"Edges"
        +"\t"+"Submit time (s)"
        +"\t"+"Index time total (s)"
        +"\t"+"Index time nodes (s)"
        +"\t"+"Index time edges (s)"
    );

    EntityKeys lastRootNode = null;
    long start = System.currentTimeMillis();
    long lastCommit = start;
    for (int i=0; i<numRootNodes; i++) {
      if (i % 100000 == 0) {
        System.out.println("\nGenerated "+i+" of "+numRootNodes+" root nodes so far ("+totalOps+" total graph operations)");
      }
      if (ops.size() >= maxPatchSetSize) {
//        System.out.print("c");
        int opsThisTime = ops.size();
        totalOps = totalOps + ops.size();
        TxnUtils.submitAsTxn(graphConn1, ops);
        ops.clear();

        // Now try querying the view to force incremental updates (apparently this is faster)
//        System.out.print("i");
        graphConn1.getNodeDao().getByKey(new EntityKeys("Nonexistent...", "foo"));
//        System.out.print(".");
        RevisionLogCouchDBImpl revLog = (RevisionLogCouchDBImpl) graphConn1.getRevisionLog();
        CouchGraphConnection conn = (CouchGraphConnection) graphConn1;
        long now = System.currentTimeMillis();
        double timeSinceLastCommit = (now - lastCommit) / 1000d;
        double timeSinceStart = (now - start) / 1000d;
        lastCommit = now;
        System.out.println(timeSinceStart + "\t" + timeSinceLastCommit +
            "\t"+opsThisTime
            +"\t"+totalOps
            +"\t"+revLog.getTotalDocsSubmitted()
            +"\t"+revLog.getTotalOpsSubmitted()
            +"\t"+revLog.getTotalNodeUpdates()
            +"\t"+revLog.getTotalEdgeUpdates()
            +"\t"+revLog.getTotalMsSpentSubmitting() / 1000d
            +"\t" +conn.getIndexer().getTotalMsSpentIndexing() / 1000d
            +"\t" +conn.getIndexer().getTotalMsSpentIndexingNodes() / 1000d
            +"\t" +conn.getIndexer().getTotalMsSpentIndexingEdges() / 1000d
        );
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
    System.out.println("Done dataset generation. Committing final "+ops.size()+" graph operations.");
    totalOps = totalOps + ops.size();
    TxnUtils.submitAsTxn(graphConn1, ops);

//    TxnUtils.submitAsTxn(graphConn1, ops);

    long ended = System.currentTimeMillis();
    double secs = (ended - started) / 1000d;
    double opsPerSec = totalOps / secs;

    System.out.println("\n\nDone graph creation. Processed "+totalOps+" operations in "+secs+" seconds.");
    System.out.println("Created "+parentNodes+" parent nodes, "+childNodes+" child nodes, "+edgeCount+" edges.");
    System.out.println("Ops per second: "+opsPerSec);

//    System.out.println("\n\nIterating nodes/edges");
//
//    for (Node node : graphConn1.getNodeDao().iterateAll()) {
//      System.out.println(" * Node: "+node.toString());
//    }
//
//    for (Edge node : graphConn1.getEdgeDao().iterateAll()) {
//      System.out.println(" * Edge: "+node.toString());
//    }
  }

}
