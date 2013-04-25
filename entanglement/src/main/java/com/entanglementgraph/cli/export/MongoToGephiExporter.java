
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
import java.util.*;
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
  private HashSet<String> investigatedEdges;

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
    this.investigatedEdges = new HashSet<>();
  }

  /**
   * This method will create an id appropriate to a gephi node from the Entanglement object,
   * so there is no need to deserialize.
   *
   * @param object the entanglement object to examine
   * @return an appropriate *unique* string for that object
   */
  private static String keysetToId(DBObject object) {

    if (!((BasicDBList) ((BasicDBObject) object.get("keys")).get("uids")).isEmpty()) {
      // just return the first UID.
      return (String) ((BasicDBList) ((BasicDBObject) object.get("keys")).get("uids")).iterator().next();
    }

    if (!((BasicDBList) ((BasicDBObject) object.get("keys")).get("names")).isEmpty() &&
        !((String) ((BasicDBObject) object.get("keys")).get("type")).isEmpty()) {
      return (String) ((BasicDBObject) object.get("keys")).get("type") +
          ((BasicDBList) ((BasicDBObject) object.get("keys")).get("names")).iterator().next();
    }
    throw new IllegalArgumentException("An entity must have at least "
        + "one UID -OR- a suitable type/name combination. Offending entanglement object was: " + object);
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
   * Export a subgraph to a file rather than to a Gephi GraphModel. Will call
   * the GraphModel version of exportSubgraph().
   *
   * @param nodeUid    The id to begin the subgraph with
   * @param stopTypes  a list of node types which will stop the progression of
   *                   the query
   * @param outputFile the file to export the subgraph to
   */
  @SuppressWarnings("UnusedDeclaration")
  public void exportSubgraph(String nodeUid,
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
    GraphModel graphModel = exportSubgraph(nodeUid, stopTypes);
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
   * provided. It will continue exploring all edges irrespective of directionality until reaching a
   * "stop" node of one the types provided.
   *
   * @param nodeUid   The id to begin the subgraph with
   * @param stopTypes a list of node types which will stop the progression of
   *                  the query
   * @return the GraphModel containing the subgraph requested, or null if not found.
   */
  public GraphModel exportSubgraph(String nodeUid,
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
      logger.log(Level.WARNING, "No node with Uid {0} found in database.", nodeUid);
      return null;
    }
    directedGraph.addNode(parseEntanglementNode(coreNode, graphModel, attributeModel));

    // Export subgraph into gephi directed graph
    Node coreAriesNode = marshaller.deserialize(coreNode, Node.class);
    addChildNodes(coreAriesNode.getKeys(), stopTypes, directedGraph, graphModel, attributeModel);

    // Print out a summary of the full graph
    logger.log(Level.INFO, "Complete Nodes: {0} Complete Edges: {1}",
        new Integer[]{directedGraph.getNodeCount(), directedGraph.getEdgeCount()});

    return graphModel;

  }

  /**
   * Export all nodes and edges in the mongodb graph
   *
   * @param outputFile the file to export the graph to
   * @throws IOException
   * @throws GraphModelException
   * @throws RevisionLogException
   */
  public void exportAll(File outputFile)
      throws IOException, GraphModelException, RevisionLogException {

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
      org.gephi.graph.api.Edge gephiEdge = parseEntanglementEdge(edge, graphModel, directedGraph);
      if (gephiEdge != null) {
        directedGraph.addEdge(gephiEdge);
      }
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
                                                        AttributeModel attributeModel) {


    // create the gephi node object after creating a unique identifier using available key attributes.
    org.gephi.graph.api.Node gephiNode = graphModel.factory().newNode(keysetToId(nodeObject));
    logger.log(Level.INFO, "Parsing Entanglement node with a constructed Gephi node id of {0}",
        gephiNode.getNodeData().getId());

    // assign values from the names attribute to the gephi node in the appropriate location.
    String type = "";
    for (String nodeAttrName : nodeObject.keySet()) {
      // Parse the nested values within keys
      switch (nodeAttrName) {
        case NodeDAO.FIELD_KEYS:
          // the value for the node attributes is never written as "keys.names", for example. It is just written
          // as "keys" and then you have to drill down further. Here, the result of a get() call is a DBObject
          // rather than a BasicDBObject, which is what you'd get if there weren't nested attributes (e.g. with _id)
          // we are not storing the UIDs in Gephi right now.
          DBObject nestedObj = (DBObject) nodeObject.get(nodeAttrName);
          for (String keysAttrName : nestedObj.keySet()) {
            if (keysAttrName.equals("names")) { // can't use NodeDAO.FIELD_KEYS_NAME as that includes the string "keys."
              if (nestedObj.get(keysAttrName) instanceof Set) {
                Set names = (Set) nestedObj.get(keysAttrName);
                logger.log(Level.INFO, "Nested value for attribute {0} is {1}", new String[]{keysAttrName, names.toString()});
                gephiNode.getNodeData().setLabel(names.toString()); //TODO ugly name
              }
            } else if (keysAttrName.equals("type")) { // can't use NodeDAO.FIELD_KEYS_TYPE as that includes the string "keys."
              // save the type of the node while we're at it
              type = nestedObj.get(keysAttrName).toString();
              logger.log(Level.INFO, "Nested value for attribute {0} is {1}", new String[]{keysAttrName, type});
              AttributeColumn typeColumn;
              // If not already present, save the node type in the list of attributes for that gephi node.
              if ((typeColumn = attributeModel.getNodeTable().getColumn(keysAttrName)) == null) {
                typeColumn = attributeModel.getNodeTable().addColumn(keysAttrName, AttributeType.STRING);
              }
              gephiNode.getNodeData().getAttributes().setValue(typeColumn.getIndex(), type);
            }
          }
          break;
        case "_id":
          break; // we have already parsed the id value.
        default:
          // Now parse all node attributes which are not FIELD_KEYS, specifically those which are not nested.
          Object attributeValueObj = nodeObject.get(nodeAttrName);
          String attributeValue = "";
          // For two simple types, just convert to String in preparation for loading into the Gephi node.
          if (attributeValueObj instanceof BasicDBList || attributeValueObj instanceof BasicDBObject) {
            attributeValue = attributeValueObj.toString();
            logger.log(Level.INFO, "Converting attribute value object to string: {0}", attributeValue);
          }
          if (attributeValue.isEmpty()) {
            logger.log(Level.INFO, "Skipping node attribute {0} whose value cannot be resolved to a string", nodeAttrName);
            continue;
          }
          logger.log(Level.INFO, "Value for attribute {0} is {1}", new String[]{nodeAttrName, attributeValue});
          AttributeColumn attrColumn;
          if ((attrColumn = attributeModel.getNodeTable().getColumn(nodeAttrName)) == null) {
            attrColumn = attributeModel.getNodeTable().addColumn(nodeAttrName, AttributeType.STRING);
          }
          logger.log(Level.INFO, "attrCol: " + attrColumn.getIndex());
          System.out.println("Gephi node: " + gephiNode);
          System.out.println("Node data: " + gephiNode.getNodeData());
          System.out.println("Attributes: " + gephiNode.getNodeData().
              getAttributes());
          // Now set the attribute's value on this gephi node
          gephiNode.getNodeData().getAttributes().setValue(attrColumn.getIndex(), attributeValue);
          break;
      }
    }

    // Now that all parsing is complete, if you couldn't find a name, fill with the id instead
    if (gephiNode.getNodeData().getLabel() == null) {
      gephiNode.getNodeData().setLabel(((Integer) gephiNode.getId()).toString());
    }

    // retrieve the appropriate color based on provided color mappings.
    Color nodeColour = DEFAULT_COLOR;
//    System.out.println("Type: " + type + ", custom color: " + colorMapping.get(type));
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

    return gephiNode;

  }

  /**
   * Adds "from", "to", and an appropriate label to a new Gephi edge based on the Entanglement edge. Note that
   * currently the entanglement edge UIDs are not saved in Gephi, as Gephi does not have a specific place to
   * store such an ID. If it turns out we wish to store this as a standard edge attribute, this functionality can
   * easily be added.
   *
   * @param edge          the Entanglement edge to process
   * @param graphModel    the graph model to retrieve the Gephi factory from
   * @param directedGraph the DirectedGraph which already contains the nodes on either side of the new Gephi edge
   * @return the new Gephi edge
   * @throws GraphModelException if there is a problem retrieving the from/to nodes from Entanglement
   */
  private org.gephi.graph.api.Edge parseEntanglementEdge(Edge edge,
                                                         GraphModel graphModel, DirectedGraph directedGraph) throws
      GraphModelException {

    BasicDBObject fromObj = nodeDao.getByKey(edge.getFrom());
    BasicDBObject toObj = nodeDao.getByKey(edge.getTo());

    // Entanglement edges are allowed to be hanging. If this happens, do not export the edge to Gephi
    if (fromObj == null || toObj == null) {
      System.out.println("Edge " + edge.getKeys().getUids().iterator().next() + " is hanging and will not be" +
          "propagated to Gephi.");
      return null;
    }

    String fromId = keysetToId(fromObj);
    String toId = keysetToId(toObj);
    org.gephi.graph.api.Edge gephiEdge = graphModel.factory().newEdge(directedGraph.getNode(fromId), directedGraph.
        getNode(toId), 1f, true);
    gephiEdge.getEdgeData().setLabel(edge.getKeys().getType());

    return gephiEdge;

  }

  /**
   * Adds all edges (irrespective of directionality) and their associated nodes connected to the parent keys until
   * there are either no more edges, or until a stop node is reached.
   *
   * @param parentKeys     the EntityKeys which define the node to start at
   * @param stopTypes      the node types which determine where a subgraph should stop
   * @param directedGraph  the Gephi directed graph to add nodes and edges to
   * @param graphModel     the graph model to use when creating new edges and nodes
   * @param attributeModel helps to store the known attribute columns for the current node
   * @throws GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void addChildNodes(EntityKeys parentKeys, Set<String> stopTypes,
                             DirectedGraph directedGraph, GraphModel graphModel,
                             AttributeModel attributeModel)
      throws GraphModelException, DbObjectMarshallerException {

    /*
     * Start with the provided node, and iterate through all
     * edges for that node and down through the nodes attached to those edges until you have to stop.
     */
    logger.log(Level.INFO, "Iterating over outgoing edges of {0}", parentKeys.getUids().iterator().next());
    iterateEdges(edgeDao.iterateEdgesFromNode(parentKeys), true, stopTypes, directedGraph, graphModel, attributeModel);
    logger.log(Level.INFO, "Iterating over incoming edges of {0}", parentKeys.getUids().iterator().next());
    iterateEdges(edgeDao.iterateEdgesToNode(parentKeys), false, stopTypes, directedGraph, graphModel, attributeModel);
  }

  /**
   * Adds all edges associated with the particular Iterable until
   * there are either no more edges, or until a stop node is reached.
   *
   * @param edgeIterator        the iterator which define the set of edges to add
   * @param iterateOverOutgoing if true, the iterator is looking at outgoing edges, and so the opposing node is
   *                            retrieved via getTo(). If false, the iterator is looking at incoming edges, and
   *                            so the opposing node is retrieved via getFrom().
   * @param stopTypes           the node types which determine where a subgraph should stop
   * @param directedGraph       the Gephi directed graph to add nodes and edges to
   * @param graphModel          the graph model to use when creating new edges and nodes
   * @param attributeModel      helps to store the known attribute columns for the current node
   * @throws GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void iterateEdges(Iterable<DBObject> edgeIterator, boolean iterateOverOutgoing, Set<String> stopTypes,
                            DirectedGraph directedGraph, GraphModel graphModel,
                            AttributeModel attributeModel) throws DbObjectMarshallerException, GraphModelException {

    for (DBObject obj : edgeIterator) {
      // deserialize the DBObject to get all Edge properties.
      Edge currentEdge = marshaller.deserialize(obj, Edge.class);
      logger.log(Level.INFO, "Found {0} edge with uid {1}", new String[]{currentEdge.getKeys().getType(),
          currentEdge.getKeys().getUids().toString()});
      // to ensure we don't traverse the same edge twice, check to see if it has already been investigated.
      if (investigatedEdges.containsAll(currentEdge.getKeys().getUids())) {
        logger.log(Level.INFO, "Edge {0} already investigated. Skipping.", currentEdge.getKeys().getUids().toString());
        continue;
      } else {
        investigatedEdges.addAll(currentEdge.getKeys().getUids());
      }

      EntityKeys opposingNodeKeys = currentEdge.getFrom();
      if (iterateOverOutgoing) {
        opposingNodeKeys = currentEdge.getTo();
      }
      logger.log(Level.INFO, "Found opposing node on edge with type {0}", opposingNodeKeys.getType());

      // add the node that the current edge is pointing to
      DBObject currentNodeObject = nodeDao.getByKey(opposingNodeKeys);
      if (currentNodeObject != null) {
        org.gephi.graph.api.Node gNode = parseEntanglementNode(currentNodeObject, graphModel, attributeModel);
        directedGraph.addNode(gNode);
        logger.log(Level.INFO, "Added node to Gephi: {0}", gNode.getNodeData().getId());

        // add the current edge's information. This cannot be added until nodes at both ends have been added.
        org.gephi.graph.api.Edge gephiEdge = parseEntanglementEdge(currentEdge, graphModel, directedGraph);
        if (gephiEdge != null) {
          directedGraph.addEdge(gephiEdge);
          logger.log(Level.INFO, "Added edge to Gephi: {0}", gephiEdge.getEdgeData().getId());
        }

        Node currentNode = marshaller.deserialize(currentNodeObject, Node.class);
        logger.log(Level.INFO, "Edge {0} links to node {1}",
            new String[]{currentEdge.getKeys().getUids().iterator().next().toString(),
                currentNode.getKeys().getUids().iterator().next().toString()});
        /*
         * if the node is a stop type, then don't drill down further
         * into the subgraph. Otherwise, continue until there are no
         * further edges.
         */
        if (stopTypes.contains(currentNode.getKeys().getType())) {
          logger.log(Level.INFO, "Stopping at pre-defined stop node of type {0}", currentNode.getKeys().getType());
          continue;
        }
        logger.log(Level.INFO, "Finding children of node {0}", currentNode.getKeys().getUids().toString());
        addChildNodes(currentNode.getKeys(), stopTypes, directedGraph, graphModel, attributeModel);
      } else {
        logger.log(Level.INFO, "Edge {0} is a hanging edge", currentEdge.getKeys().toString());
      }
    }

  }

}
