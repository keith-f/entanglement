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

import com.entanglementgraph.graph.*;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.util.GraphConnection;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.scalesinformatics.uibot.BotLogger;
import com.scalesinformatics.uibot.BotLoggerFactory;

import java.io.Serializable;
import java.util.Iterator;

/**
 * This class implements a 'cursor' that is able to walk through a graph from node to node. This class may be more
 * convenient that using the low-level <code>NodeDAO</code> and <code>EdgeDAO</code> means of accessing a graph since
 * it makes the process of stepping between nodes much smoother and less disjoint.
 *
 * Graph histories may be useful for a number of applications
 * including: logging and debugging, graphical display, or even motif pattern matching operations.
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

  public static class HistoryItem implements Serializable {
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
   * Returns the a Node object represents the position that this cursor is currently located at. Note that the returned
   * Node object may:
   *
   * a) complete with content (if the content happened to be resolved, or was requested)
   *
   * b) a 'shell' object that only contains the node type and UIDs. In this case, the content of this node may be
   * resolvable if it exists in the database.
   *
   * c) an unresolvable virtual node that only contains the node type and UIDs. The 'content' of the node doesn't
   * exist in any configured graph and therefore can't be resolved, but the node can be stepped over like any other.
   *
   * @return the EntityKeys object originally specified in the constructor for this GraphCursor.
   */
  private Node currentPosition() {
    if (historyItems.isEmpty()) {
      return null;
    }
    return historyItems.get(historyItems.size()-1).getDestination();
  }



  public EntityKeys<? extends Node> getPosition() {
    return null;
//    return position;
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
      fullEdge = conn.getEdgeDao().getByKey(via.getKeys()); //FIXME not always necessary - test whether the provided entity is complete
      logger.println("Resolved full edge edge: %s",
          entFormat.formatEdgeKeyset(fullEdge.getKeys()).toString());
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
   * from the current cursor position are considered. The first available edge is picked.
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
    /*
     * 1) Find edges between the current location and the specified destination. If there are none, error.
     * 2) Pick the first edge, and move the cursor onto the destination.
     */

    Node current = currentPosition();
    Node fullDestination;
    Iterator<Edge<Content, Content, Content>> edgeItr;
    try {
      fullDestination = conn.getNodeDao().getByKey(specifiedDestination.getKeys()); //FIXME not always necessary - test whether the provided entity is complete
      edgeItr = conn.getEdgeDao().iterateEdgesBetweenNodes(current.getKeys(), specifiedDestination.getKeys()).iterator();
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database.", e);
    }

    if (!edgeItr.hasNext()) {
      throw new GraphCursorException("Attempted to move from: "+current+" to: "+specifiedDestination
          +". However, these two nodes do not appear to be directly linked.");
    }

    Edge<Content, Content, Content> edge = edgeItr.next();

    HistoryItem item = new HistoryItem(fullDestination, edge, MovementTypes.STEP_VIA_EDGE);
    historyItems.add(item);

    return this;
  }

  /**
   * Steps the cursor to the specified directly-connected node via the first available edge of the specified type.
   * Nodes connected to both incoming and outgoing edges from the current cursor position are considered (as long
   * as they match the type <code>edgeType</code>.
   *
   * @param specifiedDestination a specification for the destination node. This may be a complete, fully populated
   *                        <code>EntityKeys</code> instance, or it may be a partial object. For example, if only a
   *                        single UID or single type/name combination is specified, this may be enough in most cases.
   *                        Even specifying a single 'name' may be appropriate as long as the name is unique among
   *                        directly connected nodes.
   * @param edgeType the type of edge to attempt the step by.
   * @return a new graph cursor representing the new position.
   * @throws GraphCursorException if the specified node was not directly connected to the current location by an
   * edge of type <code>edgeType</code>.
   */
  public GraphCursor stepToNodeViaEdgeOfType(Node specifiedDestination, String edgeType)
      throws GraphCursorException {
    /*
     * 1) Find edges between the current location and the specified destination. If there are none, error.
     * 2) Pick the first edge, and move the cursor onto the destination.
     */

    Node current = currentPosition();
    Node fullDestination;
    Iterator<Edge<Content, Content, Content>> edgeItr;
    try {
      fullDestination = conn.getNodeDao().getByKey(specifiedDestination.getKeys()); //FIXME not always necessary - test whether the provided entity is complete
      edgeItr = conn.getEdgeDao().iterateEdgesBetweenNodes(edgeType, current.getKeys(), specifiedDestination.getKeys()).iterator();
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database.", e);
    }

    if (!edgeItr.hasNext()) {
      throw new GraphCursorException("Attempted to move from: "+current+" to: "+specifiedDestination
          +". However, these two nodes do not appear to be directly linked.");
    }

    Edge<Content, Content, Content> edge = edgeItr.next();

    HistoryItem item = new HistoryItem(fullDestination, edge, MovementTypes.STEP_VIA_EDGE);
    historyItems.add(item);

    return this;
  }


  /*
   * Generic edge queries
   */
//  public DBCursor iterateIncomingEdges(GraphConnection conn) throws GraphCursorException {
//    return null;
//    try {
//      return conn.getEdgeDao().iterateEdgesToNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
//  }
  public long countIncomingEdges(GraphConnection conn) throws GraphCursorException {
    return -1;
//    try {
//      return conn.getEdgeDao().countEdgesToNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
  }
//  public DBCursor iterateOutgoingEdges(GraphConnection conn) throws GraphCursorException {
//    return null;
//    try {
//      return conn.getEdgeDao().iterateEdgesFromNode(position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
//  }
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
//  public DBCursor iterateOutgoingEdgesOfType(GraphConnection conn, String edgeType) throws GraphCursorException {
//    return null;
//    try {
//      return conn.getEdgeDao().iterateEdgesFromNode(edgeType, position);
//    } catch (GraphModelException e) {
//      throw new GraphCursorException("Failed to query graph.", e);
//    }
//  }


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


}
