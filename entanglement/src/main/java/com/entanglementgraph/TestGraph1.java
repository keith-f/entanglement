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

package com.entanglementgraph;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.graph.commands.EdgeModification;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.commands.NodeModification;
import com.entanglementgraph.graph.couchdb.CouchGraphConnectionFactory;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.TxnUtils;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.util.UidGenerator;
import java.net.UnknownHostException;
import java.util.*;

/**
 *
 * @author Keith Flanagan
 */
public class TestGraph1
{
  private static class Chromosome implements Content {
    private int length;
    private String description;

    @Override
    public String toString() {
      return "Chromosome{" +
          "length=" + length +
          ", description='" + description + '\'' +
          '}';
    }

    public int getLength() {
      return length;
    }

    public void setLength(int length) {
      this.length = length;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  private static class Gene implements Content {
    private String description;

    @Override
    public String toString() {
      return "Gene{" +
          "description='" + description + '\'' +
          '}';
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  private static class ExistsWithin implements Content {

  }

  public static void main(String[] args) throws UnknownHostException, RevisionLogException, GraphConnectionFactoryException, DbObjectMarshallerException {

    String clusterName = "local";
    String databaseName = "TestGraph1";
    CouchGraphConnectionFactory.registerNamedCluster(clusterName, "http://localhost:5984");


    Map<Class, String> mappings = new HashMap<>();
    mappings.put(Chromosome.class, "Chromosome");
    mappings.put(Gene.class, "Gene");
    mappings.put(ExistsWithin.class, "ExistsWithin");

    CouchGraphConnectionFactory connFact = new CouchGraphConnectionFactory(clusterName, databaseName, mappings);
    GraphConnection graphConn1 = connFact.connect("graph1");
//    GraphConnection genesConn = connFact.connect("genes");

    // Populate chromosomes
    String txnId = TxnUtils.beginNewTransaction(graphConn1);
    List<GraphOperation> ops = new LinkedList<>();
    List<String> chromosomeNames = new ArrayList<>();
    for (int i=0; i<3; i++) {
      Chromosome chromData = new Chromosome();
      chromData.setDescription("This is chromosome " + i);
      chromData.setLength((int) Math.random()*100000);
      Node chromNode = new Node(new EntityKeys("Chromosome", "c" + i), chromData);

      ops.add(new NodeModification<>(MergePolicy.APPEND_NEW__LEAVE_EXISTING, chromNode));
      chromosomeNames.addAll(chromNode.getKeys().getNames());
    }
    TxnUtils.submitTxnPart(graphConn1, txnId, 1, ops);
    TxnUtils.commitTransaction(graphConn1, txnId);


    // Populate genes
    ops = new LinkedList<>();
    for (int i=0; i<3; i++) {
      Gene geneData = new Gene();
      geneData.setDescription("This is gene " + i);

      Node geneNode = new Node(new EntityKeys("Gene", "g" + i), geneData);
      ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, geneNode));

      // Create a hanging edge between this gene and a chromosome.
      // Note that the chromosome doesn't exist in the GENES graph.

      Edge geneToChrom = new Edge();
      geneToChrom.getKeys().setType(ExistsWithin.class.getName());
      geneToChrom.getKeys().addUid(UidGenerator.generateUid());

      geneToChrom.setFrom(geneNode.getKeys()); //Set the 'from' node
      //Set the 'to' node. Note that we don't know the chromosome's UID, but we do know its type and one of its names
      Collections.shuffle(chromosomeNames);
      geneToChrom.setTo(new EntityKeys(Chromosome.class.getName(), chromosomeNames.iterator().next()));
      ops.add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, geneToChrom));
    }
    TxnUtils.submitAsTxn(graphConn1, ops);


    /*
     * We now have two graphs:
     * Chromosomes: contains 3 nodes
     * Genes: contains 3 nodes, 3 (hanging edges)
     *
     * Next, try importing these graphs into a third 'integrated' graph:
     */
//    GraphConnection mergedConn = connFact.connect("merged", "trunk");
//
//    txnId = TxnUtils.beginNewTransaction(mergedConn);
//    ops = new LinkedList<>();
//    ops.add(new BranchImport("chromosomes", "trunk"));
//    ops.add(new BranchImport("genes", "trunk"));
//    mergedConn.getRevisionLog().submitRevisions(mergedConn.getGraphName(), mergedConn.getGraphBranch(), txnId, 1, ops);
//    TxnUtils.commitTransaction(mergedConn, txnId);

    System.out.println("\n\nDone.");
  }

}
