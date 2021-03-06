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

package uk.ac.ncl.aries.entanglement.shell;

import com.torrenttamer.mongodb.MongoDbFactory;
import static uk.ac.ncl.aries.entanglement.shell.EntanglementStatePropertyNames.*;
import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.torrenttamer.mongodb.MongoDbFactoryException;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.util.UidGenerator;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.ObjectMarshallerFactory;
import uk.ac.ncl.aries.entanglement.cli.export.MongoGraphToGephi;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.GraphCheckoutNamingScheme;
import uk.ac.ncl.aries.entanglement.graph.InsertMode;
import uk.ac.ncl.aries.entanglement.player.LogPlayer;
import uk.ac.ncl.aries.entanglement.player.LogPlayerMongoDbImpl;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphDAOFactory;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogDirectToMongoDbImpl;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;
import uk.ac.ncl.aries.entanglement.revlog.commands.GraphOperation;
import uk.ac.ncl.aries.entanglement.revlog.commands.SetNamedNodeProperty;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionBegin;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionCommit;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionRollback;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItemContainer;
import uk.ac.ncl.aries.entanglement.shell.gdfexport.GraphToGDFExporter;
import uk.ac.ncl.aries.entanglement.shell.navigator.NavigatorShell;

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

  private ShellState state;
  
  private static Mongo mongo;
  private static DB db;
  private static RevisionLog revLog;
  private static NodeDAO nodeDao;
  private static EdgeDAO edgeDao;

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
  public void reconnect() throws RevisionLogException, MongoDbFactoryException {
    String hostname = state.getProperties().get(PROP_HOSTNAME);
    String database = state.getProperties().get(PROP_DB_NAME);
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    String insertMode = state.getProperties().get(PROP_INSERT_MODE);
    
    logger.info("Connecting to: "+hostname+"/"+database+", graph: "+graphName+"/"+branchName);
    
    MongoDbFactory dbFactory = new MongoDbFactory(hostname, database);
    mongo = dbFactory.createMongoConnection();
    db = mongo.getDB(database);
    
    revLog = new RevisionLogDirectToMongoDbImpl(classLoader, mongo, db);
    
    GraphCheckoutNamingScheme collectionNamer = new GraphCheckoutNamingScheme(graphName, branchName);
    DBCollection nodeCol = db.getCollection(collectionNamer.getNodeCollectionName());
    DBCollection edgeCol = db.getCollection(collectionNamer.getEdgeCollectionName());
    nodeDao = GraphDAOFactory.createDefaultNodeDAO(classLoader, mongo, db, nodeCol, edgeCol);
    edgeDao = GraphDAOFactory.createDefaultEdgeDAO(classLoader, mongo, db, nodeCol, edgeCol);
    
    if (insertMode != null && insertMode.equals(InsertMode.INSERT_CONSISTENCY.name())) {
      System.out.println("Setting DAO insert mode to: "+insertMode);
      nodeDao.setInsertModeHint(InsertMode.INSERT_CONSISTENCY);
      edgeDao.setInsertModeHint(InsertMode.INSERT_CONSISTENCY);
    } else if (insertMode != null && insertMode.equals(InsertMode.INSERT_PERFORMANCE.name())) {
      System.out.println("Setting DAO insert mode to: "+insertMode);
      nodeDao.setInsertModeHint(InsertMode.INSERT_PERFORMANCE);
      edgeDao.setInsertModeHint(InsertMode.INSERT_PERFORMANCE);
    }
    
    logger.info("Connected!");
  }
  
  @Command
  public void startNavigator(String nodeUid)
          throws IOException, GraphModelException {
    NavigatorShell navShell = new NavigatorShell(revLog, nodeDao, edgeDao, nodeUid);
    NavigatorShell.startSubShell(navShell);
  }
  @Command
  public void startNavigator(String nodeType, String nodeName) 
          throws IOException, GraphModelException {
    String nodeUid = nodeDao.lookupUniqueIdForName(nodeType, nodeName);
    NavigatorShell navShell = new NavigatorShell(revLog, nodeDao, edgeDao, nodeUid);
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
    for (RevisionItemContainer container : revLog.iterateCommittedRevisionsForGraph(graphName, branchName))
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
    
    AsciiArtPrinter.printGraphAsAsciiArt(graphName, branchName, revLog);
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
    GraphToGDFExporter exporter = new GraphToGDFExporter(marshaller, revLog, nodeDao, edgeDao);
    exporter.setColorPropsFile(colorPropsFile);
    exporter.setOutputFile(outputFile);
    exporter.writeToFile();
    System.out.println("Done.");
  }
  
  @Command
  public void exportGraphAsGdf()
          throws IOException, GraphModelException, RevisionLogException
  {
    GraphToGDFExporter exporter = new GraphToGDFExporter(marshaller, revLog, nodeDao, edgeDao);
    System.out.println(exporter.writeToString());
    System.out.println("Done.");
  }
  
  @Command
  public void exportGraphAsGexf(File outputFile)
           throws IOException, GraphModelException, RevisionLogException
  {
    MongoGraphToGephi exporter = new MongoGraphToGephi(nodeDao, edgeDao);
    exporter.exportGexf(outputFile);
    System.out.println("Done.");
  }
  
  @Command
  public void exportGraphAsGexf(
          @Param(name="outputFile")
          File outputFile, 
          @Param(name="nodeToColorMapping")
          File nodeToColorMapping) 
           throws IOException, GraphModelException, RevisionLogException
  {
    MongoGraphToGephi exporter = new MongoGraphToGephi(nodeDao, edgeDao);
    exporter.setColorPropsFile(nodeToColorMapping);
    exporter.exportGexf(outputFile);
    System.out.println("Done.");
  }
  
  @Command
  public void playAllRevisions()
      throws RevisionLogException, GraphModelException, LogPlayerException {
    String graphName = state.getProperties().get(PROP_GRAPH_NAME);
    String branchName = state.getProperties().get(PROP_GRAPH_BRANCH_NAME);
    
    System.out.println("Playing all committed revisions from the revision history "
            + "list into a graph structure: "+graphName+"/"+branchName);
    
    LogPlayer player = new LogPlayerMongoDbImpl(classLoader, marshaller,
            graphName, branchName, revLog, nodeDao, edgeDao);
    player.replayAllRevisions();
    
    System.out.println("Done.");
  }
  
 
  
  /*
   * ---------------------------------------------------------------------------
   * Graph queries
   * ---------------------------------------------------------------------------
   */
  
  @Command
  public void findGraphNodeByUid(String uid)
      throws RevisionLogException, GraphModelException {
    DBObject node = nodeDao.getByUid(uid);
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
    DBObject node = nodeDao.getByName(nodeType, wellKnownName);
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
    DBObject node = nodeDao.getByName(nodeType, wellKnownName);
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
    List<String> types = nodeDao.listTypes();
    System.out.println(types.size()+" node types are present:");
    for(String type : types) {
      System.out.println("  * "+type);
    }
  }
  
  @Command
  public void listNodeUids(String type, int offset, int limit) throws GraphModelException
  {
    Iterable<String> uidItr = nodeDao.iterateIdsByType(type, offset, limit);
    System.out.println("UIDs of nodes with type: "+type);
    for (String uid : uidItr) {
      System.out.println("  * "+uid);
    }
  }
  @Command
  public void listNodeNames(String type, int offset, int limit) throws GraphModelException
  {
    Iterable<String> nameItr = nodeDao.iterateNamesByType(type, offset, limit);
    System.out.println("Names of nodes with type: "+type);
    for (String name : nameItr) {
      System.out.println("  * "+name);
    }
  }
  
  @Command
  public void countNodes() throws GraphModelException
  {
    long count = nodeDao.count();
    System.out.println("Total nodes: "+count);
  }
  
  @Command
  public void countNodes(String type) throws GraphModelException
  {
    long count = nodeDao.countByType(type);
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
    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, txOp);
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
    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, txOp);
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
    revLog.submitRevision(graphName, branchName, txnId, txnSubmitId, txOp);
  }
  
}
