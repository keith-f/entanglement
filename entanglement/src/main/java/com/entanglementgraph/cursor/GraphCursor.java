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

package com.entanglementgraph.cursor;

import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.util.GraphConnection;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.BotLoggerFactory;
import com.scalesinformatics.uibot.BotLoggerIrc;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * This class implements a 'cursor' that is able to walk through a graph from node to node. This class may be more
 * convenient that using the low-level <code>NodeDAO</code> and <code>EdgeDAO</code> means of accessing a graph since
 * it makes the process of stepping between nodes much smoother and less disjoint.
 *
 * Cursor objects represent a particular step through a graph and are immutable once created. This approach allows for
 * a history of graph movement operations to be logged. Graph histories may be useful for a number of applications
 * including: logging and debugging, graphical display, or even motif pattern matching operations.
 *
 *
 *
 * 1) Cursor name
 * 2a) Distributed history object with the same name (or with a prefix).
 * 2b) ... containing history items with a reason, node
 *
 * @author Keith Flanagan
 */
public class GraphCursor implements Serializable {
//  private static final Logger logger = Logger.getLogger(GraphCursor.class.getName());
  private static final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();
  private static final String HZ_HISTORY_PREFIX = GraphCursor.class.getName() + "_";


  public static enum MovementTypes {
    START_POSITION,
    JUMP,
    STEP_VIA_EDGE,
    STEP_TO_NODE,
    STEP_TO_FIRST_NODE_OF_TYPE,  //TODO needed?
    STEP_VIA_FIRST_EDGE_OF_TYPE; //TODO needed?
  }

  public static class HistoryItem {
    /**
     * The coordinate of the node that we ended up at.
     */
    private Node destination;

    /**
     * The keyset of the Edge that the cursor travelled along in order to reach the <code>destination</code>
     * (assuming that the cursor travelled via an edge, and didn't 'jump' to its destination).
     */
    private Edge arrivedVia;

    /**
     * The reason why this history item exists. Examples: the initial starting point;
     * a step via a particular edge type; a step to a particular node type.
     */
    private MovementTypes reason;

    private HistoryItem() {
    }

    private HistoryItem(Node destination, Edge arrivedVia, MovementTypes reason) {
      this.destination = destination;
      this.arrivedVia = arrivedVia;
      this.reason = reason;
    }

    public Node getDestination() {
      return destination;
    }

    public void setDestination(Node destination) {
      this.destination = destination;
    }

    public Edge getArrivedVia() {
      return arrivedVia;
    }

    public void setArrivedVia(Edge arrivedVia) {
      this.arrivedVia = arrivedVia;
    }

    public MovementTypes getReason() {
      return reason;
    }

    public void setReason(MovementTypes reason) {
      this.reason = reason;
    }
  }

  private final GraphConnection conn;
  private final BotLogger logger;
  private final HazelcastInstance hz;
  private final String name;
  private final IList<HistoryItem> historyItems;

  public GraphCursor(BotLogger parentLogger, HazelcastInstance hz, GraphConnection conn, String cursorName) {
    this.logger = BotLoggerFactory.createNewLogger(parentLogger, "Cur|"+entFormat.formatCursorName(cursorName).toString());
    this.hz = hz;
    this.conn = conn;
    this.name = cursorName;
    this.historyItems = hz.getList(HZ_HISTORY_PREFIX+cursorName);
  }


  /*
   * Utilities
   */

  /**
   * A convenience method that records this GraphCursor in a distributed history, and notifies other processes
   * of the cursor's new position. You should call this method after every cursor movement if this cursor is a
   * distributed cursor.
   * @param hzInstance
   */
  private void recordHistoryAndNotify(HazelcastInstance hzInstance, GraphCursor cursor) {
    if (hzInstance != null) {
      //Update current position and history for this named cursor
      hzInstance.getMap(GraphCursorRegistry.HZ_CURSOR_POSITIONS_MAP).put(name, cursor);
      hzInstance.getMultiMap(GraphCursorRegistry.HZ_CURSOR_HISTORIES_MULTIMAP).put(name, cursor);
    }
  }

  private Node currentPosition() {
    if (historyItems.isEmpty()) {
      return null;
    }
    return historyItems.get(historyItems.size()-1).getDestination();
  }


  /*
   * Cursor movement
   */

  /**
   * Starts or restarts a cursor session at a specified node.
   * Note that this method deletes all previous history.
   *
   * @param node the node to start at for this cursor session.
   * @return this GraphCursor instance for convenient method chaining.
   * @throws GraphCursorException
   */
  public GraphCursor start(Node node) throws GraphCursorException {
    logger.println("Clearing history and setting new starting point: %s",
        entFormat.formatNodeKeysetShort(node.getKeys(), 3).toString());
    historyItems.clear();

    HistoryItem item = new HistoryItem(node, null, MovementTypes.START_POSITION);
    historyItems.add(item);
    return this;
  }

  /**
   * Causes the cursor to jump to the specified node. The destination node does not have to be related to the current
   * position in any way.
   * @param node
   * @return this GraphCursor instance for convenient method chaining.
   * @throws GraphCursorException
   */
  public GraphCursor jump(Node node) throws GraphCursorException {
    HistoryItem item = new HistoryItem(node, null, MovementTypes.JUMP);
    historyItems.add(item);
    return this;
  }

  /**
   * Causes the cursor to step along an edge (<code>via</code>) to a destination node.
   *
   * @param via the edge that the cursor should step along. Note that this edge <i>must</i> link from/to the current
   *            node.
   * @return this GraphCursor instance for convenient method chaining.
   * @throws GraphCursorException
   */
  public GraphCursor stepVia(Edge via)
      throws GraphCursorException {
    Node current = currentPosition();
    if (current == null) {
      throw new GraphCursorException("This cursor hasn't been placed on any node yet!");
    }

    logger.println("Stepping from: %s, via edge %s",
        entFormat.formatNodeKeysetShort(current.getKeys(), 3).toString(),
        entFormat.formatEdgeKeyset(via.getKeys()).toString()
    );

    // Ensure that the user-specified edge actually links the current node (and also find the direction of travel).
    boolean edgeFromTo;
    if (EntityKeys.doKeysetsReferToSameEntity(current.getKeys(), via.getFrom(), false)) {
      edgeFromTo = true; // We're travelling in the direction: from --> to
    } else if (EntityKeys.doKeysetsReferToSameEntity(current.getKeys(), via.getFrom(), false)) {
      edgeFromTo = false; // We're travelling in the direction: to --> from
    } else {
      throw new GraphCursorException("Attempted to step from: "+current.getKeys()
          + " via: "+via.getKeys()+". However, we couldn't link the node's keyset with either end of the edge. "
          + "Are you sure the specified edge is connected to the current node?");
    }


    // Lookup specified edge
    Edge fullEdge;
    try {
      fullEdge = conn.getEdgeDao().getByKey(via.getKeys());
      logger.println("Resolved full edge edge: %s",
          entFormat.formatEdgeKeyset(via.getKeys()).toString());
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database for edge: "+via.getKeys(), e);
    }
    if (fullEdge == null) {
      throw new GraphCursorException("The specified edge: "+via.getKeys()+" could not be found.");
    }


    Node destination = new Node(edgeFromTo ? fullEdge.getTo() : fullEdge.getFrom());

    HistoryItem item = new HistoryItem(destination, fullEdge, MovementTypes.STEP_VIA_EDGE);
    historyItems.add(item);

    return this;
  }

  /**
   * Steps the cursor to the specified directly-connected node. Nodes connected to both incoming and outgoing edges
   * from the current cursor position are considered.
   *
   * @param specifiedDestination a specification for the destination node. This may be a complete, fully populated
   *                        <code>EntityKeys</code> instance, or it may be a partial object. For example, if only a
   *                        single UID or single type/name combination is specified, this may be enough in most cases.
   *                        Even specifying a single 'name' may be appropriate as long as the name is unique among
   *                        directly connected nodes.
   * @return a new graph cursor representing the new position.
   * @throws GraphCursorException if the specified node was not directly connected to the current location.
   */
  public GraphCursor stepToNode(Node specifiedDestination)
      throws GraphCursorException {
    return null;
//    DBObject edgeObj;
//    Edge edge;
//    Node actualDestination;
//    GraphConnection conn = c.getConn();
//    try (Iterable<Edge> edgeItr = conn.getEdgeDao().iterateEdgesBetweenNodes(position.getKeys(), specifiedDestination.getKeys())) {
//      if (!edgeItr.hasNext()) {
//        // There are no edges between the cursor and the specified destination.
//        throw new GraphCursorException("The following nodes are not directly related: "+ position +" and "+specifiedDestination);
//      }
//
//      // If there happen to be multiple destinations, we don't care here - just pick the first one.
//      edgeObj = dbCursor.next();
//      edge = conn.getMarshaller().deserialize(edgeObj, Edge.class);
//
//      // The iterator may return either outgoing or incoming edges. Decide which node to use here.
//      boolean fromContainsDestinationSpec = EntityKeys.doKeysetsReferToSameEntity(edge.getFrom(), specifiedDestination, true);
//      boolean toContainsDestinationSpec = EntityKeys.doKeysetsReferToSameEntity(edge.getTo(), specifiedDestination, true);
//
//      if (fromContainsDestinationSpec && toContainsDestinationSpec) {
//        //We couldn't tell the difference between from/to
//        logger.info("For information - 'from' and 'to' refer to the same node. " +
//            "This is probably a circular reference and nothing to worry about!. Edge was: "+edge.toString());
//      }
//      actualDestination = fromContainsDestinationSpec ? edge.getFrom() : edge.getTo();
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to iterate edges between nodes: "+ position +" and "+specifiedDestination, e);
//    } catch (DbObjectMarshallerException e) {
//      throw new GraphCursorException("Failed to deserialise an object", e);
//    }
//
//    Map<String, Object> humanReadableProvenanceParams = new HashMap<>();
//    humanReadableProvenanceParams.put("specifiedDestination", specifiedDestination);
//
//    GraphCursor cursor = new GraphCursor(
//        name, MovementTypes.STEP_TO_NODE, this, edge.getKeys(), actualDestination, humanReadableProvenanceParams);
//    recordHistoryAndNotify(c.getHz(), cursor);
//    return cursor;
  }
//
//  public GraphCursor stepToFirstNodeOfType(CursorContext c, String remoteNodeType)
//      throws GraphCursorException {
//    return null;
//    Edge chosenEdge = null;
//    EntityKeys<Node> destination = null;
//    GraphConnection conn = c.getConn();
//    // Choose first matching edge - ends at a node of a specified type.
//    try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesFromNodeToNodeOfType(position, remoteNodeType)) {
//      for (DBObject edgeDoc : dbCursor) {
//        chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
//        destination = chosenEdge.getTo();
//        break;
//      }
//    } catch (Exception e) {
//      throw new GraphCursorException("Failed to query database", e);
//    }
//    // If we didn't find a suitable outgoing edge, check for an incoming edge
//    if (chosenEdge == null) {
//      try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesToNodeFromNodeOfType(position, remoteNodeType)) {
//        for (DBObject edgeDoc : dbCursor) {
//          chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
//          destination = chosenEdge.getFrom();
//          break;
//        }
//      } catch (Exception e) {
//        throw new GraphCursorException("Failed to query database", e);
//      }
//    }
//    if (chosenEdge == null) {
//      throw new GraphCursorException("Node: "+position+" is not connected to a node of type: "+remoteNodeType);
//    }
//
//    Map<String, Object> humanReadableProvenanceParams = new HashMap<>();
//    humanReadableProvenanceParams.put("destinationType", remoteNodeType);
//
//    GraphCursor cursor = new GraphCursor(
//        name, MovementTypes.STEP_TO_FIRST_NODE_OF_TYPE, this, chosenEdge.getKeys(), destination, humanReadableProvenanceParams);
//    recordHistoryAndNotify(c.getHz(), cursor);
//    return cursor;
//  }
//
//  public GraphCursor stepToFirstEdgeOfType(CursorContext c, String edgeType)
//      throws GraphCursorException {
//    return null;
//    Edge chosenEdge = null;
//    EntityKeys<Node> destination = null;
//    GraphConnection conn = c.getConn();
//    // Choose first matching outgoing edge.
//    try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesFromNode(edgeType, position)) {
//      for (DBObject edgeDoc : dbCursor) {
//        chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
//        destination = chosenEdge.getTo();
//        break;
//      }
//    } catch (Exception e) {
//      throw new GraphCursorException("Failed to query database", e);
//    }
//    // If we didn't find a suitable outgoing edge, check for an incoming edge
//    if (chosenEdge == null) {
//      try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesToNode(edgeType, position)) {
//        for (DBObject edgeDoc : dbCursor) {
//          chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
//          destination = chosenEdge.getFrom();
//          break;
//        }
//      } catch (Exception e) {
//        throw new GraphCursorException("Failed to query database", e);
//      }
//    }
//    if (chosenEdge == null) {
//      throw new GraphCursorException("Node: "+position+" is not connected to a node of type: "+destinationType);
//    }
//
//    Map<String, Object> humanReadableProvenanceParams = new HashMap<>();
//    humanReadableProvenanceParams.put("edgeType", edgeType);
//
//    GraphCursor cursor = new GraphCursor(
//        name, MovementTypes.STEP_VIA_FIRST_EDGE_OF_TYPE, this, chosenEdge.getKeys(), destination, humanReadableProvenanceParams);
//    recordHistoryAndNotify(c.getHz(), cursor);
//    return cursor;
//  }

  /*
   * Cursor queries
   */

//  public boolean isAtDeadEnd() {
//    return position == null;
//  }
//
//  public boolean exists(GraphConnection conn) throws GraphCursorException {
//    return false;
//    try {
//      return conn.getNodeDao().existsByKey(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query database", e);
//    }
//  }

  /**
   * Makes an attempt to load the database document that the current cursor position represents. If no matching
   * document could be found, then an exception is thrown
   * @param conn the graph connection to use
   * @return the MongoDB document for the current cursor location.
   * @throws GraphCursorException
   */
  public BasicDBObject resolve(GraphConnection conn)  throws GraphCursorException {
    return null;
//    BasicDBObject nodeDoc;
//    try {
//      nodeDoc = conn.getNodeDao().getByKey(position);
//      if (nodeDoc == null) {
//        nodeDoc = VirtualNodeFactory.createVirtualNodeForLocation(conn.getMarshaller(), position);
//      }
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query database", e);
//    } catch (DbObjectMarshallerException e) {
//      throw new GraphCursorException("Failed to create virtual node", e);
//    }
//    if (nodeDoc == null) {
//      throw new GraphCursorException("The current position could not be resolved (no database object exists) for: "+position);
//    }
//    return nodeDoc;
  }

  public <T> T resolveAndDeserialise(GraphConnection conn, Class<T> javaBeanType)  throws GraphCursorException {
    return null;
//    DBObject rawNodeDoc = resolve(conn);
//    try {
//      return conn.getMarshaller().deserialize(rawNodeDoc, javaBeanType);
//    } catch (DbObjectMarshallerException e) {
//      throw new GraphCursorException("Failed to deserialise to type: "+javaBeanType+". Document was: "+rawNodeDoc);
//    }
  }


  /*
   * Generic edge queries
   */
  public DBCursor iterateIncomingEdges(GraphConnection conn) throws GraphCursorException {
    return null;
//    try {
//      return conn.getEdgeDao().iterateEdgesToNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }
  public long countIncomingEdges(GraphConnection conn) throws GraphCursorException {
    return -1;
//    try {
//      return conn.getEdgeDao().countEdgesToNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }
  public DBCursor iterateOutgoingEdges(GraphConnection conn) throws GraphCursorException {
    return null;
//    try {
//      return conn.getEdgeDao().iterateEdgesFromNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }
  public long countOutgoingEdges(GraphConnection conn) throws GraphCursorException {
    return -1;
//    try {
//      return conn.getEdgeDao().countEdgesFromNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }

  /*
   * Edge by type queries
   */
  public DBCursor iterateOutgoingEdgesOfType(GraphConnection conn, String edgeType) throws GraphCursorException {
    return null;
//    try {
//      return conn.getEdgeDao().iterateEdgesFromNode(edgeType, position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }


  public Iterable<EntityKeys<? extends Node>> iterateOutgoingNodeRefs(GraphConnection conn) throws GraphCursorException {
    return null;
//    try {
//      DbObjectMarshaller m = conn.getMarshaller();
//      DBCursor edgeResults = conn.getEdgeDao().iterateEdgesFromNode(position);
////      return new KeyExtractingIterable<>(edgeResults, m, FIELD_TO_KEYS, EntityKeys.class);
//      /*
//       * MakeIterable is required here because KeyExtractingIterable can only provide Iterable<EntityKeys>,
//       * and not Iterable<EntityKeys<? extends Node>>. Thank Java generics for that.
//       */
//      return MakeIterable.byCastingElements(
//          new KeyExtractingIterable<>(edgeResults, m, FIELD_TO_KEYS, EntityKeys.class));
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }

  /*
   * ***************************** Iterate and resolve edge/node pairs
   */

  /**
   * Using the current cursor position as a starting point, iterates over every incoming or outgoing edge (depending
   * on the value of <code>outgoingEdges</code> and resolves the raw DBObject representation of the edge and also the
   * DBObject of the destination node. This class is useful when it is necessary to return node and edge data in
   * a single operation, rather than querying for them individually.
   *
   * @param conn the graph database connection to use.
   * @param outgoingEdges set 'true' to iterate over outgoing edges, or 'false' to iterate over incoming edges.
   * @return an Iterable set of source-edge-destination tuples. In the case where a database entry for a node doesn't
   * exist, but an <code>EntityKeys</code> exists in the edge (i.e. a hanging edges), then the edge will be resolved,
   * but the node document(s) will be NULL.
   * @throws GraphCursorException
   */
//  public Iterable<NodeEdgeNodeTuple> iterateAndResolveEdgeDestPairs(
//      final GraphConnection conn, final boolean outgoingEdges)
//      throws GraphCursorException {
//    return null;
//    final BasicDBObject subjectNode = resolve(conn);
//    return new EdgeIteratorToNENTupleIterator(conn, true, position, subjectNode, outgoingEdges, new Callable<DBCursor>() {
//      @Override
//      public DBCursor call() throws Exception {
//        return outgoingEdges
//            ? conn.getEdgeDao().iterateEdgesFromNode(position)
//            : conn.getEdgeDao().iterateEdgesToNode(position);
//      }
//    });
//  }

//  public NodeEdgeNodeTuple iterateAndResolveOneEdgeDestPair(
//      final GraphConnection conn, final boolean outgoingEdges)
//      throws GraphCursorException {
//    return null;
//    Iterator<NodeEdgeNodeTuple> itr = iterateAndResolveEdgeDestPairs(conn, outgoingEdges).iterator();
//    if (itr.hasNext()) {
//      return itr.next();
//    }
//    return null;
//  }

  /**
   * Using the current cursor position as a starting point, iterates over every incoming or outgoing edge (depending
   * on the value of <code>outgoingEdges</code> and resolves the raw DBObject representation of the edge and also the
   * DBObject of the destination node. This class is useful when it is necessary to return node and edge data in
   * a single operation, rather than querying for them individually.
   *
   * @param conn the graph database connection to use.
   * @param outgoingEdges set 'true' to iterate over outgoing edges, or 'false' to iterate over incoming edges.
   * @param remoteNodeType filters edges that don't have a node with type <code>remoteNodeType</code> at the remote
   *                       end.
   * @return an Iterable set of source-edge-destination tuples. In the case where a database entry for a node doesn't
   * exist, but an <code>EntityKeys</code> exists in the edge (i.e. a hanging edges), then the edge will be resolved,
   * but the node document(s) will be NULL.
   * @throws GraphCursorException
   */
//  public Iterable<NodeEdgeNodeTuple> iterateAndResolveEdgeDestPairsToRemoteNodeOfType(
//      final GraphConnection conn, final boolean outgoingEdges, final String remoteNodeType)
//      throws GraphCursorException {
//    return null;
//    final BasicDBObject subjectNode = resolve(conn);
//    return new EdgeIteratorToNENTupleIterator(conn, true, position, subjectNode, outgoingEdges, new Callable<DBCursor>() {
//      @Override
//      public DBCursor call() throws Exception {
//        return outgoingEdges
//            ? conn.getEdgeDao().iterateEdgesFromNodeToNodeOfType(position, remoteNodeType)
//            : conn.getEdgeDao().iterateEdgesToNodeFromNodeOfType(position, remoteNodeType);
//      }
//    });
//  }

//  public NodeEdgeNodeTuple iterateAndResolveOneEdgeDestPairToRemoteNodeOfType(
//      final GraphConnection conn, final boolean outgoingEdges, final String remoteNodeType)
//      throws GraphCursorException {
//    Iterator<NodeEdgeNodeTuple> itr = iterateAndResolveEdgeDestPairsToRemoteNodeOfType(
//        conn, outgoingEdges, remoteNodeType).iterator();
//    if (itr.hasNext()) {
//      return itr.next();
//    }
//    return null;
//  }

  /**
   * Using the current cursor position as a starting point, iterates over every outgoing edge and resolves the raw
   * DBObject representation of the edge and also the DBObject of the destination node. This class is useful when it
   * is necessary to return node and edge data in a single operation, rather than querying for them individually.
   *
   * @param conn the graph database connection to use.
   * @return an Iterable set of source-edge-destination tuples. In the case where a database entry for a node doesn't
   * exist, but an <code>EntityKeys</code> exists in the edge (i.e. a hanging edges), then the edge will be resolved,
   * but the node document(s) will be NULL.
   * @throws GraphCursorException
   * @deprecated replaced by a more generic implementation
   */
//  public Iterable<NodeEdgeNodeTuple> iterateAndResolveOutgoingEdgeDestPairsOld(final GraphConnection conn)
//      throws GraphCursorException {
//    return null;
//    final DbObjectMarshaller m = conn.getMarshaller();
//    final BasicDBObject sourceNode = resolve(conn);
//    return new Iterable<NodeEdgeNodeTuple>() {
//      @Override
//      public Iterator<NodeEdgeNodeTuple> iterator() {
//        final DBCursor edgeItr;
//        try {
//          edgeItr = conn.getEdgeDao().iterateEdgesFromNode(position);
//        } catch (GraphModelException e) {
//          throw new RuntimeException("Failed to query database", e);
//        }
//        return new Iterator<NodeEdgeNodeTuple>() {
//
//          @Override
//          public boolean hasNext() {
//            return edgeItr.hasNext();
//          }
//
//          @Override
//          public NodeEdgeNodeTuple next() {
//            BasicDBObject edgeObj = (BasicDBObject) edgeItr.next();
//            try {
//              EntityKeys<? extends Node> destinationKeys = m.deserialize(edgeObj, Edge.class).getTo();
//              BasicDBObject destinationNode = conn.getNodeDao().getByKey(destinationKeys);
//              if (destinationNode == null) {
//                //This is probably a 'hanging' edge - we have the node reference, but no node exists.
//                logger.info("Potential hanging edge found: "+destinationKeys);
//              }
//              NodeEdgeNodeTuple nodeEdgeNode = new NodeEdgeNodeTuple(sourceNode, edgeObj, destinationNode);
//              return nodeEdgeNode;
//            } catch (Exception e) {
//              throw new RuntimeException("Failed to iterate destination nodes for: "+ position, e);
//            }
//
//          }
//
//          @Override
//          public void remove() {
//            throw new UnsupportedOperationException("remove() is not supported by this Iterator.");
//          }
//        };
//      }
//    };
//  }

  /**
   * Using the current cursor position as a destination, iterates over every incoming edge and resolves the raw
   * DBObject representation of the edge and also the DBObject of the source node. This class is useful when it
   * is necessary to return node and edge data in a single operation, rather than querying for them individually.
   *
   * @param conn the graph database connection to use.
   * @return an Iterable set of source-edge-destination tuples. In the case where a database entry for a node doesn't
   * exist, but an <code>EntityKeys</code> exists in the edge (i.e. a hanging edges), then the edge will be resolved,
   * but the node document(s) will be NULL.
   * @throws GraphCursorException
   * @deprecated replaced by a more generic implementation
   */
//  public Iterable<NodeEdgeNodeTuple> iterateAndResolveIncomingEdgeDestPairsOld(final GraphConnection conn)
//      throws GraphCursorException {
//    return null;
//    final DbObjectMarshaller m = conn.getMarshaller();
//    final BasicDBObject destinationNode = resolve(conn);
//    return new Iterable<NodeEdgeNodeTuple>() {
//      @Override
//      public Iterator<NodeEdgeNodeTuple> iterator() {
//        final DBCursor edgeItr;
//        try {
//          edgeItr = conn.getEdgeDao().iterateEdgesToNode(position);
//        } catch (GraphModelException e) {
//          throw new RuntimeException("Failed to query database", e);
//        }
//        return new Iterator<NodeEdgeNodeTuple>() {
//
//          @Override
//          public boolean hasNext() {
//            return edgeItr.hasNext();
//          }
//
//          @Override
//          public NodeEdgeNodeTuple next() {
//            BasicDBObject edgeObj = (BasicDBObject) edgeItr.next();
//            try {
//              EntityKeys<? extends Node> fromKeys = m.deserialize(edgeObj, Edge.class).getFrom();
//              BasicDBObject sourceNode = conn.getNodeDao().getByKey(fromKeys);
//              if (sourceNode == null) {
//                //This is probably a 'hanging' edge - we have the node reference, but no node exists.
//                logger.info("Potential hanging edge found: "+fromKeys);
//              }
//              NodeEdgeNodeTuple nodeEdgeNode = new NodeEdgeNodeTuple(sourceNode, edgeObj, destinationNode);
//              return nodeEdgeNode;
//            } catch (Exception e) {
//              throw new RuntimeException("Failed to iterate destination nodes for: "+ position, e);
//            }
//
//          }
//
//          @Override
//          public void remove() {
//            throw new UnsupportedOperationException("remove() is not supported by this Iterator.");
//          }
//        };
//      }
//    };
//  }

  /**
   * Returns the EntityKeys that represent the node that this cursor is currently located at. Note that the returned
   * EntityKeys contains only the UIDs and names that were originally specified in the constructor of this
   * <code>GraphCursor</code>. Therefore, the UIDs and names may be a subset of the actual EntityKeys for the node
   * as stored in the database.
   *
   * @return the EntityKeys object originally specified in the constructor for this GraphCursor.
   */
  public EntityKeys<? extends Node> getPosition() {
    return null;
//    return position;
  }

  /**
   * Returns the position within the graph cursor history that this GraphCursor instance is located at.
   *
   * @return
   */
//  public int getCursorHistoryIdx() {
//    return cursorHistoryIdx;
//  }

  /**
   * The name of this graph cursor.
   * @return
   */
//  public String getName() {
//    return name;
//  }

//  public MovementTypes getMovementType() {
//    return movementType;
//  }
//
//  public DestinationType getDestinationType() {
//    return destinationType;
//  }

//  public EntityKeys<? extends Edge> getArrivedVia() {
//    return null;
//    return arrivedVia;
//  }

//  public Map<String, Object> getParameters() {
//    return parameters;
//  }

}
