
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
package com.entanglementgraph.visualisation.jung;

import com.entanglementgraph.graph.*;
import com.entanglementgraph.graph.mongodb.ObjectMarshallerFactory;
import com.entanglementgraph.cursor.VirtualNodeFactory;
import com.entanglementgraph.util.EntityKeyElementCacheWithLookups;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.graph.mongodb.MongoUtils;
import com.entanglementgraph.util.InMemoryEntityKeyElementCacheWithLookups;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Allyson Lister
 * @author Keith Flanagan
 */
public class MongoToJungGraphExporter {

  private static final Logger logger = Logger.getLogger(MongoToJungGraphExporter.class.getName());

  private static final DbObjectMarshaller marshaller =
      ObjectMarshallerFactory.create(MongoToJungGraphExporter.class.getClassLoader());

  private Graph<Node, Edge> graph;

//  // Keys to DBObject cache - keep track of these so that we can add edges to nodes we've already added.
//  // Map of UID -> DBObject node objects
//  private final Map<String, DBObject> uidToNode;
//  // Map of type -> name -> DBObject node objects
//  private final Map<String, Map<String, DBObject>> typeToNameToNode;

  private EntityKeyElementCacheWithLookups<Content, Node<? extends Content>> seenNodes = new InMemoryEntityKeyElementCacheWithLookups();


  public MongoToJungGraphExporter() {
    clearGraph();

  }

  public MongoToJungGraphExporter(Graph<Node, Edge> existing) {
    this.graph = existing;

  }

  public void clearGraph() {
    graph = new DirectedSparseGraph<>();

    // Clear caches of 'seen' nodes.
    seenNodes = new InMemoryEntityKeyElementCacheWithLookups();
  }


  public void writeToGraphMLFile(File outputFile) throws IOException {
    logger.info("Writing to file: " + outputFile.getAbsolutePath());
    FileWriter fw = new FileWriter(outputFile);
    GraphMLWriter writer = new GraphMLWriter();
    writer.save(graph, fw);
    fw.flush();
    fw.close();
    logger.info("Written: " + outputFile.getAbsolutePath());
  }


//  public void writeToPngFile(File outputFile) throws IOException {
//    logger.info("Writing to file: " + outputFile.getAbsolutePath());
//    //Dimension loDims = getGraphLayout().getSize();
//    Dimension vsDims = getSize();
//
//    int width = vsDims.width;
//    int height = vsDims.height;
//    Color bg = Color.BLACK;
//
//    BufferedImage im = new BufferedImage(width,height, BufferedImage.TYPE_INT_BGR);
//    Graphics2D graphics = im.createGraphics();
//    graphics.setColor(bg);
//    graphics.fillRect(0, 0, width, height);
//
//    paintComponent(graphics);
//
//    try{
//      ImageIO.write(im,"png",new File(outputFile));
//    }catch(Exception e){
//      e.printStackTrace();
//    }
//  }

  public void writeToSvgFile(File outputFile) throws IOException {
    logger.info("Writing to file: " + outputFile.getAbsolutePath());

    Layout<Node, Edge> layout = new FRLayout<>(graph);
    layout.setSize(new Dimension(800, 800));
//    Layout<Integer, Number> layout = new FRLayout2<Integer,Number>(graph);
    VisualizationViewer<Node, Edge> vv =  new VisualizationViewer<>(layout);

    Properties p = new Properties();
    p.setProperty("PageSize","A5");
    VectorGraphics g = new SVGGraphics2D(outputFile, new Dimension(400,300));
    g.setProperties(p);
    g.startExport();
    vv.print(g);
    g.endExport();
    logger.info("Written: " + outputFile.getAbsolutePath());
  }


  /**
   * Build a subgraph (as a Gephi object) associated with the node which has a Uid matching that
   * provided. It will continue exploring all edges irrespective of directionality until reaching a
   * "stop" node of one the types provided.
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

    // Start with the core node
    Node coreNode = graphConn.getNodeDao().getByKey(entityKey);
    if (coreNode == null) {
      logger.log(Level.WARNING, "No node with EntityKey {0} found in database.", entityKey);
      return false;
    }
    addNode(coreNode);

    // Store the IDs of edges that we've already seen as we step through the graph.
    // investigatedEdges needs to be created here, as addChildNodes is called many times in the subgraph
    // algorithm and we can't have investigatedEdges being cleared partway through subgraph creation.
    Set<String> investigatedEdges = new HashSet<>();

    // Export subgraph into the current Jung graph model
    addChildNodes(graphConn, investigatedEdges, coreNode.getKeys(), stopNodeTypes, stopEdgeTypes);

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
   * @return true if a node with the given EntityKey was found, false otherwise.
   */
  @SuppressWarnings("UnusedDeclaration")
  public boolean addDepthBasedSubgraph(GraphConnection graphConn,
                                       EntityKeys entityKey, int traversalDepth,
                                       ArrayList<Set<String>> stopNodeTypes, ArrayList<Set<String>> stopEdgeTypes)
      throws GraphModelException, DbObjectMarshallerException {

    // Start with the core node
    Node coreNode = graphConn.getNodeDao().getByKey(entityKey);
    if (coreNode == null) {
      logger.log(Level.WARNING, "No node with EntityKey {0} found in database.", entityKey);
      return false;
    }
    addNode(coreNode);

    // Store the IDs of edges that we've already seen as we step through the graph.
    // investigatedEdges needs to be created here, as addChildNodes is called many times in the subgraph
    // algorithm and we can't have investigatedEdges being cleared partway through subgraph creation.
    Set<String> investigatedEdges = new HashSet<>();

    // Export subgraph into the current Jung graph model
    addChildNodesAtDepth(graphConn, investigatedEdges, coreNode.getKeys(), 0, traversalDepth,
        stopNodeTypes, stopEdgeTypes);

    return true;

  }

  /**
   * Adds a new node to a Jung graph. If at least one of the items in the <code>nodeObj</code> keyset matches
   * an entry in the node cache, the node will node be added again.
   *
   * @param nodeObj the DBObject (Entanglement) node to add to the Jung graph
   * @return true if <code>nodeObj</code> was added, otherwise false.
   * @throws com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException
   */
  private boolean addNode(Node nodeObj) throws DbObjectMarshallerException {
    EntityKeys keyset = nodeObj.getKeys();
    Node existingNode = seenNodes.getUserObjectFor(keyset);
    if (existingNode != null) {
      return false;
    }

    graph.addVertex(nodeObj);
    seenNodes.cacheElementsAndAssociateWithObject(keyset, nodeObj);
    return true;
  }


  private void addEdge(Edge edge) throws DbObjectMarshallerException {
    if (edge == null) {
      throw new DbObjectMarshallerException("The specified edge DBObject was null!");
    }
    Node fromNode = seenNodes.getUserObjectFor(edge.getFrom());
    Node toNode = seenNodes.getUserObjectFor(edge.getTo());

    // Deal with hanging edges. We haven't come across these yet, because previously we only iterated of the set of existing nodes.
    if (fromNode == null) {
//      fromNodeObj = VirtualNodeFactory.createVirtualNodeForLocation(marshaller, edge.getFrom());
      fromNode = new Node(edge.getFrom());
      fromNode.setVirtual(true);
      seenNodes.cacheElementsAndAssociateWithObject(edge.getFrom(), fromNode);
    }
    if (toNode == null) {
//      toNodeObj = VirtualNodeFactory.createVirtualNodeForLocation(marshaller, edge.getTo());
      toNode = new Node(edge.getTo());
      toNode.setVirtual(true);
      seenNodes.cacheElementsAndAssociateWithObject(edge.getTo(), toNode);
    }
    graph.addEdge(edge, fromNode, toNode, EdgeType.DIRECTED);
  }


  /**
   * Adds all the nodes and eddges in the specified Entanglement graph to the in-memory Gephi workspace.
   *
   * @param graphConn the Entanglement graph to be added
   * @throws java.io.IOException
   * @throws com.entanglementgraph.graph.GraphModelException
   *
   * @throws com.entanglementgraph.graph.RevisionLogException
   *
   */
  public void addEntireGraph(GraphConnection graphConn)
      throws GraphModelException, DbObjectMarshallerException {

    for (Node node : graphConn.getNodeDao().iterateAll()) {
      addNode(node);
    }

    for (Edge edgeObj : graphConn.getEdgeDao().iterateAll()) {
      addEdge(edgeObj);
    }
  }


  /**
   * Adds all child nodes and the edges between them (irrespective of directionality) until
   * there are either no more edges, or until a stop node is reached.
   *
   * @param parentKeys    the EntityKeys which define the node to start at (the parent node)
   * @param stopNodeTypes the node types which determine where a subgraph should stop
   * @param stopEdgeTypes the edge types which determine where a subgraph should stop
   * @throws com.entanglementgraph.graph.GraphModelException
   *          if there is a problem retrieving part of an entanglement graph
   * @throws com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException
   *          if there is a problem deserializing an entanglement database object
   */
  private void addChildNodes(GraphConnection graphConn, Set<String> investigatedEdges, EntityKeys parentKeys,
                             Set<String> stopNodeTypes, Set<String> stopEdgeTypes)
      throws GraphModelException, DbObjectMarshallerException {
    EdgeDAO edgeDao = graphConn.getEdgeDao();
    /*
     * Start with the provided node, and iterate through all
     * edges for that node and down through the nodes attached to those edges until you have to stop.
     */
//    logger.log(Level.FINE, "Iterating over outgoing edges of {0}", keysetToId(parentKeys));
    iterateEdges(graphConn, investigatedEdges, edgeDao.iterateEdgesFromNode(parentKeys), true, stopNodeTypes, stopEdgeTypes);
//    logger.log(Level.FINE, "Iterating over incoming edges of {0}", keysetToId(parentKeys));
    iterateEdges(graphConn, investigatedEdges, edgeDao.iterateEdgesToNode(parentKeys), false, stopNodeTypes, stopEdgeTypes);
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
   * @throws com.entanglementgraph.graph.GraphModelException
   *          if there is a problem retrieving part of an entanglement graph
   * @throws com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException
   *          if there is a problem deserializing an entanglement database object
   */
  private void addChildNodesAtDepth(GraphConnection graphConn, Set<String> investigatedEdges, EntityKeys parentKeys,
                                    int currentDepth, int traversalDepth, ArrayList<Set<String>> stopNodeTypes,
                                    ArrayList<Set<String>> stopEdgeTypes)
      throws GraphModelException, DbObjectMarshallerException {
    EdgeDAO edgeDao = graphConn.getEdgeDao();
    /*
     * Start with the provided node, and iterate through all
     * edges for that node and down through the nodes attached to those edges until you have to stop.
     */
//    logger.log(Level.FINE, "Iterating over outgoing edges of {0}", keysetToId(parentKeys));
    iterateEdgesAtDepth(graphConn, investigatedEdges, edgeDao.iterateEdgesFromNode(parentKeys), true, currentDepth,
        traversalDepth, stopNodeTypes, stopEdgeTypes);
//    logger.log(Level.FINE, "Iterating over incoming edges of {0}", keysetToId(parentKeys));
    iterateEdgesAtDepth(graphConn, investigatedEdges, edgeDao.iterateEdgesToNode(parentKeys), false, currentDepth,
        traversalDepth, stopNodeTypes, stopEdgeTypes);
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
   * @throws com.entanglementgraph.graph.GraphModelException
   *          if there is a problem retrieving part of an entanglement graph
   * @throws com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException
   *          if there is a problem deserializing an entanglement database object
   */
  private void iterateEdges(GraphConnection graphConn, Set<String> investigatedEdges,
                            Iterable<Edge> edgeIterator, boolean iterateOverOutgoing, Set<String> stopNodeTypes,
                            Set<String> stopEdgeTypes)
      throws DbObjectMarshallerException, GraphModelException {

    for (Edge currentEdge : edgeIterator) {
      // deserialize the DBObject to get all Edge properties.
//      Edge currentEdge = marshaller.deserialize(edgeObj, Edge.class);
      logger.log(Level.FINE, "Found {0} edge with uid {1}",
          new String[]{currentEdge.getKeys().getType(), currentEdge.getKeys().toString()});
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
      Node currentNode = graphConn.getNodeDao().getByKey(opposingNodeKeys);

      //Add a new Jung node or retrieve reference to existing node (if we've seen this node before)
      if (currentNode != null) {
        //EntityKeys<?> currentNodeKeyset = MongoUtils.parseKeyset(marshaller, currentNodeObject, GraphEntityDAO.FIELD_KEYS);
        EntityKeys currentNodeKeyset = currentNode.getKeys();
        if (seenNodes.seenElementOf(currentNodeKeyset)) {
          // this node may have been added previously. If it has been, then we know that we may actually be in
          // the middle of investigating it, some recursion levels upwards. Therefore if we hit a known node,
          // don't add the current edge, and move on to the next one without investigating further children.
          logger.log(Level.FINE,
              "Jung node {0} already present. Skipping entire edge and node addition.",
              currentNodeKeyset.toString());
          continue;
        }
        logger.log(Level.FINE, "Added node to Jung: {0}", currentNodeKeyset.toString());


        // add the current edge's information. This cannot be added until nodes at both ends have been added.
        addEdge(currentEdge);
        logger.log(Level.FINE, "Added edge to Jung: {0}", currentEdge.getKeys().toString());

        logger.log(Level.FINE, "Edge {0} links to node {1}", new String[]{currentEdge.getKeys().toString(),
            currentNode.getKeys().toString()});
        /*
         * if the node is a stop type, then don't drill down further
         * into the subgraph. Otherwise, continue until there are no
         * further edges.
         */
        if (stopNodeTypes.contains(currentNode.getKeys().getType())) {
          logger.log(Level.FINE, "Stopping at pre-defined stop node of type {0}", currentNode.getKeys().getType());
          continue;
        }
        logger.log(Level.FINE, "Finding children of node {0}", currentNode.getKeys().toString());
        addChildNodes(graphConn, investigatedEdges, currentNode.getKeys(), stopNodeTypes, stopEdgeTypes);
      } else {
        logger.log(Level.FINE, "Edge {0} is a hanging edge", currentEdge.getKeys().toString());
      }
    }

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
   * @throws com.entanglementgraph.graph.GraphModelException
   *          if there is a problem retrieving part of an entanglement graph
   * @throws com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException
   *          if there is a problem deserializing an entanglement database object
   */
  private void iterateEdgesAtDepth(GraphConnection graphConn, Set<String> investigatedEdges,
                                   Iterable<Edge> edgeIterator, boolean iterateOverOutgoing, int currentDepth,
                                   int traversalDepth, ArrayList<Set<String>> stopNodeTypes,
                                   ArrayList<Set<String>> stopEdgeTypes)
      throws DbObjectMarshallerException, GraphModelException {

    logger.fine("At traversal depth " + currentDepth + 1 + "/" + traversalDepth);

    for (Edge currentEdge : edgeIterator) {
      // deserialize the DBObject to get all Edge properties.
//      Edge currentEdge = marshaller.deserialize(edgeObj, Edge.class);
      logger.log(Level.FINE, "Found {0} edge with uid {1}",
          new String[]{currentEdge.getKeys().getType(), currentEdge.getKeys().toString()});
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
//      DBObject currentNodeObject = graphConn.getNodeDao().getByKey(opposingNodeKeys);
      Node currentNode = graphConn.getNodeDao().getByKey(opposingNodeKeys);

      //Add a new Jung node or retrieve reference to existing node (if we've seen this node before)
      if (currentNode != null) {
//        EntityKeys currentNodeKeyset = MongoUtils.parseKeyset(marshaller, currentNodeObject, GraphEntityDAO.FIELD_KEYS);
        EntityKeys currentNodeKeyset = currentNode.getKeys();
        if (seenNodes.seenElementOf(currentNodeKeyset)) {
          // this node may have been added previously. If it has been, then we know that we may actually be in
          // the middle of investigating it, some recursion levels upwards. Therefore if we hit a known node,
          // don't add the current edge, and move on to the next one without investigating further children.
          logger.log(Level.FINE,
              "Jung node {0} already present. Skipping entire edge and node addition.",
              currentNodeKeyset.toString());
          continue;
        }
        boolean added = addNode(currentNode);
        logger.log(Level.FINE, "Added node to Jung: {0}: {1}", new Object[]{currentNodeKeyset.toString(), added});
//        org.gephi.graph.api.Node gNode = parseEntanglementNode(currentNodeObject, attributeModel);


        // add the current edge's information. This cannot be added until nodes at both ends have been added.
        addEdge(currentEdge);
        logger.log(Level.FINE, "Added edge to Jung: {0}", currentEdge.getKeys().toString());


//        Node currentNode = marshaller.deserialize(currentNodeObject, Node.class);
        logger.log(Level.FINE, "Edge {0} links to node {1}", new String[]{currentEdge.getKeys().toString(),
            currentNode.getKeys().toString()});
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
          logger.log(Level.FINE, "Finding children of node {0}", currentNode.getKeys().toString());
          addChildNodesAtDepth(graphConn, investigatedEdges, currentNode.getKeys(), currentDepth + 1, traversalDepth,
              stopNodeTypes, stopEdgeTypes);
        } else {
          logger.fine("Stopping traversal of graph at traversal depth of " + currentDepth + 1 + "/" + traversalDepth);
          //noinspection UnnecessaryContinue
          continue;
        }
      } else {
        logger.log(Level.FINE, "Edge {0} is a hanging edge", currentEdge.getKeys().toString());
      }
    }

  }

  public Graph<Node, Edge> getGraph() {
    return graph;
  }
}
