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

import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.revlog.commands.*;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.entanglementgraph.util.TxnUtils;
import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.util.UidGenerator;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.entanglementgraph.revlog.RevisionLogException;

/**
 *
 * @author Keith Flanagan
 */
public class TestGraph1
{
  private static class Chromosome extends Node {
    private int length;
    private String description;

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

  private static class Gene extends Node {
    private String description;

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  private static class ExistsWithin extends Edge<Gene, Chromosome> {

  }

  public static void main(String[] args) throws UnknownHostException, RevisionLogException, GraphConnectionFactoryException, DbObjectMarshallerException {
    if (args.length != 1) {
      System.out.println("USAGE:\n"
          + "  * database name\n"
      );
      System.exit(1);
    }

    String hostname = "localhost";
    String databaseName = args[0];



    GraphConnectionFactory connFact = new GraphConnectionFactory(hostname, databaseName);
    GraphConnection chromConn = connFact.connect("chromosomes", "trunk");
    GraphConnection genesConn = connFact.connect("genes", "trunk");

    // Populate chromosomes graph
    String txnId = TxnUtils.beginNewTransaction(chromConn);
    List<GraphOperation> ops = new LinkedList<>();
    for (int i=0; i<3; i++) {
      Chromosome chromosome = new Chromosome();
      chromosome.getKeys().setType(Chromosome.class.getName());
      chromosome.getKeys().addName("c" + i);
      chromosome.getKeys().addUid(UidGenerator.generateUid());
      chromosome.setDescription("This is chromosome " + i);
      ops.add(NodeModification.create(chromConn, MergePolicy.APPEND_NEW__LEAVE_EXISTING, chromosome));
    }
    chromConn.getRevisionLog().submitRevisions(chromConn.getGraphName(), chromConn.getGraphBranch(), txnId, 1, ops);
    TxnUtils.commitTransaction(chromConn, txnId);


    // Populate genes graph
    txnId = TxnUtils.beginNewTransaction(genesConn);
    ops = new LinkedList<>();
    for (int i=0; i<3; i++) {
      Gene gene = new Gene();
      gene.getKeys().setType(Gene.class.getName());
      gene.getKeys().addName("g" + i);
      gene.getKeys().addUid(UidGenerator.generateUid());
      gene.setDescription("This is gene " + i);
      ops.add(NodeModification.create(genesConn, MergePolicy.APPEND_NEW__LEAVE_EXISTING, gene));

      // Create a hanging edge between this gene and a chromosome.
      // Note that the chromosome doesn't exist in the GENES graph.
      ExistsWithin geneToChrom = new ExistsWithin();
      geneToChrom.getKeys().setType(ExistsWithin.class.getName());
      geneToChrom.getKeys().addUid(UidGenerator.generateUid());

      /*
       * One or both of the connected nodes doesn't exist in this graph. We can assert this because, by design,
       * the gene graph doesn't contain chromosome nodes.
       */
      geneToChrom.setHanging(true);
      geneToChrom.setFrom(gene.getKeys()); //Set the 'from' node
      //Set the 'to' node. Note that we don't know the chromosome's UID, but we do know its type and one of its names
      geneToChrom.setTo(new EntityKeys(Chromosome.class.getName(), "c1"));
      ops.add(EdgeModification.create(genesConn, MergePolicy.APPEND_NEW__LEAVE_EXISTING, true, geneToChrom));
    }
    genesConn.getRevisionLog().submitRevisions(genesConn.getGraphName(), genesConn.getGraphBranch(), txnId, 1, ops);
    TxnUtils.commitTransaction(genesConn, txnId);


    /*
     * We now have two graphs:
     * Chromosomes: contains 3 nodes
     * Genes: contains 3 nodes, 3 (hanging edges)
     *
     * Next, try importing these graphs into a third 'integrated' graph:
     */
    GraphConnection mergedConn = connFact.connect("merged", "trunk");

    txnId = TxnUtils.beginNewTransaction(mergedConn);
    ops = new LinkedList<>();
    ops.add(new BranchImport("chromosomes", "trunk"));
    ops.add(new BranchImport("genes", "trunk"));
    mergedConn.getRevisionLog().submitRevisions(mergedConn.getGraphName(), mergedConn.getGraphBranch(), txnId, 1, ops);
    TxnUtils.commitTransaction(mergedConn, txnId);

    System.out.println("\n\nDone.");
  }
  
  private static void listCollections(DB db)
  {
    Set<String> colls = db.getCollectionNames();

    for (String s : colls) {
        System.out.println(s);
    }
  }
  
  private static void findOne(DBCollection collection)
  {
    DBObject myDoc = collection.findOne();
    System.out.println(myDoc);
  }
  
  private static void cursorIterateAllDocs(DBCollection collection)
  {
    DBCursor cursor = collection.find();
    try
    {
      while (cursor.hasNext())
      {
        System.out.println(cursor.next());
      }
    }
    finally
    {
      cursor.close();
    }
  }
}
