
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
package com.entanglementgraph.export.gephi;

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
import org.gephi.data.attributes.AttributeControllerImpl;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.dynamic.DynamicControllerImpl;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.dhns.DhnsGraphController;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.impl.ExportControllerImpl;
import org.gephi.project.api.Project;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Allyson Lister
 */
public class MongoToGephiExporter {

  private static final Logger logger = Logger.getLogger(MongoToGephiExporter.class.getName());
  // a Gephi label string will use the value below to separate alternative labels.
  private static final String LABEL_SPLIT_REGEX = "\" , \"";
  // A Gephi label string will start and end with the following values
  public static final String LABEL_LIST_START = "[ \"";
  public static final String LABEL_LIST_END = "\"]";


  private static final Color DEFAULT_COLOR = Color.BLACK;
  private static final DbObjectMarshaller marshaller =
      ObjectMarshallerFactory.create(MongoToGephiExporter.class.getClassLoader());

  private final Map<String, Color> colorMapping;


  private ProjectController projectController;
  //  private Project project;
  private Workspace workspace;

  private GraphModel graphModel;
  private DirectedGraph directedGraph;


  public MongoToGephiExporter() {
    colorMapping = new HashMap<>();

//    projectController = new ProjectControllerImpl();
    projectController = Lookup.getDefault().lookup(ProjectController.class);
    synchronized (projectController) {
//      projectController.newProject();
      Project project = projectController.getCurrentProject();
      if (project == null) {
        logger.info("Creating a new Gephi project");
        projectController.newProject();
        project = projectController.getCurrentProject();
        logger.info("Project: " + project);
      }
//      workspace = projectController.getCurrentWorkspace();
      workspace = projectController.newWorkspace(project);
    }

    GraphController gc = new DhnsGraphController();
    this.graphModel = gc.getModel(workspace);

    // Create a directed graph based on this graph model
    this.directedGraph = graphModel.getDirectedGraph();
  }

  /**
   * Call this when you're done with exporting the graph to ensure that all Gephi objects are tidied up.
   */
  public void close() {
//    projectController.closeCurrentWorkspace();
//    projectController.closeCurrentProject();
    projectController.deleteWorkspace(workspace);
  }

  public void clearWorkspace() {
    directedGraph.clear();
    projectController.cleanWorkspace(workspace);
  }

  public void writeToFile(File outputFile) throws IOException {
    // This line is a hack to get around a weird NullPointerException
    // which crops up when exporting to GEXF. See url below for details:
    // https://forum.gephi.org/viewtopic.php?f=27&t=2337
    //noinspection UnusedDeclaration
//    DynamicModel dynamicModel = Lookup.getDefault().lookup(
//        DynamicController.class).getModel();
    DynamicController dc = new DynamicControllerImpl();
    DynamicModel dynamicModel = dc.getModel(workspace);

    // Export workspace in GEXF format
    ExportController ec = new ExportControllerImpl();
    ec.exportFile(outputFile, workspace);
    logger.log(Level.INFO, "Output file: {0}", outputFile.getAbsoluteFile());
  }

  /**
   * This method will create an id appropriate to a gephi node from the Entanglement object,
   * so there is no need to deserialize.
   *
   * @param object the entanglement object to examine
   * @return an appropriate *unique* string for that object
   */
  public static String keysetToId(DBObject object) {

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

  /**
   * This method will create an id appropriate to a gephi node from the EntityKeys object.
   * Only use this method if you have already deserialized the Entanglement DBObject, otherwise use the
   * other form of this method to save deserialization.
   *
   * @param keys the EntityKeys object to examine
   * @return an appropriate *unique* string for that object
   */
  public static String keysetToId(EntityKeys keys) {

    if (!keys.getUids().isEmpty()) {
      // just return the first UID.
      return (String) keys.getUids().iterator().next();
    }

    if (!keys.getNames().isEmpty() && !keys.getType().isEmpty()) {
      return keys.getType() + keys.getNames();
    }
    throw new IllegalArgumentException("An entity must have at least "
        + "one UID -OR- a suitable type/name combination. Offending entanglement object was: " + keys);
  }

//  @SuppressWarnings("UnusedDeclaration")
//  public DirectedGraph getDirectedGraph() {
//    return directedGraph;
//  }


  /**
   * Build a subgraph (as a Gephi object) associated with the node which has a Uid matching that
   * provided. It will continue exploring all edges irrespective of directionality until reaching a
   * "stop" node or edge of one the types provided.
   * <p/>
   * The subgraph is stored in the class variable directedGraph.
   *
   * @param entityKey     The entity key set to begin the subgraph with
   * @param stopNodeTypes a list of node types which will stop the progression of the query
   * @param stopEdgeTypes a list of edge types which will stop the progression of the query
   * @return true if a node with the given EntityKey was found, false otherwise.
   */
  public boolean addSubgraph(GraphConnection graphConn,
                             EntityKeys entityKey, Set<String> stopNodeTypes, Set<String> stopEdgeTypes)
      throws GraphModelException, DbObjectMarshallerException {

    // Add column for node type
//    AttributeController ac = Lookup.getDefault().
//        lookup(AttributeController.class);
//    AttributeModel attributeModel = ac.getModel();
    AttributeController ac = new AttributeControllerImpl();
    AttributeModel attributeModel = ac.getModel(workspace);
    // ensure the current graph is empty
    //directedGraph.clear();

    // Start with the core node
    BasicDBObject coreNode = graphConn.getNodeDao().getByKey(entityKey);
    if (coreNode == null) {
      logger.log(Level.WARNING, "No node with EntityKey {0} found in database.", entityKey);
      return false;
    }
    directedGraph.addNode(parseEntanglementNode(coreNode, attributeModel));

    // Store the IDs of edges that we've already seen as we step through the graph.
    // investigatedEdges needs to be created here, as addChildNodes is called many times in the subgraph
    // algorithm and we can't have investigatedEdges being cleared partway through subgraph creation.
    Set<String> investigatedEdges = new HashSet<>();

    // Export subgraph into gephi directed graph
    Node coreAriesNode = marshaller.deserialize(coreNode, Node.class);
    addChildNodes(graphConn, investigatedEdges, coreAriesNode.getKeys(), stopNodeTypes, stopEdgeTypes, attributeModel);

    // Print out a summary of the full graph
    logger.log(Level.INFO, "Complete Nodes: {0} Complete Edges: {1}",
        new Integer[]{directedGraph.getNodeCount(), directedGraph.getEdgeCount()});

    return true;

  }

  /**
   * Build a subgraph (as a Gephi object) associated with the node which has a Uid matching that
   * provided. It will continue exploring all edges irrespective of directionality only to the depth
   * provided in traversalDepth until reaching a "stop" node or edge of one the types provided. Specific stop
   * nodes and edges can be provided for each level of the traversal depth using the stopEdgeTypes and stopNodeTypes
   * parameters.
   * <p/>
   * The subgraph is stored in the class variable directedGraph.
   *
   * @param entityKey     The entity key set to begin the subgraph with
   * @param stopNodeTypes a list of node types which will stop the progression of the query
   * @param stopEdgeTypes a list of edge types which will stop the progression of the query
   * @return true if a node with the given EntityKey was found, false otherwise (e.g. if query node not found, or
   *         if the traversalDepth does not match the size of the stop type arrays.
   */
  public boolean addDepthBasedSubgraph(GraphConnection graphConn,
                                       EntityKeys entityKey, int traversalDepth,
                                       ArrayList<Set<String>> stopNodeTypes, ArrayList<Set<String>> stopEdgeTypes)
      throws GraphModelException, DbObjectMarshallerException {

    if (stopNodeTypes.size() != traversalDepth || stopEdgeTypes.size() != traversalDepth) {
      logger.warning("traversal depth of " + traversalDepth + " does not match the stop edge (" + stopEdgeTypes.size() +
          ") or node (" + stopNodeTypes.size() + ") list size");
      return false;
    }

    AttributeController ac = new AttributeControllerImpl();
    AttributeModel attributeModel = ac.getModel(workspace);

    // Start with the core node
    BasicDBObject coreNode = graphConn.getNodeDao().getByKey(entityKey);
    if (coreNode == null) {
      logger.log(Level.WARNING, "No node with EntityKey {0} found in database.", entityKey);
      return false;
    }
    directedGraph.addNode(parseEntanglementNode(coreNode, attributeModel));

    // Store the IDs of edges that we've already seen as we step through the graph.
    // investigatedEdges needs to be created here, as addChildNodes is called many times in the subgraph
    // algorithm and we can't have investigatedEdges being cleared partway through subgraph creation.
    Set<String> investigatedEdges = new HashSet<>();

    // Export subgraph into gephi directed graph
    Node coreAriesNode = marshaller.deserialize(coreNode, Node.class);
    addChildNodesAtDepth(graphConn, investigatedEdges, coreAriesNode.getKeys(), 0, traversalDepth,
        stopNodeTypes, stopEdgeTypes, attributeModel);

    // Print out a summary of the full graph
    logger.log(Level.INFO, "Complete Nodes: {0} Complete Edges: {1}",
        new Integer[]{directedGraph.getNodeCount(), directedGraph.getEdgeCount()});

    return true;

  }

  /**
   * Adds all the nodes and eddges in the specified Entanglement graph to the in-memory Gephi workspace.
   *
   * @param graphConn the Entanglement graph to be added
   * @throws IOException
   * @throws GraphModelException
   * @throws RevisionLogException
   */
  public void addEntireGraph(GraphConnection graphConn)
      throws IOException, GraphModelException, RevisionLogException {
    AttributeController ac = new AttributeControllerImpl();
    AttributeModel attributeModel = ac.getModel(workspace);

    // Create Gephi nodes
    for (DBObject node : graphConn.getNodeDao().iterateAll()) {
      directedGraph.addNode(parseEntanglementNode(node, attributeModel));
    }

    // Create Gephi edges; currently with a standard weight of 1
    // and no set color
    Iterable<Edge> edgeItr = new DeserialisingIterable<>(graphConn.getEdgeDao().iterateAll(), marshaller, Edge.class);
    for (Edge edge : edgeItr) {
      org.gephi.graph.api.Edge gephiEdge = parseEntanglementEdge(graphConn, edge);
      if (gephiEdge != null) {
        directedGraph.addEdge(gephiEdge);
      }
    }

    // Print out a summary of the full graph
    logger.log(Level.INFO, "Complete Nodes: {0} Complete Edges: {1}",
        new Integer[]{directedGraph.getNodeCount(), directedGraph.getEdgeCount()});
  }

  /**
   * Given a DBObject (which is expected to be a node), parse all attributes of the object and store within a
   * Gephi node object.
   *
   * @param nodeObject     the Entanglement object to parse
   * @param attributeModel the Gephi AttributeModel to use when adding new column types to Gephi
   * @return the populated Gephi node
   */
  private org.gephi.graph.api.Node parseEntanglementNode(DBObject nodeObject,
                                                         AttributeModel attributeModel) {
    // create the gephi node object after creating a unique identifier using available key attributes.
    org.gephi.graph.api.Node gephiNode = graphModel.factory().newNode(keysetToId(nodeObject));
    logger.log(Level.FINE, "Parsing Entanglement node with a constructed Gephi node id of {0}",
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
              if (nestedObj.get(keysAttrName) instanceof BasicDBList) {
                BasicDBList list = (BasicDBList) nestedObj.get(keysAttrName);
                logger.log(Level.FINE, "Nested value for attribute {0} is {1}",
                    new String[]{keysAttrName, list.toString()});
                if (list.size() == 1) {
                  gephiNode.getNodeData().setLabel((String) list.iterator().next());
                } else {
                  gephiNode.getNodeData().setLabel(list.toString());
                }
              } else {
                logger.log(Level.WARNING,
                    "Nested value for attribute {0} is not a BasicDBList as expected. Not parsing.", keysAttrName);
              }
            } else if (keysAttrName.equals("type")) { // can't use NodeDAO.FIELD_KEYS_TYPE as that includes the string "keys."
              // save the type of the node while we're at it
              type = nestedObj.get(keysAttrName).toString();
              logger.log(Level.FINE, "Nested value for attribute {0} is {1}", new String[]{keysAttrName, type});
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
          String attributeValue = attributeValueObj.toString();
          // Just convert to String in preparation for loading into the Gephi node.
          logger.log(Level.FINE, "Converting attribute value object to string: {0}", attributeValue);
          if (attributeValue.isEmpty()) {
            logger.log(Level.FINE, "Skipping node attribute {0} whose value cannot be resolved to a string", nodeAttrName);
            continue;
          }
          logger.log(Level.FINE, "Value for attribute {0} is {1}", new String[]{nodeAttrName, attributeValue});
          AttributeColumn attrColumn;
          if ((attrColumn = attributeModel.getNodeTable().getColumn(nodeAttrName)) == null) {
            attrColumn = attributeModel.getNodeTable().addColumn(nodeAttrName, AttributeType.STRING);
          }
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
   * @param edge the Entanglement edge to process
   * @return the new Gephi edge
   * @throws GraphModelException if there is a problem retrieving the from/to nodes from Entanglement
   */
  private org.gephi.graph.api.Edge parseEntanglementEdge(GraphConnection graphConn, Edge edge) throws
      GraphModelException {

    NodeDAO nodeDao = graphConn.getNodeDao();
    BasicDBObject fromObj = nodeDao.getByKey(edge.getFrom());
    BasicDBObject toObj = nodeDao.getByKey(edge.getTo());

    // Entanglement edges are allowed to be hanging. If this happens, do not export the edge to Gephi
    if (fromObj == null || toObj == null) {
      logger.log(Level.FINE, "Edge {0} is hanging and will not be propagated to Gephi.", keysetToId(edge.getKeys()));
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
   * Adds all child nodes and the edges between them (irrespective of directionality) until
   * there are either no more edges, or until a stop node is reached.
   *
   * @param graphConn         the connection to the database
   * @param investigatedEdges the edges which have already been checked in the subgraph
   * @param parentKeys        the EntityKeys which define the node to start at (the parent node)
   * @param stopNodeTypes     the node types which determine where a subgraph should stop
   * @param stopEdgeTypes     the edge types which determine where a subgraph should stop
   * @param attributeModel    helps to store the known attribute columns for the current node
   * @throws GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void addChildNodes(GraphConnection graphConn, Set<String> investigatedEdges, EntityKeys parentKeys,
                             Set<String> stopNodeTypes, Set<String> stopEdgeTypes, AttributeModel attributeModel)
      throws GraphModelException, DbObjectMarshallerException {
    EdgeDAO edgeDao = graphConn.getEdgeDao();
    /*
     * Start with the provided node, and iterate through all
     * edges for that node and down through the nodes attached to those edges until you have to stop.
     */
    logger.log(Level.FINE, "Iterating over outgoing edges of {0}", keysetToId(parentKeys));
    iterateEdges(graphConn, investigatedEdges, edgeDao.iterateEdgesFromNode(parentKeys), true, stopNodeTypes, stopEdgeTypes, attributeModel);
    logger.log(Level.FINE, "Iterating over incoming edges of {0}", keysetToId(parentKeys));
    iterateEdges(graphConn, investigatedEdges, edgeDao.iterateEdgesToNode(parentKeys), false, stopNodeTypes, stopEdgeTypes, attributeModel);
  }

  /**
   * Adds all child nodes and the edges between them (irrespective of directionality) until
   * there are either no more edges, or until a stop node is reached. Current depth is monitored, and the subgraph
   * will stop when the appropriate depth is reached.
   *
   * @param graphConn         the connection to the database
   * @param investigatedEdges the edges which have already been checked in the subgraph
   * @param parentKeys        the EntityKeys which define the node to start at (the parent node)
   * @param currentDepth      the current depth we are investigating for the subgraph
   * @param traversalDepth    the maximum depth we are allowing for the subgraph
   * @param stopNodeTypes     the node types which determine where a subgraph should stop at each traversal depth
   * @param stopEdgeTypes     the edge types which determine where a subgraph should stop at each traversal depth
   * @param attributeModel    helps to store the known attribute columns for the current node
   * @throws GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void addChildNodesAtDepth(GraphConnection graphConn, Set<String> investigatedEdges, EntityKeys parentKeys,
                                    int currentDepth, int traversalDepth, ArrayList<Set<String>> stopNodeTypes,
                                    ArrayList<Set<String>> stopEdgeTypes, AttributeModel attributeModel)
      throws GraphModelException, DbObjectMarshallerException {
    EdgeDAO edgeDao = graphConn.getEdgeDao();
    /*
     * Start with the provided node, and iterate through all
     * edges for that node and down through the nodes attached to those edges until you have to stop.
     */
    logger.log(Level.FINE, "Iterating over outgoing edges of {0}", keysetToId(parentKeys));
    iterateEdgesAtDepth(graphConn, investigatedEdges, edgeDao.iterateEdgesFromNode(parentKeys), true, currentDepth,
        traversalDepth, stopNodeTypes, stopEdgeTypes, attributeModel);
    logger.log(Level.FINE, "Iterating over incoming edges of {0}", keysetToId(parentKeys));
    iterateEdgesAtDepth(graphConn, investigatedEdges, edgeDao.iterateEdgesToNode(parentKeys), false, currentDepth,
        traversalDepth, stopNodeTypes, stopEdgeTypes, attributeModel);
  }

  /**
   * Adds all edges associated with the particular Iterable until there are either no more edges, or until a stop
   * node is reached.
   *
   * @param edgeIterator        the iterator which define the set of edges to add
   * @param iterateOverOutgoing if true, the iterator is looking at outgoing edges, and so the opposing node is
   *                            retrieved via getTo(). If false, the iterator is looking at incoming edges, and
   *                            so the opposing node is retrieved via getFrom().
   * @param stopNodeTypes       the node types which determine where a subgraph should stop
   * @param stopEdgeTypes       the edge types which determine where a subgraph should stop
   * @param attributeModel      helps to store the known attribute columns for the current node
   * @throws GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void iterateEdges(GraphConnection graphConn, Set<String> investigatedEdges,
                            Iterable<DBObject> edgeIterator, boolean iterateOverOutgoing, Set<String> stopNodeTypes,
                            Set<String> stopEdgeTypes, AttributeModel attributeModel) throws DbObjectMarshallerException, GraphModelException {

    for (DBObject obj : edgeIterator) {
      // deserialize the DBObject to get all Edge properties.
      Edge currentEdge = marshaller.deserialize(obj, Edge.class);
      logger.log(Level.FINE, "Found {0} edge with uid {1}", new String[]{currentEdge.getKeys().getType(),
          keysetToId(currentEdge.getKeys())});
      // ignore any edges whose type matches those found within the edge stop types.
      if (stopEdgeTypes.contains(currentEdge.getKeys().getType())) {
        logger.log(Level.FINE, "Stopping at pre-defined stop edge of type {0}", currentEdge.getKeys().getType());
        continue;
      }
      // to ensure we don't traverse the same edge twice, check to see if it has already been investigated.
      // we want to do this before we start parsing entanglement nodes and edges, which is why
      // directedGraph.contains(Edge) isn't being used.
      if (investigatedEdges.containsAll(currentEdge.getKeys().getUids())) {
        logger.log(Level.FINE, "Edge {0} already investigated. Skipping.", currentEdge.getKeys().getUids().toString());
        continue;
      } else {
        //noinspection unchecked
        investigatedEdges.addAll(currentEdge.getKeys().getUids());
      }

      EntityKeys opposingNodeKeys = currentEdge.getFrom();
      if (iterateOverOutgoing) {
        opposingNodeKeys = currentEdge.getTo();
      }
      logger.log(Level.FINE, "Found opposing node on edge with type {0}", opposingNodeKeys.getType());

      // add the node that the current edge is pointing to, if it hasn't already been added.
      DBObject currentNodeObject = graphConn.getNodeDao().getByKey(opposingNodeKeys);
      if (currentNodeObject != null) {
        org.gephi.graph.api.Node gNode = parseEntanglementNode(currentNodeObject, attributeModel);

        // this node may have been added previously. If it has been, then we know that we may actually be in
        // the middle of investigating it, some recursion levels upwards. Therefore if we hit a known node,
        // don't add the current edge, and move on to the next one without investigating further children.
        if (directedGraph.getNode(gNode.getNodeData().getId()) == null) {
          directedGraph.addNode(gNode);
          logger.log(Level.FINE, "Added node to Gephi: {0}", gNode.getNodeData().getId());
        } else {
          logger.log(Level.FINE, "Gephi node {0} already present. Skipping entire edge and node addition.",
              gNode.getNodeData().getId());
          continue;
        }

        // add the current edge's information. This cannot be added until nodes at both ends have been added.
        org.gephi.graph.api.Edge gephiEdge = parseEntanglementEdge(graphConn, currentEdge);
        if (gephiEdge != null) {
          directedGraph.addEdge(gephiEdge);
          logger.log(Level.FINE, "Added edge to Gephi: {0}", gephiEdge.getEdgeData().getId());
        }

        Node currentNode = marshaller.deserialize(currentNodeObject, Node.class);
        logger.log(Level.FINE, "Edge {0} links to node {1}", new String[]{keysetToId(currentEdge.getKeys()),
            keysetToId(currentNode.getKeys())});
        /*
         * if the node is a stop type, then don't drill down further
         * into the subgraph. Otherwise, continue until there are no
         * further edges.
         */
        if (stopNodeTypes.contains(currentNode.getKeys().getType())) {
          logger.log(Level.FINE, "Stopping at pre-defined stop node of type {0}", currentNode.getKeys().getType());
          continue;
        }
        logger.log(Level.FINE, "Finding children of node {0}", keysetToId(currentNode.getKeys()));
        addChildNodes(graphConn, investigatedEdges, currentNode.getKeys(), stopNodeTypes, stopEdgeTypes, attributeModel);
      } else {
        logger.log(Level.FINE, "Edge {0} is a hanging edge", keysetToId(currentEdge.getKeys()));
      }
    }
    logger.fine("Counter: nodes = " + directedGraph.getNodeCount() + " edges = " + directedGraph.getEdgeCount());
  }

  /**
   * Adds all edges associated with the particular Iterable until there are either no more edges, or until a stop
   * node is reached.
   *
   * @param edgeIterator        the iterator which define the set of edges to add
   * @param iterateOverOutgoing if true, the iterator is looking at outgoing edges, and so the opposing node is
   *                            retrieved via getTo(). If false, the iterator is looking at incoming edges, and
   *                            so the opposing node is retrieved via getFrom().
   * @param currentDepth        the current depth we are investigating for the subgraph
   * @param traversalDepth      the maximum depth we are allowing for the subgraph
   * @param stopNodeTypes       the node types which determine where a subgraph should stop at each traversal depth
   * @param stopEdgeTypes       the edge types which determine where a subgraph should stop at each traversal depth
   * @param attributeModel      helps to store the known attribute columns for the current node
   * @throws GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void iterateEdgesAtDepth(GraphConnection graphConn, Set<String> investigatedEdges,
                                   Iterable<DBObject> edgeIterator, boolean iterateOverOutgoing,
                                   int currentDepth, int traversalDepth, ArrayList<Set<String>> stopNodeTypes,
                                   ArrayList<Set<String>> stopEdgeTypes, AttributeModel attributeModel)
      throws DbObjectMarshallerException, GraphModelException {

    logger.fine("At traversal depth " + currentDepth + 1 + "/" + traversalDepth);

    for (DBObject obj : edgeIterator) {
      // deserialize the DBObject to get all Edge properties.
      Edge currentEdge = marshaller.deserialize(obj, Edge.class);
      logger.log(Level.FINE, "Found {0} edge with uid {1}", new String[]{currentEdge.getKeys().getType(),
          keysetToId(currentEdge.getKeys())});
      // ignore any edges whose type matches those found within the edge stop types.
      if (stopEdgeTypes.get(currentDepth).contains(currentEdge.getKeys().getType())) {
        logger.fine("Stopping at pre-defined stop edge of type " + currentEdge.getKeys().getType() + " at traversal " +
            "depth of " + currentDepth + 1 + "/" + traversalDepth);
        continue;
      }
      // to ensure we don't traverse the same edge twice, check to see if it has already been investigated.
      // we want to do this before we start parsing entanglement nodes and edges, which is why
      // directedGraph.contains(Edge) isn't being used.
      if (investigatedEdges.containsAll(currentEdge.getKeys().getUids())) {
        logger.log(Level.FINE, "Edge {0} already investigated. Skipping.", currentEdge.getKeys().getUids().toString());
        continue;
      } else {
        //noinspection unchecked
        investigatedEdges.addAll(currentEdge.getKeys().getUids());
      }

      EntityKeys opposingNodeKeys = currentEdge.getFrom();
      if (iterateOverOutgoing) {
        opposingNodeKeys = currentEdge.getTo();
      }
      logger.log(Level.FINE, "Found opposing node on edge with type {0}", opposingNodeKeys.getType());

      // add the node that the current edge is pointing to, if it hasn't already been added.
      DBObject currentNodeObject = graphConn.getNodeDao().getByKey(opposingNodeKeys);
      if (currentNodeObject != null) {
        org.gephi.graph.api.Node gNode = parseEntanglementNode(currentNodeObject, attributeModel);

        // this node may have been added previously. If it has been, then we know that we may actually be in
        // the middle of investigating it, some recursion levels upwards. Therefore if we hit a known node,
        // don't add the current edge, and move on to the next one without investigating further children.
        if (directedGraph.getNode(gNode.getNodeData().getId()) == null) {
          directedGraph.addNode(gNode);
          logger.log(Level.FINE, "Added node to Gephi: {0}", gNode.getNodeData().getId());
        } else {
          logger.log(Level.FINE, "Gephi node {0} already present. Skipping entire edge and node addition.",
              gNode.getNodeData().getId());
          continue;
        }

        // add the current edge's information. This cannot be added until nodes at both ends have been added.
        org.gephi.graph.api.Edge gephiEdge = parseEntanglementEdge(graphConn, currentEdge);
        if (gephiEdge != null) {
          directedGraph.addEdge(gephiEdge);
          logger.log(Level.FINE, "Added edge to Gephi: {0}", gephiEdge.getEdgeData().getId());
        }

        Node currentNode = marshaller.deserialize(currentNodeObject, Node.class);
        logger.log(Level.FINE, "Edge {0} links to node {1}", new String[]{keysetToId(currentEdge.getKeys()),
            keysetToId(currentNode.getKeys())});
        /*
         * if the node is a stop type, then don't drill down further
         * into the subgraph. Otherwise, continue until there are no
         * further edges.
         */
        if (stopNodeTypes.get(currentDepth).contains(currentNode.getKeys().getType())) {
          logger.fine("Stopping at pre-defined stop node of type " + currentNode.getKeys().getType() + " at traversal " +
              "depth of " + currentDepth + 1 + "/" + traversalDepth);
          continue;
        }

        // only add the next level of child nodes if its depth value is less than the total traversal depth
        if (currentDepth + 1 < traversalDepth) {
          logger.log(Level.FINE, "Finding children of node {0}", keysetToId(currentNode.getKeys()));
          addChildNodesAtDepth(graphConn, investigatedEdges, currentNode.getKeys(), currentDepth + 1, traversalDepth,
              stopNodeTypes, stopEdgeTypes, attributeModel);
        } else {
          logger.fine("Stopping traversal of graph at traversal depth of " + currentDepth + 1 + "/" + traversalDepth +
              " for node " + keysetToId(currentNode.getKeys()));
          continue;
        }
      } else {
        logger.log(Level.FINE, "Edge {0} is a hanging edge", keysetToId(currentEdge.getKeys()));
      }
    }
    logger.fine("Counter: nodes = " + directedGraph.getNodeCount() + " edges = " + directedGraph.getEdgeCount());
  }

  /**
   * This will reformat the default value stored within a Gephi node label to be a String[].
   * Assumes that the label being passed is one that has been auto-generated by this class via parseEntanglementNode.
   * String[] aren't used by default for the Gephi export, as Gephi expects only a String as the label value.
   *
   * @param label the string to parse
   * @return a cleaned String[] which stores all labels in their own position in the array
   */
  @SuppressWarnings("UnusedDeclaration")
  public static String[] getLabelArray(String label) {
    // if there is only one label, just add it and return immediately
    if (!label.contains(LABEL_SPLIT_REGEX)) {
      return new String[]{label};
    }

    // cut off leading and trailing chars.
    label = label.substring(LABEL_LIST_START.length());
    label = label.substring(0, label.length() - LABEL_LIST_END.length());

    // split the label
    return label.split(LABEL_SPLIT_REGEX);
  }


  public void addColourMapping(String entityType, Color color) {
    colorMapping.put(entityType, color);
  }

  public void addColourMappings(Map<String, Color> mappings) {
    colorMapping.putAll(mappings);
  }

  public void clearColourMappings() {
    colorMapping.clear();
  }

  public Map<String, Color> getColorMapping() {
    return colorMapping;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public ProjectController getProjectController() {
    return projectController;
  }
}
