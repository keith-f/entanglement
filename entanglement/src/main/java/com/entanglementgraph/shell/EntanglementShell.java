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
 * File created: 04-Dec-2012, 14:42:02
 */

package com.entanglementgraph.shell;

import static com.entanglementgraph.shell.EntanglementStatePropertyNames.*;
import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import com.entanglementgraph.cli.export.MongoToGephiExporter;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.revlog.commands.*;
import com.entanglementgraph.util.TxnUtils;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.torrenttamer.mongodb.MongoDbFactory;
import com.torrenttamer.mongodb.MongoDbFactoryException;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.util.UidGenerator;

import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.revlog.data.RevisionItemContainer;
import com.entanglementgraph.shell.gdfexport.GraphToGDFExporter;
import com.entanglementgraph.shell.navigator.NavigatorShell;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;

/**
 * A simple interactive command line shell for MongoGraph. This program may be
 * useful for debugging graph operations or for manipulating a live system.
 * 
 * @author Keith Flanagan
 */
public class EntanglementShell
{
  private static final Logger logger =
          Logger.getLogger(EntanglementShell.class.getName());

  private ScheduledThreadPoolExecutor exe;

  private ShellState state;

  private GraphConnectionFactory factory;
  private GraphConnection graphConn;

  private final ClassLoader classLoader;
  private final DbObjectMarshaller marshaller;
  

  public static void main(String[] args) throws IOException
  {    
    Shell shell = create(null);
    shell.commandLoop();
  }
  
  public static Shell create(Map<String, String> additionalProps) throws IOException
  {
    StateUtils stateUtils = new StateUtils();
    ShellState state = stateUtils.loadStateIfExists();
    if (additionalProps != null) {
      state.getProperties().putAll(additionalProps);
    }

    Shell shell = ShellFactory.createConsoleShell(
            "entanglement", "\n\nWelcome to Entanglement - let the hairballs commence!\n", new EntanglementShell(state));
    shell.setDisplayTime(true);
    return shell;
  }

  public EntanglementShell(ShellState state) {
    this.state = state;
    this.exe = new ScheduledThreadPoolExecutor(10);
    classLoader = EntanglementShell.class.getClassLoader();
    marshaller = ObjectMarshallerFactory.create(classLoader);
  }
  
  

  @Command
  public String save() throws IOException {
    StateUtils stateUtils = new StateUtils();
    stateUtils.saveState(state);

    return "Saved.";
  }

  @Command
  public String propSet(String name, String value) {
    String old = state.getProperties().put(name, value);

    if (old == null) {
      return "Setting "+name+" --> "+value;
    } else {
      return "Setting "+name+" --> "+value + " (old value was: "+old+")";
    }
  }
  @Command
  public String propUnset(String name) {
    String old = state.getProperties().remove(name);

    if (old == null) {
      return "Unsetting" + name;
    } else {
      return "Unsetting " + name + " (old value was: "+old+")";
    }
  }

  @Command
  public String propGet(String name) {
    return state.getProperties().get(name);
  }

  @Command
  public String propList() {
    StringBuilder text = new StringBuilder();
    List<String> names = new ArrayList<>(state.getProperties().keySet());
    Collections.sort(names);
    for (String name : names) {
      String val = state.getProperties().get(name);
      text.append("    ").append(name).append(" ---> ").append(val).append("\n");
    }
    text.append("Total properties set: ").append(state.getProperties().size()).append("\n");
    return text.toString();
  }
  
  @Command
  public void reconnect() throws RevisionLogException, MongoDbFactoryException, GraphConnectionFactoryException {
    String hostname = state.getProperties().get(PROP_HOSTNAME);
    String database = state.getProperties().get(PROP_DB_NAME);
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);

    factory = new GraphConnectionFactory(classLoader, hostname, database);
    GraphConnection connection = factory.connect(graphName, branchName);
    graphConn = connection;

    logger.info("Connected to "+graphName+"/"+branchName+"!");
  }
  
  @Command
  public void startNavigator(String nodeUid)
          throws IOException, GraphModelException {
    NavigatorShell navShell = new NavigatorShell(graphConn, nodeUid);
    NavigatorShell.startSubShell(navShell);
  }
  @Command
  public void startNavigator(String nodeType, String nodeName) 
          throws IOException, GraphModelException {
    //FIXME - we need to go through this code and use keys instead of UIDs.
    graphConn.getNodeDao().getEntityKeysetForName(nodeType, nodeName).getUids().iterator().next();
    EntityKeys nodeKeyset = graphConn.getNodeDao().getEntityKeysetForName(nodeType, nodeName);
    Set<String> uids = nodeKeyset.getUids();
    String nodeUid = uids.iterator().next();
    NavigatorShell navShell = new NavigatorShell(graphConn, nodeUid);
    NavigatorShell.startSubShell(navShell);
  }
  
  @Command
  public void listGraphOperations() throws IntrospectionException {
    ServiceLoader<GraphOperation> loader = ServiceLoader.load(GraphOperation.class);
    
    StringBuilder txt = new StringBuilder("Available operations:\n");
    for (GraphOperation operation : loader) {
      txt.append("  * ").append(operation.getClass().getSimpleName()).append(":: ");
      BeanInfo info = Introspector.getBeanInfo(operation.getClass());
      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
        //Ignore certain common properties.
        switch (pd.getName()) {
          case "class" : 
            continue;
        }
        Method getter = pd.getReadMethod();
        Class returnType = getter.getReturnType();
        
        txt.append(pd.getName()).append("(").append(returnType.getSimpleName()).append(") ");
        
        
//        Object val = getter.invoke(bean);

//        propertyToValue.put(pd.getName(), val);
//        AttributeName attType = 
//            MetaGraphUtils.loadAttributeType(config, pd.getName());
//        annotations.add(new PropertyAnnotation(attType.getId(), val));
      }
      txt.append("\n");
    }
    System.out.println(txt);
  }

  @Command(description = "Imports the specified graph content into the current graph")
  public void importGraph(@Param(name = "srcGraphName")   String srcGraphName,
                          @Param(name = "srcGraphBranch") String srcGraphBranch)
      throws GraphConnectionFactoryException, RevisionLogException {
    String txnId = TxnUtils.beginNewTransaction(graphConn);
    List ops = new LinkedList<>();
    ops.add(new BranchImport(srcGraphName, srcGraphBranch));

    graphConn.getRevisionLog().submitRevisions(graphConn.getGraphName(), graphConn.getGraphBranch(), txnId, 1, ops);
    TxnUtils.commitTransaction(graphConn, txnId);
  }
  
  
  
  
  /*
   * ---------------------------------------------------------------------------
   * Revision log queries
   * ---------------------------------------------------------------------------
   */

  
  @Command
  public void listRevisions() {
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    logger.info("Listing revisions for: "+graphName+"/"+branchName);
    
    int count = 0;
    for (RevisionItemContainer container : graphConn.getRevisionLog().iterateCommittedRevisionsForGraph(graphName, branchName))
    {
      StringBuilder txt = new StringBuilder("  Rev: ");
      txt.append(container.getTransactionUid()).append(", ").append(container.getTxnSubmitId()).append(", ");
      txt.append(container.getTimestamp()).append(", ");
      txt.append("RevisionItems: ").append(container.getItems().size());
//      txt.append(container.getOperation().getClass().getSimpleName()).append(", ");
//      txt.append(container.getOperation().toString());
      txt.append("\n");
      
      System.out.print(txt);
      count++;
    }
    System.out.println("\nPrinted "+count+" revisions.");
  }
  
  @Command
  public void exportRevLogAsAsciiArt()
  {
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    
    AsciiArtPrinter.printGraphAsAsciiArt(graphName, branchName, graphConn.getRevisionLog());
  }

  @Command
  public void exportGraphAsGdf(File outputFile)
           throws IOException, GraphModelException, RevisionLogException
  {
    exportGraphAsGdf(outputFile, null);
  }
  
  @Command
  public void exportGraphAsGdf(File outputFile, File colorPropsFile)
           throws IOException, GraphModelException, RevisionLogException
  {
    GraphToGDFExporter exporter = new GraphToGDFExporter(graphConn);
    exporter.setColorPropsFile(colorPropsFile);
    exporter.setOutputFile(outputFile);
    exporter.writeToFile();
    System.out.println("Done.");
  }
  
  @Command
  public void exportGraphAsGdf()
          throws IOException, GraphModelException, RevisionLogException
  {
    GraphToGDFExporter exporter = new GraphToGDFExporter(graphConn);
    System.out.println(exporter.writeToString());
    System.out.println("Done.");
  }

  @Command
  public void exportGraphAsGexf(File outputFile)
      throws IOException, GraphModelException, RevisionLogException, DbObjectMarshallerException {
    MongoToGephiExporter exporter = new MongoToGephiExporter(graphConn, null);
    exporter.exportAll(outputFile);
    System.out.println("Done.");
  }

  @Command
  public void exportGraphAsGexf(
      @Param(name="outputFile")
      File outputFile,
      @Param(name="nodeToColorMapping")
      File nodeToColorMapping)
      throws IOException, GraphModelException, RevisionLogException, DbObjectMarshallerException {
    Map<String, Color> nodeToColors = MongoToGephiExporter.loadColorMappings(nodeToColorMapping);
    MongoToGephiExporter exporter = new MongoToGephiExporter(graphConn, nodeToColors);
    exporter.exportAll(outputFile);
    System.out.println("Done.");
  }

  @Command
  public void startLogger(
      @Param(name="outputFile") File outputFile,
      @Param(name="hostname") String hostname,
      @Param(name="database") String database)
      throws IOException, MongoDbFactoryException {
    MongoDbFactory dbFactory = new MongoDbFactory(hostname, database);
    Mongo mongo = dbFactory.createMongoConnection();
    DB db = mongo.getDB(database);
    BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, true));
    bw.write(PerformanceLogger.getHeader());
    PerformanceLogger perfLogger = new PerformanceLogger(mongo, db, bw);
    exe.scheduleWithFixedDelay(perfLogger, 0, 120 * 1000, TimeUnit.MILLISECONDS);
  }
  
 
  
  /*
   * ---------------------------------------------------------------------------
   * Graph queries
   * ---------------------------------------------------------------------------
   */
  
  @Command
  public void findGraphNodeByUid(String uid)
      throws RevisionLogException, GraphModelException {
    DBObject node = graphConn.getNodeDao().getByUid(uid);
    if (node == null) {
      System.out.println("No node with UID: "+uid);
    } else {
      System.out.println("Found node: "+uid);
      System.out.println(node);
    }
  }
  
  @Command
  public void findGraphNodeByName(String nodeType, String wellKnownName)
      throws RevisionLogException, GraphModelException {
    DBObject node = graphConn.getNodeDao().getByName(nodeType, wellKnownName);
    if (node == null) {
      System.out.println("No node with name: "+wellKnownName);
    } else {
      System.out.println("Found node: "+wellKnownName);
      System.out.println(node);
    }
  }
  
  @Command
  public void printPropertiesOfNamedNode(String nodeType, String wellKnownName)
      throws RevisionLogException, GraphModelException {
    DBObject node = graphConn.getNodeDao().getByName(nodeType, wellKnownName);
    if (node == null) {
      System.out.println("No node with name: "+wellKnownName);
      return;
    }
    StringBuilder sb = new StringBuilder("Annotations for node: ");
    sb.append(wellKnownName).append("\n");
    for (String propName : node.keySet()) {
      Object val = node.get(propName);
      sb.append("  * ").append(val.getClass().getName()).append(": ")
              .append(propName).append(" --> ").append(val).append("\n");
    }
    System.out.println(sb);
  }
  
  @Command
  public void listNodeTypes() throws GraphModelException
  {
    List<String> types = graphConn.getNodeDao().listTypes();
    System.out.println(types.size()+" node types are present:");
    for(String type : types) {
      System.out.println("  * "+type);
    }
  }
  
  @Command
  public void listNodeUids(String type, int offset, int limit) throws GraphModelException
  {
    Iterable<EntityKeys> keysItr = graphConn.getNodeDao().iterateKeysByType(type, offset, limit);
    System.out.println("UIDs of nodes with type: "+type);
    for (EntityKeys keys : keysItr) {
      System.out.println("  * "+keys.getType()+": "+keys.getUids()+"; "+keys.getNames());
    }
  }
//  @Command
//  public void listNodeNames(String type, int offset, int limit) throws GraphModelException
//  {
//    Iterable<String> nameItr = nodeDao.iterateNamesByType(type, offset, limit);
//    System.out.println("Names of nodes with type: "+type);
//    for (String name : nameItr) {
//      System.out.println("  * "+name);
//    }
//  }
  
  @Command
  public void countNodes() throws GraphModelException
  {
    long count = graphConn.getNodeDao().count();
    System.out.println("Total nodes: "+count);
  }
  
  @Command
  public void countNodes(String type) throws GraphModelException
  {
    long count = graphConn.getNodeDao().countByType(type);
    System.out.println("Total nodes of type: "+type+": "+count);
  }
  
  /*
   * ---------------------------------------------------------------------------
   * Graph revision log command implementations
   * ---------------------------------------------------------------------------
   */

  
  private int getTxnSubmitIdAndIncrement()
  {
    int txnSubmitId = Integer.parseInt(state.getProperties().get(PROP_TXN_SUBMIT_ID));
    int newSubmitId = txnSubmitId + 1;
    state.getProperties().put(PROP_TXN_SUBMIT_ID, String.valueOf(newSubmitId));
    return txnSubmitId;
  }
  
  
//  @Command
//  public void createEdgeBetweenNamedNodes(String edgeType, String fromNodeName, String toNodeName)
//      throws RevisionLogException
//  {
//    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
//    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
//    String txnId = state.getProperties().get(PROP_TXN_UID);
//    int txnSubmitId = getTxnSubmitIdAndIncrement();
//    
//    String edgeUniqueId = UidGenerator.generateUid();
//    
//    CreateEdgeBetweenNamedNodes op = new CreateEdgeBetweenNamedNodes(
//        edgeType, edgeUniqueId, fromNodeName, toNodeName);
//    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, op);
//  }
  
  
//  @Command
//  public void createNodeIfNotExistsByName(String nodeType, String wellKnownName, String dataSourceUid, String evidenceTypeUid)
//      throws RevisionLogException
//  {
//    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
//    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
//    String txnId = state.getProperties().get(PROP_TXN_UID);
//    int txnSubmitId = getTxnSubmitIdAndIncrement();
//    
//    String nodeUniqueId = UidGenerator.generateUid();
//    
//    CreateNodeIfNotExistsByName op = new CreateNodeIfNotExistsByName(
//        nodeType, nodeUniqueId, wellKnownName, dataSourceUid, evidenceTypeUid);
//    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, op);
//  }
  
  
//  @Command
//  public void createNodeIfNotExistsByName(String nodeType, String wellKnownName)
//      throws RevisionLogException
//  {
//    String dataSourceUid = state.getProperties().get(PROP_DEFAULT_DATASOURCE_NAME);
//    String evidenceTypeUid = state.getProperties().get(PROP_DEFAULT_EVIDENCETYPE_NAME);
//    createNodeIfNotExistsByName(nodeType, wellKnownName, dataSourceUid, evidenceTypeUid);
//  }

//  @Command
//  public void deleteEdgeByUid(String edgeUniqueId)
//      throws RevisionLogException
//  {
//    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
//    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
//    String txnId = state.getProperties().get(PROP_TXN_UID);
//    int txnSubmitId = getTxnSubmitIdAndIncrement();
//    
//    DeleteEdgeByUid op = new DeleteEdgeByUid(edgeUniqueId);
//    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, op);
//  }
  
//  @Command
//  public void deleteNodeByUid(String nodeUniqueId)
//      throws RevisionLogException
//  {
//    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
//    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
//    String txnId = state.getProperties().get(PROP_TXN_UID);
//    int txnSubmitId = getTxnSubmitIdAndIncrement();
//    
//    DeleteEdgeByUid op = new DeleteEdgeByUid(nodeUniqueId);
//    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, op);
//  }
  
//  @Command
//  public void setNamedNodeProperty(String nodeWellKnownName, String propertyName, 
//      String propertyValue)
//      throws RevisionLogException
//  {
//    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
//    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
//    String txnId = state.getProperties().get(PROP_TXN_UID);
//    int txnSubmitId = getTxnSubmitIdAndIncrement();
//    
//    SetNamedNodeProperty op = new SetNamedNodeProperty(
//        nodeWellKnownName, propertyName, propertyValue);
//    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, op);
//  }
  
//  @Command
//  public void setNodeProperty(String nodeUniqueId, String propertyName, 
//      String propertyType, String propertyValue)
//      throws RevisionLogException
//  {
//    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
//    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
//    String txnId = state.getProperties().get(PROP_TXN_UID);
//    int txnSubmitId = getTxnSubmitIdAndIncrement();
//    
//    SetNodeProperty op = new SetNodeProperty(
//        nodeUniqueId, propertyName, propertyType, propertyValue);
//    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, op);
//  }
  
  
  
  /*
   * ---------------------------------------------------------------------------
   * Transaction-related graph revision log operations
   * ---------------------------------------------------------------------------
   */

  @Command
  public void transactionSet(String txnId, int submitId) {
    StringBuilder sb = new StringBuilder();
    
    String currentTxnId = state.getProperties().get(PROP_TXN_UID);
    state.getProperties().put(PROP_TXN_UID, txnId);
    state.getProperties().put(PROP_TXN_SUBMIT_ID, String.valueOf(submitId));
    
    sb.append("Current transaction: ").append(currentTxnId).append("\n");
    sb.append("Setting new transaction ID: ").append(txnId);
    sb.append("\nThe previous transaction has not been committed/rolled back "
        + "- you may return to it by setting its transaction ID.\n");
    System.out.println(sb);
  }
  
  @Command
  public void transactionSet() {
    String txnId = UidGenerator.generateUid();
    
    StringBuilder sb = new StringBuilder();
    
    String currentTxnId = state.getProperties().get(PROP_TXN_UID);
    state.getProperties().put(PROP_TXN_UID, txnId);
    state.getProperties().put(PROP_TXN_SUBMIT_ID, "0");
    
    sb.append("Current transaction: ").append(currentTxnId).append("\n");
    sb.append("Setting new transaction ID: ").append(txnId);
    sb.append("\nThe previous transaction has not been committed/rolled back "
        + "- you may return to it by setting its transaction ID.\n");
    System.out.println(sb);
  }
  
  @Command
  public void sendTransactionBegin() throws RevisionLogException {
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    String txnId = state.getProperties().get(PROP_TXN_UID);
    int txnSubmitId = getTxnSubmitIdAndIncrement();

    TransactionBegin txOp = new TransactionBegin(txnId);
    
    StringBuilder sb = new StringBuilder();
    sb.append("Sending a 'begin transaction' command for transaction ID: ").append(txnId);
    System.out.println(sb);
    graphConn.getRevisionLog().submitRevision(graphName, branchName, txnId, txnSubmitId, txOp);
  }
  
  @Command
  public void sendTransactionRollback() throws RevisionLogException {
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    String txnId = state.getProperties().get(PROP_TXN_UID);
    int txnSubmitId = getTxnSubmitIdAndIncrement();

    TransactionRollback txOp = new TransactionRollback(txnId);
    
    String newTxnId = UidGenerator.generateUid();
    StringBuilder sb = new StringBuilder();
    sb.append("Sending a 'rollback transaction' command for transaction ID: ").append(txnId);
    sb.append("\nFor convenience, now setting a new random transaction ID: "+newTxnId);
    System.out.println(sb);
    graphConn.getRevisionLog().submitRevision(graphName, branchName, txnId, txnSubmitId, txOp);
  }
  
  @Command
  public void sendTransactionCommit() throws RevisionLogException {
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    String txnId = state.getProperties().get(PROP_TXN_UID);
    int txnSubmitId = getTxnSubmitIdAndIncrement();

    TransactionCommit txOp = new TransactionCommit(txnId);
    
    String newTxnId = UidGenerator.generateUid();
    StringBuilder sb = new StringBuilder();
    sb.append("Sending a 'commit transaction' command for transaction ID: ").append(txnId);
    sb.append("\nFor convenience, now setting a new random transaction ID: "+newTxnId);
    System.out.println(sb);
    graphConn.getRevisionLog().submitRevision(graphName, branchName, txnId, txnSubmitId, txOp);
  }
  
}
