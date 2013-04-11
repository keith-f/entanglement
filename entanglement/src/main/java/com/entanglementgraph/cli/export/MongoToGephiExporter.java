
/*
 *
 * Copyright 2013 Allyson Lister and employers
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
package com.entanglementgraph.cli.export;

import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Allyson Lister
 */
public class MongoToGephiExporter {

  private static final Logger logger = Logger.
      getLogger(MongoGraphToGephi.class.getName());
  private static final Color DEFAULT_COLOR = Color.BLACK;
  private static final DbObjectMarshaller marshaller =
      ObjectMarshallerFactory.create(MongoToGephiExporter.class.getClassLoader());
  private final NodeDAO nodeDao;
  private final EdgeDAO edgeDao;
  private Map<String, Color> colorMapping;

  /**
   * @param conn           set up node and edge DAOs
   * @param colorPropsFile the optional color mapping for node types
   */
  public MongoToGephiExporter(GraphConnection conn,
                              File colorPropsFile) throws IOException {
    if (colorPropsFile != null) {
      this.colorMapping.putAll(loadColorMappings(colorPropsFile));
    } else {
      this.colorMapping = new HashMap<>();

    }
    this.nodeDao = conn.getNodeDao();
    this.edgeDao = conn.getEdgeDao();
  }

  private static String keysetToId(EntityKeys keyset) {
    if (keyset.getUids().isEmpty()) {
      throw new IllegalArgumentException("An entity must have at least "
          + "one UID. Offending keyset was: " + keyset);
    }
    Set<String> uids = keyset.getUids();
    return uids.iterator().next();
  }

  private static Map<String, Color> loadColorMappings(File propFile)
      throws IOException {
    Properties props;
    try (FileInputStream is = new FileInputStream(propFile)) {
      props = new Properties();
      props.load(is);
    }

    Map<String, Color> nodeTypeToColour = new HashMap<>();
    for (String nodeType : props.stringPropertyNames()) {
      String colorString = props.getProperty(nodeType);
      Color color;

      switch (colorString) {
        case "BLACK":
          color = Color.BLACK;
          break;
        case "BLUE":
          color = Color.BLUE;
          break;
        case "CYAN":
          color = Color.CYAN;
          break;
        case "DARK_GRAY":
          color = Color.DARK_GRAY;
          break;
        case "GRAY":
          color = Color.GRAY;
          break;
        case "GREEN":
          color = Color.GREEN;
          break;
        case "LIGHT_GRAY":
          color = Color.LIGHT_GRAY;
          break;
        case "MAGENTA":
          color = Color.MAGENTA;
          break;
        case "ORANGE":
          color = Color.ORANGE;
          break;
        case "PINK":
          color = Color.PINK;
          break;
        case "RED":
          color = Color.RED;
          break;
        case "WHITE":
          color = Color.WHITE;
          break;
        case "YELLOW":
          color = Color.YELLOW;
          break;
        default:
          color = DEFAULT_COLOR;
      }


      nodeTypeToColour.put(nodeType, color);
    }
    return nodeTypeToColour;
  }

  /**
   * Export a subgraph associated with the node which has a Uid matching that
   * provided. It will continue exploring all outgoing edges ONLY until reaching a
   * "stop" node of one the types provided.
   *
   * @param nodeUid    The id to begin the subgraph with
   * @param stopTypes  a list of node types which will stop the progression of
   *                   the query
   * @param outputFile the file to export the subgraph to
   */
  public void exportOutgoingSubgraph(String nodeUid,
                                     Set<String> stopTypes,
                                     File outputFile) throws GraphModelException,
      DbObjectMarshallerException, IOException {

    // We need to have our own gephi workspace prepared in this method in order to have something to store the
    // incoming GraphModel.

    // Init a gephi project - and therefore a workspace
    ProjectController pc = Lookup.getDefault().lookup(
        ProjectController.class);
    pc.newProject();

    // Gephi tutorials suggest having the line below, even if not used...
    Workspace workspace = pc.getCurrentWorkspace();
    GraphModel graphModel = exportOutgoingSubgraph(nodeUid, stopTypes);
    if (graphModel == null) {
      return;
    }
    workspace.add(graphModel);

    // This line is a hack to get around a weird NullPointerException
    // which crops up when exporting to GEXF. See url below for details:
    // https://forum.gephi.org/viewtopic.php?f=27&t=2337
    //noinspection UnusedDeclaration
    DynamicModel dynamicModel = Lookup.getDefault().lookup(
        DynamicController.class).getModel();

    // Export full graph in GEXF format
    ExportController ec = Lookup.getDefault().
        lookup(ExportController.class);
    System.out.println(outputFile.getAbsoluteFile());
    ec.exportFile(outputFile);

  }

  /**
   * Export a subgraph (as a Gephi object) associated with the node which has a Uid matching that
   * provided. It will continue exploring all outgoing edges ONLY until reaching a
   * "stop" node of one the types provided.
   *
   * @param nodeUid   The id to begin the subgraph with
   * @param stopTypes a list of node types which will stop the progression of
   *                  the query
   * @return the GraphModel containing the subgraph requested, or null if not found.
   */
  public GraphModel exportOutgoingSubgraph(String nodeUid,
                                           Set<String> stopTypes) throws GraphModelException,
      DbObjectMarshallerException {

    // Init a gephi project - and therefore a workspace
    ProjectController pc = Lookup.getDefault().lookup(
        ProjectController.class);
    pc.newProject();
    // Gephi tutorials suggest having the line below, even if not used...
    //noinspection UnusedDeclaration
    Workspace workspace = pc.getCurrentWorkspace();

    // Get a graph model - it exists because we have a workspace
    GraphModel graphModel = Lookup.getDefault().lookup(
        GraphController.class).getModel();

    // Create a directed graph based on this graph model
    DirectedGraph directedGraph = graphModel.getDirectedGraph();
    // Add column for node type
    AttributeController ac = Lookup.getDefault().
        lookup(AttributeController.class);
    AttributeModel attributeModel = ac.getModel();

    // Start with the core node
    BasicDBObject coreNode = nodeDao.getByUid(nodeUid);
    if (coreNode == null) {
      System.err.println("No node with Uid " + nodeUid + " found in database.");
      return null;
    }
    directedGraph.addNode(parseEntanglementNode(coreNode, graphModel, attributeModel));

    // Export subgraph into gephi directed graph
    Node coreAriesNode = marshaller.deserialize(coreNode, Node.class);
    addChildNodes(coreAriesNode.getKeys(), stopTypes, directedGraph, graphModel, attributeModel);

    // Print out a summary of the full graph
    System.out.println("Complete Nodes: " + directedGraph.getNodeCount()
        + " Complete Edges: " + directedGraph.getEdgeCount());

    return graphModel;

  }

  /**
   * Export all nodes and edges in the mongodb graph
   *
   * @param outputFile the file to export the graph to
   * @throws IOException
   * @throws GraphModelException
   * @throws RevisionLogException
   * @throws DbObjectMarshallerException
   */
  public void exportAll(File outputFile)
      throws IOException, GraphModelException, RevisionLogException,
      DbObjectMarshallerException {

    // Init a gephi project - and therefore a workspace
    ProjectController pc = Lookup.getDefault().lookup(
        ProjectController.class);
    pc.newProject();
    // Gephi tutorials suggest having the line below, even if not used...
    //noinspection UnusedDeclaration
    Workspace workspace = pc.getCurrentWorkspace();

    // Get a graph model - it exists because we have a workspace
    GraphModel graphModel = Lookup.getDefault().lookup(
        GraphController.class).getModel();

    // Create a directed graph based on this graph model
    DirectedGraph directedGraph = graphModel.getDirectedGraph();
    // Add column for node type
    AttributeController ac = Lookup.getDefault().
        lookup(AttributeController.class);
    AttributeModel attributeModel = ac.getModel();


//        AttributeColumn nodeTypeCol = attributeModel.getNodeTable().addColumn(
//                "nodeType",
//                AttributeType.STRING );


    // Create Gephi nodes
    for (DBObject node : nodeDao.iterateAll()) {
      directedGraph.addNode(parseEntanglementNode(node, graphModel, attributeModel));
    }

    // Create Gephi edges; currently with a standard weight of 1
    // and no set color
    Iterable<Edge> edgeItr = new DeserialisingIterable<>(
        edgeDao.iterateAll(), marshaller, Edge.class);
    for (Edge edge : edgeItr) {
      directedGraph.addEdge(parseEntanglementEdge(edge, graphModel,
          directedGraph));
    }

    // Print out a summary of the full graph
    System.out.println("Complete Nodes: " + directedGraph.getNodeCount()
        + " Complete Edges: " + directedGraph.getEdgeCount());

    // This line is a hack to get around a weird NullPointerException
    // which crops up when exporting to gexf. See url below for details:
    // https://forum.gephi.org/viewtopic.php?f=27&t=2337
    //noinspection UnusedDeclaration
    DynamicModel dynamicModel = Lookup.getDefault().lookup(
        DynamicController.class).getModel();

    // Export full graph in GEXF format
    ExportController ec = Lookup.getDefault().
        lookup(ExportController.class);
    System.out.println(outputFile.getAbsoluteFile());
    ec.exportFile(outputFile);

  }

  public org.gephi.graph.api.Node parseEntanglementNode(DBObject nodeObject,
                                                        GraphModel graphModel,
                                                        AttributeModel attributeModel)
      throws DbObjectMarshallerException {


    org.gephi.graph.api.Node gephiNode = null;

    // create the gephi node object after finding a value within the _id field.
    for (String nodeAttrName : nodeObject.keySet()) {
      if (nodeAttrName.equals("_id")) {
        String id = (String) nodeObject.get(nodeAttrName);
        gephiNode = graphModel.factory().newNode(id);
        break;
      }
    }
    if (gephiNode == null) {
      System.err.println("No identifier found for node when converting to Gephi project file.");
      return gephiNode;
    }

    // assign values from the names attribute to the gephi node in the appropriate location (the label).
    for (String nodeAttrName : nodeObject.keySet()) {
      if (nodeAttrName.equals("names")) {  // might be keys.names
        if (nodeObject.get(nodeAttrName) instanceof Set) {
          Set names = (Set) nodeObject.get(nodeAttrName);
          gephiNode.getNodeData().setLabel(names.toString()); //TODO ugly name
        }
      }
    }

    // if you couldn't find a name, fill with the id instead
    if (gephiNode.getNodeData().getLabel().isEmpty()) {
      gephiNode.getNodeData().setLabel(((Integer) gephiNode.getId()).toString());
    }

    // retrieve the appropriate color based on provided color mappings.
    String type = (String) nodeObject.get(NodeDAO.FIELD_KEYS_TYPE);
    Color nodeColour = DEFAULT_COLOR;
    System.out.println("Type: " + type + ", custom color: " + colorMapping.get(type));
    if (colorMapping.containsKey(type)) {
      nodeColour = colorMapping.get(type);
    }

    // set the color for the node
    float[] rgbColorComp = nodeColour.getRGBColorComponents(null);
    gephiNode.getNodeData().setColor(rgbColorComp[0], rgbColorComp[1],
        rgbColorComp[2]);
//      gephiNode.getNodeData().setColor(nodeColour.getRed(), nodeColour.
//              getGreen(), nodeColour.getBlue());
//            gephiNode.getNodeData().getAttributes().setValue( nodeTypeCol.getIndex(), node.getType() );


    // now we move on to all other attributes present in the node.
    Map<String, AttributeColumn> nodeAttrNameToAttributeCol =
        new HashMap<>();

    for (String nodeAttrName : nodeObject.keySet()) {

      Object val = nodeObject.get(nodeAttrName);
      if (nodeAttrName.equals("_id") || nodeAttrName.equals("names")) { // again, might be keys.names
        continue; // ignore as have already set thess values.
      }
      if (val instanceof BasicDBList) {
        val = val.toString();
      } else if (val instanceof BasicDBObject) {
        logger.
            info(
                "Replacing value of type BasicDBObject with String.");
        val = val.toString();
      }
      if (val == null) {
        logger.log(Level.INFO,
            "Skipping node attribute with null value: {0}",
            nodeAttrName);
        continue;
      }
      AttributeColumn attrCol = nodeAttrNameToAttributeCol.get(
          nodeAttrName);
      if (attrCol == null) {
        attrCol = attributeModel.getNodeTable().addColumn(
            nodeAttrName, AttributeType.parse(val));
        nodeAttrNameToAttributeCol.put(nodeAttrName, attrCol);
      }
//        logger.info("nodeAttrName: " + nodeAttrName + ", val: " + val + ", type: " + val.getClass().getName());
//        logger.info("attrCol: " + attrCol);
      System.out.println("Gephi node: " + gephiNode);
      System.out.println("Node data: " + gephiNode.getNodeData());
      System.out.println("Attributes: " + gephiNode.getNodeData().
          getAttributes());
      System.out.println("Attributes col: " + attrCol.getIndex());
      System.out.println("Node attr name: " + nodeAttrName);
      System.out.println("Val: " + val);
      System.out.println("Val type: " + val.getClass().getName());
      gephiNode.getNodeData().getAttributes().setValue(attrCol.getIndex(), val);
    }

    return gephiNode;

  }

  private org.gephi.graph.api.Edge parseEntanglementEdge(Edge edge,
                                                         GraphModel graphModel, DirectedGraph directedGraph) throws
      GraphModelException, DbObjectMarshallerException {
    BasicDBObject fromObj = nodeDao.getByKey(edge.getFrom());
    String fromId = keysetToId(MongoUtils.parseKeyset(marshaller,
        fromObj));

    BasicDBObject toObj = nodeDao.getByKey(edge.getTo());
    String toId = keysetToId(MongoUtils.
        parseKeyset(marshaller, toObj));


    org.gephi.graph.api.Edge gephiEdge = graphModel.factory().
        newEdge(directedGraph.getNode(fromId), directedGraph.
            getNode(toId), 1f, true);
    gephiEdge.getEdgeData().setLabel(edge.getKeys().getType());

    return gephiEdge;

  }

  private void addChildNodes(EntityKeys entityKeys, Set<String> stopTypes,
                             DirectedGraph directedGraph, GraphModel graphModel,
                             AttributeModel attributeModel)
      throws GraphModelException, DbObjectMarshallerException {

    /*
     * Start with the provided node, and iterate through all outgoing
     * edges for that node.
     */
    for (DBObject obj : edgeDao.iterateEdgesFromNode(entityKeys)) {
      // deserialize the DBObject to get all Edge properties.
      Edge currentEdge = marshaller.deserialize(obj, Edge.class);
      // add the current edge's information
      directedGraph.addEdge(parseEntanglementEdge(currentEdge,
          graphModel,
          directedGraph));

      // add the node that the current edge is pointing to
      if (EntityKeys.containsAtLeastOneUid(currentEdge.getTo())) {
        Set<String> uids = currentEdge.getTo().getUids();
        String currentUid = uids.iterator().next();
        DBObject currentNodeObject = nodeDao.getByUid(currentUid);
        org.gephi.graph.api.Node gNode = parseEntanglementNode(currentNodeObject, graphModel, attributeModel);
        directedGraph.addNode(gNode);
                /*
                 * if the node is a stop type, then don't drill down further
                 * into the subgraph. Otherwise, continue until there are no
                 * further outgoing edges.
                 */
        if (stopTypes.contains(currentNodeObject.get(NodeDAO.FIELD_KEYS_TYPE))) {
          return;
        }
        Node currentNode = marshaller.deserialize(currentNodeObject, Node.class);
        addChildNodes(currentNode.getKeys(), stopTypes, directedGraph, graphModel, attributeModel);
      }

    }

  }
}
