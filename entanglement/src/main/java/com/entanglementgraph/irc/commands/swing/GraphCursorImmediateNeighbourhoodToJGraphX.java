/*
 * Copyright 2013 Keith Flanagan
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

package com.entanglementgraph.irc.commands.swing;

import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.cursor.GraphCursorException;
import com.entanglementgraph.export.jgraphx.DefaultEdgeNoNamesVisuals;
import com.entanglementgraph.export.jgraphx.DefaultNodeVisuals;
import com.entanglementgraph.export.jgraphx.EdgeVisuals;
import com.entanglementgraph.export.jgraphx.NodeVisuals;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.jgraphx.EntanglementMxGraph;
import com.mongodb.DBObject;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.util.UidGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class takes an Entanglement GraphCursor instance, and populates an mxGraph with nodes/edges of the cursor
 * node itself, plus the immediate neightbourhood of the cursor.
 *
 * @author Keith Flanagan
 */
public class GraphCursorImmediateNeighbourhoodToJGraphX {
  private static final Logger logger = Logger.getLogger(GraphCursorImmediateNeighbourhoodToJGraphX.class.getName());

  private static final NodeVisuals DEFAULT_NODE_STYLE_INFO = new DefaultNodeVisuals();
  private static final EdgeVisuals DEFAULT_EDGE_STYLE_INFO = new DefaultEdgeNoNamesVisuals();
  private static final DbObjectMarshaller marshaller =
      ObjectMarshallerFactory.create(GraphCursorImmediateNeighbourhoodToJGraphX.class.getClassLoader());

  private final Map<String, NodeVisuals> nodeTypeToStyleInfo;
  private final Map<String, EdgeVisuals> edgeTypeToStyleInfo;

  private final EntanglementMxGraph mxGraph;


  // Keys to graph object cache - keep track of these so that we can add edges to nodes we've already added.
  // Map of UID -> JGraphX node objects
  private final Map<String, Object> uidToNode;
  // Map of type -> name -> JGraphX node objects
  private final Map<String, Map<String, Object>> typeToNameToNode;


  /**
   *
   * @param mxGraph
   */
  public GraphCursorImmediateNeighbourhoodToJGraphX(EntanglementMxGraph mxGraph)
      throws GraphCursorException {
    this.mxGraph = mxGraph;

    uidToNode = new HashMap<>();
    typeToNameToNode = new HashMap<>();
    nodeTypeToStyleInfo = new HashMap<>();
    edgeTypeToStyleInfo = new HashMap<>();
  }

  public void populateHelloWorld(GraphCursor cursor) {
    mxGraph.getModel().beginUpdate();
    Object parent = mxGraph.getDefaultParent();
    try
    {

      Object v1 = mxGraph.insertVertex(parent, null, "Hello", 20, 20, 80, 30);
      Object v2 = mxGraph.insertVertex(parent, null, "World!", 240, 150, 80, 30);
      mxGraph.insertEdge(parent, null, "Edge1", v1, v2);

      Object v3 = mxGraph.insertVertex(parent, null, new Integer(45), 20, 180, 80, 80);
      mxGraph.insertEdge(parent, null, "Edge2", v2, v3);
    }
    finally
    {
      mxGraph.getModel().endUpdate();
    }
  }


  public void populateImmediateNeighbourhood(GraphConnection graphConn, GraphCursor cursor) throws JGraphXPopulationException {
    mxGraph.getModel().beginUpdate();
    Object parent = mxGraph.getDefaultParent();
    try
    {
      DBObject cursorNode = cursor.resolve(graphConn);
      addNode(parent, cursorNode);

      for (GraphCursor.NodeEdgeNodeTuple nodeEdgeNode : cursor.iterateAndResolveOutgoingEdgeDestPairs(graphConn)) {
        addNode(parent, nodeEdgeNode.getRawDestinationNode());
        addEdge(parent, nodeEdgeNode.getRawEdge());
      }

      for (GraphCursor.NodeEdgeNodeTuple nodeEdgeNode : cursor.iterateAndResolveIncomingEdgeDestPairs(graphConn)) {
        addNode(parent, nodeEdgeNode.getRawSourceNode());
        addEdge(parent, nodeEdgeNode.getRawEdge());
      }

    }
    catch (GraphCursorException e) {
      throw new JGraphXPopulationException("Failed to populate subgraph around cursor", e);
    } catch (DbObjectMarshallerException e) {
      throw new JGraphXPopulationException("Failed to populate subgraph around cursor", e);
    } finally
    {
      mxGraph.getModel().endUpdate();
    }
  }

  /**
   * Adds a new node to a JGraphX graph, or, if at least one of the items in the <code>nodeObj</code> keyset matches
   * an entry in the node cache, returns the existing object instead.
   *
   * @param nodeObj the DBObject (Entanglement) node to add to the JGraph
   * @return the newly-created (or already existing matching) JGraph node
   * @throws com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException
   */
  private Object addNode(Object parentContainer, DBObject nodeObj) throws DbObjectMarshallerException {
//    logger.info("Adding node: "+nodeObj);

    //noinspection unchecked
    EntityKeys<Node> keyset = MongoUtils.parseKeyset(marshaller, nodeObj, NodeDAO.FIELD_KEYS);
    Object existingNode = getJGraphNodeFromCache(keyset);
    if (existingNode != null) {
      return existingNode;
    }

    NodeVisuals visualInfo = nodeTypeToStyleInfo.get(keyset.getType());
    if (visualInfo == null) {
      visualInfo = DEFAULT_NODE_STYLE_INFO;
    }

    String id = parseIdStringFromKeyset(keyset);
    Object jgraphNode = mxGraph.insertVertex(parentContainer, id, visualInfo.toBasicString(keyset, nodeObj), 0, 0,
        visualInfo.getDefaultWidth(), visualInfo.getDefaultHeight(), keyset.getType());
    cacheJGraphXNode(keyset, jgraphNode);
    return jgraphNode;
  }


  private Object addEdge(Object parentContainer, DBObject edgeObj) throws DbObjectMarshallerException {
//    logger.info("Adding edge: "+edgeObj);
    Edge edge = marshaller.deserialize(edgeObj, Edge.class);
    Object jgraphFromNode = getJGraphNodeFromCache(edge.getFrom());
    Object jgraphToNode = getJGraphNodeFromCache(edge.getTo());

    EdgeVisuals visualInfo = edgeTypeToStyleInfo.get(edge.getKeys().getType());
    if (visualInfo == null) {
      visualInfo = DEFAULT_EDGE_STYLE_INFO;
    }

    String id = parseIdStringFromKeyset(edge.getKeys());
    return mxGraph.insertEdge(parentContainer, id,
        visualInfo.toBasicString(edge.getKeys(), edgeObj), jgraphFromNode, jgraphToNode);
  }

  /**
   * @param keyset     the keyset to use when caching
   * @param jgraphNode the node to cache
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
   *
   * @param keyset the keyset associated with the JGraph node of interest
   * @return the JGraph node associated with the EntityKey, or null if not found.
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


}
