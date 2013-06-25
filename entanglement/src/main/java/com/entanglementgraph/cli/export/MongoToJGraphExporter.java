
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
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxGdCodec;
import com.mxgraph.util.*;
import com.mxgraph.view.mxGraph;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Allyson Lister
 * @author Keith Flanagan
 */
public class MongoToJGraphExporter {

  private static final Logger logger = Logger.getLogger(MongoToJGraphExporter.class.getName());
  // a Gephi label string will use the value below to separate alternative labels.
  private static final String LABEL_SPLIT_REGEX = "\" , \"";
  // A Gephi label string will start and end with the following values
  public static final String LABEL_LIST_START = "[ \"";
  public static final String LABEL_LIST_END = "\"]";


  private static final Color DEFAULT_COLOR = Color.BLACK;
  private static final DbObjectMarshaller marshaller =
      ObjectMarshallerFactory.create(MongoToJGraphExporter.class.getClassLoader());

  private final Map<String, Color> colorMapping;



  private mxGraph graph;
  private Object parentContainer;

  // Keys to graph object cache - keep track of these so that we can add edges to nodes we've already added.
  // Map of UID -> JGraphX node objects
  private final Map<String, Object> uidToNode;
  // Map of type -> name -> JGraphX node objects
  private final Map<String, Map<String, Object>> typeToNameToNode;


  public MongoToJGraphExporter() {
    colorMapping = new HashMap<>();
    uidToNode = new HashMap<>();
    typeToNameToNode = new HashMap<>();
    clearGraph();
  }

  public void clearGraph() {
    graph = new mxGraph();
    parentContainer = graph.getDefaultParent();

    uidToNode.clear();
    typeToNameToNode.clear();
  }


  public void writeToJGraphXmlFile(File outputFile) throws IOException {
    logger.info("Writing to file: "+outputFile.getAbsolutePath());
    mxCodec codec = new mxCodec();
    String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));

    mxUtils.writeFile(xml, outputFile.getAbsolutePath());
  }

  public void writeToHtmlFile(File outputFile) throws IOException {
    logger.info("Writing to file: "+outputFile.getAbsolutePath());
    mxUtils.writeFile(mxXmlUtils.getXml(mxCellRenderer
        .createHtmlDocument(graph, null, 1, null, null)
        .getDocumentElement()), outputFile.getAbsolutePath());
  }

  public void writeToTextFile(File outputFile) throws IOException {
    logger.info("Writing to file: "+outputFile.getAbsolutePath());
    String content = mxGdCodec.encode(graph);
    mxUtils.writeFile(content, outputFile.getAbsolutePath());
  }

  public void writeToSvgFile(File outputFile) throws IOException {
    logger.info("Writing to file: "+outputFile.getAbsolutePath());
    mxSvgCanvas canvas = (mxSvgCanvas) mxCellRenderer
        .drawCells(graph, null, 1, null,
            new mxCellRenderer.CanvasFactory()
            {
              public mxICanvas createCanvas(int width, int height)
              {
                mxSvgCanvas canvas = new mxSvgCanvas(mxDomUtils.createSvgDocument(width, height));
                canvas.setEmbedded(true);
                return canvas;
              }
            });

    mxUtils.writeFile(mxXmlUtils.getXml(canvas.getDocument()), outputFile.getAbsolutePath());
  }

//  public void writeToPngFile(File outputFile) throws IOException {
//
//  }

  public void writeToPdfFile(File outputFile) throws IOException {
    logger.info("Writing to file: "+outputFile.getAbsolutePath());
    FileOutputStream fos = new FileOutputStream(outputFile);
    try {
      mxRectangle bounds = graph.getGraphBounds();
      Rectangle rectangle = new Rectangle((float) bounds.getWidth(), (float) bounds.getHeight());
      Document document = new Document(rectangle);
      PdfWriter writer = PdfWriter.getInstance(document, fos);
      document.open();
      PdfCanvasFactory pdfCanvasFactory = new PdfCanvasFactory(writer.getDirectContent());
      mxGraphics2DCanvas canvas = (mxGraphics2DCanvas) mxCellRenderer
          .drawCells(graph, null, 1, null, pdfCanvasFactory);
      canvas.getGraphics().dispose();
      document.close();
    }
    catch (DocumentException e) {
      throw new IOException(e.getLocalizedMessage());
    }
    finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   *
   * @param keyset
   * @param jgraphNode
   */
  private void cacheJGraphXNode(EntityKeys<?> keyset, Object jgraphNode) {
    for (String uid : keyset.getUids()) {
      uidToNode.put(uid, jgraphNode);
    }

    Map<String, Object> nameToJGraphNode = typeToNameToNode.get(keyset.getType());
    if (nameToJGraphNode == null) {
      nameToJGraphNode = new HashMap<>();
      typeToNameToNode.put(keyset.getType(), nameToJGraphNode);
    }
    for (String name : keyset.getNames()) {
      nameToJGraphNode.put(name, jgraphNode);
    }
  }

  /**
   * Lookup a cached node based on a keyset. This is useful when adding edges to a graph.
   * @param keyset
   * @return
   */
  private Object getJGraphNodeFromCache(EntityKeys<?> keyset) {

    for (String uid : keyset.getUids()) {
      Object jgraphNode = uidToNode.get(uid);
      if (jgraphNode != null) {
        return jgraphNode;
      }
    }

    Map<String, Object> nameToJGraphNode = typeToNameToNode.get(keyset.getType());
    if (nameToJGraphNode == null) {
      //No nodes of this type at all, so don't even need to check the name
      return null;
    }
    for (String name : keyset.getNames()) {
      Object jgraphNode = nameToJGraphNode.get(name);
      if (jgraphNode != null) {
        return jgraphNode;
      }
    }

    return null;
  }

  private boolean nodeExistsInCache(EntityKeys<?> keyset) {
    return getJGraphNodeFromCache(keyset) != null;
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
    BasicDBObject coreNode = graphConn.getNodeDao().getByKey(entityKey);
    if (coreNode == null) {
      logger.log(Level.WARNING, "No node with EntityKey {0} found in database.", entityKey);
      return false;
    }
    graph.getModel().beginUpdate();
//    directedGraph.addNode(parseEntanglementNode(coreNode, attributeModel));
    addNode(coreNode);

    // Store the IDs of edges that we've already seen as we step through the graph.
    // investigatedEdges needs to be created here, as addChildNodes is called many times in the subgraph
    // algorithm and we can't have investigatedEdges being cleared partway through subgraph creation.
    Set<String> investigatedEdges = new HashSet<>();

    // Export subgraph into the current JGraphX graph model
    Node coreAriesNode = marshaller.deserialize(coreNode, Node.class);
    addChildNodes(graphConn, investigatedEdges, coreAriesNode.getKeys(), stopNodeTypes, stopEdgeTypes);

    // Print out a summary of the full graph
//    logger.log(Level.INFO, "Complete Nodes: {0} Complete Edges: {1}",
//        new Integer[]{directedGraph.getNodeCount(), directedGraph.getEdgeCount()});
    graph.getModel().endUpdate();
    return true;

  }

  /**
   * Adds a new node to a JGraphX graph, or, if at least one of the items in the <code>nodeObj</code> keyset matches
   * an entry in the node cache, returns the existing object instead.
   *
   * @param nodeObj
   * @return
   * @throws DbObjectMarshallerException
   */
  private Object addNode(DBObject nodeObj) throws DbObjectMarshallerException {
//    logger.info("Adding node: "+nodeObj);
    EntityKeys<?> keyset = marshaller.deserialize((DBObject) nodeObj.get(NodeDAO.FIELD_KEYS), EntityKeys.class);
    Object existingNode = getJGraphNodeFromCache(keyset);
    if (existingNode != null) {
      return existingNode;
    }

    String id = parseIdStringFromKeyset(keyset);
    Object jgraphNode = graph.insertVertex(parentContainer, id, nodeObj, 0, 0, 20, 10);
    cacheJGraphXNode(keyset, jgraphNode);
    return jgraphNode;
  }

  private Object addEdge(DBObject edgeObj) throws DbObjectMarshallerException {
//    logger.info("Adding edge: "+edgeObj);
    Edge edge = marshaller.deserialize(edgeObj, Edge.class);
    Object jgraphFromNode = getJGraphNodeFromCache(edge.getFrom());
    Object jgraphToNode = getJGraphNodeFromCache(edge.getTo());

    String id = parseIdStringFromKeyset(edge.getKeys());
    Object graphEdge = graph.insertEdge(parentContainer, id, edge, jgraphFromNode, jgraphToNode);
    return graphEdge;
  }

  /**
   * Adds all the nodes and eddges in the specified Entanglement graph to the in-memory Gephi workspace.
   *
   * @param graphConn the Entanglement graph to be added
   * @throws java.io.IOException
   * @throws com.entanglementgraph.graph.GraphModelException
   * @throws com.entanglementgraph.revlog.RevisionLogException
   */
  public void addEntireGraph(GraphConnection graphConn)
      throws IOException, GraphModelException, RevisionLogException, DbObjectMarshallerException {
    graph.getModel().beginUpdate();
    for (DBObject node : graphConn.getNodeDao().iterateAll()) {
      addNode(node);
    }

//    Iterable<Edge> edgeItr = new DeserialisingIterable<>(graphConn.getEdgeDao().iterateAll(), marshaller, Edge.class);
//    for (Edge edge : edgeItr) {
    for (DBObject edgeObj : graphConn.getEdgeDao().iterateAll()) {
      addEdge(edgeObj);
    }
    graph.getModel().endUpdate();
  }


  /**
   * Given a EntityKeys, create a suitable ID string for use with JGraphX entities.
   */
  private String parseIdStringFromKeyset(EntityKeys<?> keyset) throws DbObjectMarshallerException {
    StringBuilder sb = new StringBuilder();


    if (keyset.getType() != null) {
      sb.append(String.format("[%s]: ", keyset.getType()));
    } else {
      sb.append("[Unknown]: ");
    }

    for (String name : keyset.getNames()) {
      sb.append(name).append(", ");
    }

    for (String id : keyset.getUids()) {
      sb.append(id).append(", ");
    }

    return sb.toString();

  }

  /**
   * Adds "from", "to", and an appropriate label to a new Gephi edge based on the Entanglement edge. Note that
   * currently the entanglement edge UIDs are not saved in Gephi, as Gephi does not have a specific place to
   * store such an ID. If it turns out we wish to store this as a standard edge attribute, this functionality can
   * easily be added.
   *
   * @param edge the Entanglement edge to process
   * @return the new Gephi edge
   * @throws com.entanglementgraph.graph.GraphModelException if there is a problem retrieving the from/to nodes from Entanglement
   */
//  private org.gephi.graph.api.Edge parseEntanglementEdge(GraphConnection graphConn, Edge edge) throws
//      GraphModelException {
//
//    NodeDAO nodeDao = graphConn.getNodeDao();
//    BasicDBObject fromObj = nodeDao.getByKey(edge.getFrom());
//    BasicDBObject toObj = nodeDao.getByKey(edge.getTo());
//
//    // Entanglement edges are allowed to be hanging. If this happens, do not export the edge to Gephi
//    if (fromObj == null || toObj == null) {
//      logger.log(Level.FINE, "Edge {0} is hanging and will not be propagated to Gephi.", keysetToId(edge.getKeys()));
//      return null;
//    }
//
//    String fromId = keysetToId(fromObj);
//    String toId = keysetToId(toObj);
//    org.gephi.graph.api.Edge gephiEdge = graphModel.factory().newEdge(directedGraph.getNode(fromId), directedGraph.
//        getNode(toId), 1f, true);
//    gephiEdge.getEdgeData().setLabel(edge.getKeys().getType());
//
//    addEdge()
//    return gephiEdge;
//
//  }

  /**
   * Adds all child nodes and the edges between them (irrespective of directionality) until
   * there are either no more edges, or until a stop node is reached.
   *
   * @param parentKeys     the EntityKeys which define the node to start at (the parent node)
   * @param stopNodeTypes  the node types which determine where a subgraph should stop
   * @param stopEdgeTypes  the edge types which determine where a subgraph should stop
   * @throws com.entanglementgraph.graph.GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException if there is a problem deserializing an entanglement database object
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
   * Adds all edges associated with the particular Iterable until there are either no more edges, or until a stop
   * node is reached.
   *
   * @param edgeIterator        the iterator which define the set of edges to add
   * @param iterateOverOutgoing if true, the iterator is looking at outgoing edges, and so the opposing node is
   *                            retrieved via getTo(). If false, the iterator is looking at incoming edges, and
   *                            so the opposing node is retrieved via getFrom().
   * @param stopNodeTypes       the node types which determine where a subgraph should stop
   * @param stopEdgeTypes       the edge types which determine where a subgraph should stop
   * @throws com.entanglementgraph.graph.GraphModelException         if there is a problem retrieving part of an entanglement graph
   * @throws com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException if there is a problem deserializing an entanglement database object
   */
  private void iterateEdges(GraphConnection graphConn, Set<String> investigatedEdges,
                            Iterable<DBObject> edgeIterator, boolean iterateOverOutgoing, Set<String> stopNodeTypes,
                            Set<String> stopEdgeTypes)
      throws DbObjectMarshallerException, GraphModelException {

    for (DBObject edgeObj : edgeIterator) {
      // deserialize the DBObject to get all Edge properties.
      Edge currentEdge = marshaller.deserialize(edgeObj, Edge.class);
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
      DBObject currentNodeObject = graphConn.getNodeDao().getByKey(opposingNodeKeys);

      //Add a new JGraphX node or retrieve reference to existing node (if we've seen this node before)
      if (currentNodeObject != null) {
        EntityKeys<?> currentNodeKeyset = MongoUtils.parseKeyset(marshaller, currentNodeObject);
        if (nodeExistsInCache(currentNodeKeyset)) {
          // this node may have been added previously. If it has been, then we know that we may actually be in
          // the middle of investigating it, some recursion levels upwards. Therefore if we hit a known node,
          // don't add the current edge, and move on to the next one without investigating further children.
          logger.log(Level.FINE,
              "JGraphX node {0} already present. Skipping entire edge and node addition.",
              currentNodeKeyset.toString());
          continue;
        }
        Object jgraphNode = addNode(currentNodeObject);
        logger.log(Level.FINE, "Added node to JGraphX: {0}", currentNodeKeyset.toString());
//        org.gephi.graph.api.Node gNode = parseEntanglementNode(currentNodeObject, attributeModel);


        // add the current edge's information. This cannot be added until nodes at both ends have been added.
        Object jgraphEdge = addEdge(edgeObj);
        if (jgraphEdge != null) {
          logger.log(Level.FINE, "Added edge to JGraphX: {0}", currentEdge.getKeys().toString());
        }

        Node currentNode = marshaller.deserialize(currentNodeObject, Node.class);
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

//  /**
//   * This will reformat the default value stored within a Gephi node label to be a String[].
//   * Assumes that the label being passed is one that has been auto-generated by this class via parseEntanglementNode.
//   * String[] aren't used by default for the Gephi export, as Gephi expects only a String as the label value.
//   *
//   * @param label the string to parse
//   * @return a cleaned String[] which stores all labels in their own position in the array
//   */
//  @SuppressWarnings("UnusedDeclaration")
//  public static String[] getLabelArray(String label) {
//    // if there is only one label, just add it and return immediately
//    if (!label.contains(LABEL_SPLIT_REGEX)) {
//      return new String[]{label};
//    }
//
//    // cut off leading and trailing chars.
//    label = label.substring(LABEL_LIST_START.length());
//    label = label.substring(0, label.length() - LABEL_LIST_END.length());
//
//    // split the label
//    return label.split(LABEL_SPLIT_REGEX);
//  }


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

  public mxGraph getGraph() {
    return graph;
  }

  public Object getParentContainer() {
    return parentContainer;
  }
}
