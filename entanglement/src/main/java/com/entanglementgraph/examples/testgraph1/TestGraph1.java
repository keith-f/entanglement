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

package com.entanglementgraph.examples.testgraph1;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.graph.commands.EdgeUpdate;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.commands.NodeUpdate;
import com.entanglementgraph.graph.couchdb.CouchGraphConnectionFactory;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.util.UidGenerator;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class TestGraph1
{
  private static final Logger logger = Logger.getLogger(TestGraph1.class.getSimpleName());

  public static void main(String[] args) throws UnknownHostException, RevisionLogException, GraphConnectionFactoryException, DbObjectMarshallerException, GraphModelException, InterruptedException {

    String clusterName = "local";
    String databaseName = "testgraph1";
    CouchGraphConnectionFactory.registerNamedCluster(clusterName, "http://localhost:5984");


    Map<Class, String> mappings = new HashMap<>();
    mappings.put(Chromosome.class, "Chromosome");
    mappings.put(Gene.class, "Gene");
    mappings.put(ExistsWithin.class, "ExistsWithin");

    CouchGraphConnectionFactory connFact = new CouchGraphConnectionFactory(clusterName, databaseName, mappings);
    GraphConnection graphConn1 = connFact.connect("graph1");
//    GraphConnection genesConn = connFact.connect("genes");

    long totalOps = 0;
    long started = System.currentTimeMillis();

    // Populate chromosomes
    String txnId = TxnUtils.beginNewTransaction(graphConn1);
    List<GraphOperation> ops = new LinkedList<>();
    List<String> chromosomeNames = new ArrayList<>();
    for (int i=0; i<3; i++) {
      Chromosome chromData = new Chromosome();
      chromData.setDescription("This is chromosome " + i);
      chromData.setLength((int) Math.random()*100000);
      Node chromNode = new Node(new EntityKeys("Chromosome", "c" + i), chromData);

      ops.add(new NodeUpdate(MergePolicy.APPEND_NEW__LEAVE_EXISTING, chromNode));
      chromosomeNames.addAll(chromNode.getKeys().getNames());
    }
    System.out.println("Committing "+ops.size()+" graph operations.");
    totalOps = totalOps + ops.size();
    TxnUtils.submitTxnPart(graphConn1, txnId, 1, ops);
    TxnUtils.commitTransaction(graphConn1, txnId);


    // Populate genes
    ops = new LinkedList<>();
    for (int i=0; i<3; i++) {
      Gene geneData = new Gene();
      geneData.setDescription("This is gene " + i);

      Node geneNode = new Node(new EntityKeys("Gene", "g" + i), geneData);
      ops.add(new NodeUpdate(MergePolicy.APPEND_NEW__LEAVE_EXISTING, geneNode));

      // Create a hanging edge between this gene and a chromosome.
      // Note that the chromosome doesn't exist in the GENES graph.
      Edge geneToChrom = new Edge("exists-within", new ExistsWithin(Math.random()));
      geneToChrom.getKeys().addUid(UidGenerator.generateUid());


      geneToChrom.setFrom(geneNode.getKeys()); //Set the 'from' node
      //Set the 'to' node. Note that we don't know the chromosome's UID, but we do know its type and one of its names
      Collections.shuffle(chromosomeNames);
      geneToChrom.setTo(new EntityKeys("Chromosome", chromosomeNames.iterator().next()));
      ops.add(new EdgeUpdate(MergePolicy.APPEND_NEW__LEAVE_EXISTING, geneToChrom));
    }
    totalOps = totalOps + ops.size();
    TxnUtils.submitAsTxn(graphConn1, ops);

    long ended = System.currentTimeMillis();
    double secs = (ended - started) / 1000d;
    double opsPerSec = totalOps / secs;

    System.out.println("\n\nDone graph creation. Processed "+totalOps+" in "+secs+" seconds.");
    System.out.println("Ops per second: "+opsPerSec);

    System.out.println("\n\nIterating nodes/edges");

    for (Node node : graphConn1.getNodeDao().iterateAll()) {
      System.out.println(" * Node: "+node.toString());
    }

    for (Edge edge : graphConn1.getEdgeDao().iterateAll()) {
      System.out.println(" * Edge: " + edge.toString());
    }

    System.out.println("\n\nIterating nodes by type:");
    for (Node node : graphConn1.getNodeDao().iterateByType("Gene")) {
      System.out.println(" * Node: "+node.toString()+". Content type: "+node.getContent().getClass().getName());
    }

//    Thread.sleep(5000);
    System.out.println("Iterating edges from g0");
    for (Edge node : graphConn1.getEdgeDao().iterateEdgesFromNode(new EntityKeys("Gene", "g0"))) {
      System.out.println(" * Edge from <-- "+node.toString());
    }

    System.out.println("Iterating edges from c1");
    for (Edge node : graphConn1.getEdgeDao().iterateEdgesFromNode(new EntityKeys("Chromosome", "c1"))) {
      System.out.println(" * Edge to --> "+node.toString());
    }

    System.out.println("Iterating edges to c1");
    for (Edge node : graphConn1.getEdgeDao().iterateEdgesToNode(new EntityKeys("Chromosome", "c1"))) {
      System.out.println(" * Edge to --> "+node.toString());
    }
  }

}
