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

import static com.entanglementgraph.graph.EdgeDAO.*;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.util.GraphConnection;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.mongodb.dbobject.KeyExtractingIterable;
import com.scalesinformatics.util.generics.MakeIterable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
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
 * @author Keith Flanagan
 */
public class GraphCursor implements Serializable {
  private static final Logger logger = Logger.getLogger(GraphCursor.class.getName());
  private static final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();

  public static class CursorContext {
    final GraphConnection conn;
    final HazelcastInstance hz;

    public CursorContext(GraphConnection conn, HazelcastInstance hz) {
      this.conn = conn;
      this.hz = hz;
    }

    public GraphConnection getConn() {
      return conn;
    }

    public HazelcastInstance getHz() {
      return hz;
    }
  }

  public static class NodeEdgeNodeTuple implements Serializable {
    private BasicDBObject rawSourceNode;
    private BasicDBObject rawEdge;
    private BasicDBObject rawDestinationNode;

    public NodeEdgeNodeTuple(BasicDBObject rawSourceNode, BasicDBObject rawEdge, BasicDBObject rawDestinationNode) {
      this.rawSourceNode = rawSourceNode;
      this.rawEdge = rawEdge;
      this.rawDestinationNode = rawDestinationNode;
    }

    public BasicDBObject getRawEdge() {
      return rawEdge;
    }

    public void setRawEdge(BasicDBObject rawEdge) {
      this.rawEdge = rawEdge;
    }

    public BasicDBObject getRawSourceNode() {
      return rawSourceNode;
    }

    public void setRawSourceNode(BasicDBObject rawSourceNode) {
      this.rawSourceNode = rawSourceNode;
    }

    public BasicDBObject getRawDestinationNode() {
      return rawDestinationNode;
    }

    public void setRawDestinationNode(BasicDBObject rawDestinationNode) {
      this.rawDestinationNode = rawDestinationNode;
    }
  };

  public static enum MovementTypes {
    START_POSITION,
    JUMP,
    STEP_TO_NODE,
    STEP_TO_FIRST_NODE_OF_TYPE,
    STEP_VIA_FIRST_EDGE_OF_TYPE;
  }

  public static enum DestinationType {
    NODE,
    DEAD_END;
  }


  private final String name;
  private final int cursorHistoryIdx;

  /**
   * The reason why this history item exists. Examples: the initial starting point;
   * a step via a particular edge type; a step to a particular node type.
   */
  private final MovementTypes movementType;

  /**
   * The coordinate of the node that we ended up at.
   */
  private final EntityKeys<? extends Node> position;

  private final DestinationType destinationType;

  /**
   * The keyset of the Edge that the cursor travelled along in order to reach the <code>destination</code>
   * (assuming that the cursor travelled via an edge, and didn't 'jump' to its destination).
   */
  private final EntityKeys<? extends Edge> arrivedVia;

  /**
   * Any parameters that were specified at the time of the move. This might be a node or edge type name, or a
   * custom MongoDB query. Parameters are mostly for debugging/provenance and only need to be human-readable.
   */
  private final Map<String, Object> parameters;



  public GraphCursor(String cursorName, EntityKeys<? extends Node> startNode) {
    this.name = cursorName;
    this.position = startNode;
    this.movementType = MovementTypes.START_POSITION;
    this.arrivedVia = null;
    this.parameters = null;
    cursorHistoryIdx = 0;

    if (startNode == null) {
      destinationType = DestinationType.DEAD_END;
    } else {
      destinationType = DestinationType.NODE;
    }
  }


  protected GraphCursor(String cursorName, MovementTypes movementType,
                        GraphCursor previousLocation, EntityKeys<? extends Edge> arrivedVia, EntityKeys<? extends Node> newPosition,
                        Map<String, Object> parameters) {
    this.name = cursorName;
    this.position = newPosition;
    this.movementType = movementType;
    this.arrivedVia = arrivedVia;
    this.parameters = parameters;
    cursorHistoryIdx = previousLocation.getCursorHistoryIdx() + 1;

    if (newPosition == null) {
      destinationType = DestinationType.DEAD_END;
    } else {
      destinationType = DestinationType.NODE;
    }
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


  /*
   * Cursor movement
   */

  /**
   * Causes the cursor to jump to the specified node. The destination node does not have to be related to the current
   * position in any way.
   * @param destinationNode
   * @return
   * @throws GraphCursorException
   */
  public GraphCursor jump(CursorContext c,
                          EntityKeys<? extends Node> destinationNode) throws GraphCursorException {
    GraphCursor cursor = new GraphCursor(name, MovementTypes.JUMP, this, null, destinationNode, null);
    recordHistoryAndNotify(c.getHz(), cursor);
    return cursor;
  }

  /**
   * Steps the cursor to the specified directly connected node. Nodes connected to both incoming and outgoing edges
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
  public GraphCursor stepToNode(CursorContext c,
                                EntityKeys<? extends Node> specifiedDestination)
      throws GraphCursorException {
    DBObject edgeObj;
    Edge edge;
    EntityKeys<Node> actualDestination;
    GraphConnection conn = c.getConn();
    try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesBetweenNodes(position, specifiedDestination)) {
      if (!dbCursor.hasNext()) {
        // There are no edges between the cursor and the specified destination.
        throw new GraphCursorException("The following nodes are not directly related: "+ position +" and "+specifiedDestination);
      }

      // If there happen to be multiple destinations, we don't care here - just pick the first one.
      edgeObj = dbCursor.next();
      edge = conn.getMarshaller().deserialize(edgeObj, Edge.class);

      // The iterator may return either outgoing or incoming edges. Decide which node to use here.
      boolean fromContainsDestinationSpec = EntityKeys.doKeysetsReferToSameEntity(edge.getFrom(), specifiedDestination, true);
      boolean toContainsDestinationSpec = EntityKeys.doKeysetsReferToSameEntity(edge.getTo(), specifiedDestination, true);

      if (fromContainsDestinationSpec && toContainsDestinationSpec) {
        //We couldn't tell the difference between from/to
        logger.info("For information - 'from' and 'to' refer to the same node. " +
            "This is probably a circular reference and nothing to worry about!. Edge was: "+edge.toString());
      }
      actualDestination = fromContainsDestinationSpec ? edge.getFrom() : edge.getTo();
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to iterate edges between nodes: "+ position +" and "+specifiedDestination, e);
    } catch (DbObjectMarshallerException e) {
      throw new GraphCursorException("Failed to deserialise an object", e);
    }

    Map<String, Object> humanReadableProvenanceParams = new HashMap<>();
    humanReadableProvenanceParams.put("specifiedDestination", specifiedDestination);

    GraphCursor cursor = new GraphCursor(
        name, MovementTypes.STEP_TO_NODE, this, edge.getKeys(), actualDestination, humanReadableProvenanceParams);
    recordHistoryAndNotify(c.getHz(), cursor);
    return cursor;
  }

  public GraphCursor stepToFirstNodeOfType(CursorContext c, String remoteNodeType)
      throws GraphCursorException {
    //DBObject chosenEdgeDoc = null;
    Edge chosenEdge = null;
    EntityKeys<Node> destination = null;
    GraphConnection conn = c.getConn();
    // Choose first matching edge - ends at a node of a specified type.
    try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesFromNodeToNodeOfType(position, remoteNodeType)) {
      for (DBObject edgeDoc : dbCursor) {
        //chosenEdgeDoc = edgeDoc;
        chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
        destination = chosenEdge.getTo();
        break;
      }
    } catch (Exception e) {
      throw new GraphCursorException("Failed to query database", e);
    }
    // If we didn't find a suitable outgoing edge, check for an incoming edge
    if (chosenEdge == null) {
      try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesToNodeFromNodeOfType(position, remoteNodeType)) {
        for (DBObject edgeDoc : dbCursor) {
          //chosenEdgeDoc = edgeDoc;
          chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
          destination = chosenEdge.getFrom();
          break;
        }
      } catch (Exception e) {
        throw new GraphCursorException("Failed to query database", e);
      }
    }
    if (chosenEdge == null) {
      throw new GraphCursorException("Node: "+position+" is not connected to a node of type: "+remoteNodeType);
    }

    Map<String, Object> humanReadableProvenanceParams = new HashMap<>();
    humanReadableProvenanceParams.put("destinationType", remoteNodeType);

    GraphCursor cursor = new GraphCursor(
        name, MovementTypes.STEP_TO_FIRST_NODE_OF_TYPE, this, chosenEdge.getKeys(), destination, humanReadableProvenanceParams);
    recordHistoryAndNotify(c.getHz(), cursor);
    return cursor;
  }

  public GraphCursor stepToFirstEdgeOfType(CursorContext c, String edgeType)
      throws GraphCursorException {
    Edge chosenEdge = null;
    EntityKeys<Node> destination = null;
    GraphConnection conn = c.getConn();
    // Choose first matching outgoing edge.
    try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesFromNode(edgeType, position)) {
      for (DBObject edgeDoc : dbCursor) {
        chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
        destination = chosenEdge.getTo();
        break;
      }
    } catch (Exception e) {
      throw new GraphCursorException("Failed to query database", e);
    }
    // If we didn't find a suitable outgoing edge, check for an incoming edge
    if (chosenEdge == null) {
      try (DBCursor dbCursor = conn.getEdgeDao().iterateEdgesToNode(edgeType, position)) {
        for (DBObject edgeDoc : dbCursor) {
          chosenEdge = conn.getMarshaller().deserialize(edgeDoc, Edge.class);
          destination = chosenEdge.getFrom();
          break;
        }
      } catch (Exception e) {
        throw new GraphCursorException("Failed to query database", e);
      }
    }
    if (chosenEdge == null) {
      throw new GraphCursorException("Node: "+position+" is not connected to a node of type: "+destinationType);
    }

    Map<String, Object> humanReadableProvenanceParams = new HashMap<>();
    humanReadableProvenanceParams.put("edgeType", edgeType);

    GraphCursor cursor = new GraphCursor(
        name, MovementTypes.STEP_VIA_FIRST_EDGE_OF_TYPE, this, chosenEdge.getKeys(), destination, humanReadableProvenanceParams);
    recordHistoryAndNotify(c.getHz(), cursor);
    return cursor;
  }

  /*
   * Cursor queries
   */

  public boolean isAtDeadEnd() {
    return position == null;
  }

  public boolean exists(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getNodeDao().existsByKey(position);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database", e);
    }
  }

  /**
   * Makes an attempt to load the database document that the current cursor position represents. If no matching
   * document could be found, then an exception is thrown
   * @param conn the graph connection to use
   * @return the MongoDB document for the current cursor location.
   * @throws GraphCursorException
   */
  public BasicDBObject resolve(GraphConnection conn)  throws GraphCursorException {
    BasicDBObject nodeDoc;
    try {
      nodeDoc = conn.getNodeDao().getByKey(position);
      if (nodeDoc == null) {
        nodeDoc = VirtualNodeFactory.createVirtualNodeForLocation(conn.getMarshaller(), position);
      }
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database", e);
    } catch (DbObjectMarshallerException e) {
      throw new GraphCursorException("Failed to create virtual node", e);
    }
    if (nodeDoc == null) {
      throw new GraphCursorException("The current position could not be resolved (no database object exists) for: "+position);
    }
    return nodeDoc;

  }

  public <T> T resolveAndDeserialise(GraphConnection conn, Class<T> javaBeanType)  throws GraphCursorException {
    DBObject rawNodeDoc = resolve(conn);
    try {
      return conn.getMarshaller().deserialize(rawNodeDoc, javaBeanType);
    } catch (DbObjectMarshallerException e) {
      throw new GraphCursorException("Failed to deserialise to type: "+javaBeanType+". Document was: "+rawNodeDoc);
    }
  }


  /*
   * Generic edge queries
   */
  public DBCursor iterateIncomingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().iterateEdgesToNode(position);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }
  public long countIncomingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().countEdgesToNode(position);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }
  public DBCursor iterateOutgoingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().iterateEdgesFromNode(position);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }
  public long countOutgoingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().countEdgesFromNode(position);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }

  /*
   * Edge by type queries
   */
  public DBCursor iterateOutgoingEdgesOfType(GraphConnection conn, String edgeType) throws GraphCursorException {
    try {
      return conn.getEdgeDao().iterateEdgesFromNode(edgeType, position);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }


  public Iterable<EntityKeys<? extends Node>> iterateOutgoingNodeRefs(GraphConnection conn) throws GraphCursorException {
    try {
      DbObjectMarshaller m = conn.getMarshaller();
      DBCursor edgeResults = conn.getEdgeDao().iterateEdgesFromNode(position);
//      return new KeyExtractingIterable<>(edgeResults, m, FIELD_TO_KEYS, EntityKeys.class);
      /*
       * MakeIterable is required here because KeyExtractingIterable can only provide Iterable<EntityKeys>,
       * and not Iterable<EntityKeys<? extends Node>>. Thank Java generics for that.
       */
      return MakeIterable.byCastingElements(
          new KeyExtractingIterable<>(edgeResults, m, FIELD_TO_KEYS, EntityKeys.class));
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
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
  public Iterable<NodeEdgeNodeTuple> iterateAndResolveEdgeDestPairs(
      final GraphConnection conn, final boolean outgoingEdges)
      throws GraphCursorException {
    final BasicDBObject subjectNode = resolve(conn);
    return new EdgeIteratorToNENTupleIterator(conn, true, position, subjectNode, outgoingEdges, new Callable<DBCursor>() {
      @Override
      public DBCursor call() throws Exception {
        return outgoingEdges
            ? conn.getEdgeDao().iterateEdgesFromNode(position)
            : conn.getEdgeDao().iterateEdgesToNode(position);
      }
    });
  }

  public NodeEdgeNodeTuple iterateAndResolveOneEdgeDestPair(
      final GraphConnection conn, final boolean outgoingEdges)
      throws GraphCursorException {
    Iterator<NodeEdgeNodeTuple> itr = iterateAndResolveEdgeDestPairs(conn, outgoingEdges).iterator();
    if (itr.hasNext()) {
      return itr.next();
    }
    return null;
  }

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
  public Iterable<NodeEdgeNodeTuple> iterateAndResolveEdgeDestPairsToRemoteNodeOfType(
      final GraphConnection conn, final boolean outgoingEdges, final String remoteNodeType)
      throws GraphCursorException {
    final BasicDBObject subjectNode = resolve(conn);
    return new EdgeIteratorToNENTupleIterator(conn, true, position, subjectNode, outgoingEdges, new Callable<DBCursor>() {
      @Override
      public DBCursor call() throws Exception {
        return outgoingEdges
            ? conn.getEdgeDao().iterateEdgesFromNodeToNodeOfType(position, remoteNodeType)
            : conn.getEdgeDao().iterateEdgesToNodeFromNodeOfType(position, remoteNodeType);
      }
    });
  }

  public NodeEdgeNodeTuple iterateAndResolveOneEdgeDestPairToRemoteNodeOfType(
      final GraphConnection conn, final boolean outgoingEdges, final String remoteNodeType)
      throws GraphCursorException {
    Iterator<NodeEdgeNodeTuple> itr = iterateAndResolveEdgeDestPairsToRemoteNodeOfType(
        conn, outgoingEdges, remoteNodeType).iterator();
    if (itr.hasNext()) {
      return itr.next();
    }
    return null;
  }

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
  public Iterable<NodeEdgeNodeTuple> iterateAndResolveOutgoingEdgeDestPairsOld(final GraphConnection conn)
      throws GraphCursorException {
    final DbObjectMarshaller m = conn.getMarshaller();
    final BasicDBObject sourceNode = resolve(conn);
    return new Iterable<NodeEdgeNodeTuple>() {
      @Override
      public Iterator<NodeEdgeNodeTuple> iterator() {
        final DBCursor edgeItr;
        try {
          edgeItr = conn.getEdgeDao().iterateEdgesFromNode(position);
        } catch (GraphModelException e) {
          throw new RuntimeException("Failed to query database", e);
        }
        return new Iterator<NodeEdgeNodeTuple>() {

          @Override
          public boolean hasNext() {
            return edgeItr.hasNext();
          }

          @Override
          public NodeEdgeNodeTuple next() {
            BasicDBObject edgeObj = (BasicDBObject) edgeItr.next();
            try {
              EntityKeys<? extends Node> destinationKeys = m.deserialize(edgeObj, Edge.class).getTo();
              BasicDBObject destinationNode = conn.getNodeDao().getByKey(destinationKeys);
              if (destinationNode == null) {
                //This is probably a 'hanging' edge - we have the node reference, but no node exists.
                logger.info("Potential hanging edge found: "+destinationKeys);
              }
              NodeEdgeNodeTuple nodeEdgeNode = new NodeEdgeNodeTuple(sourceNode, edgeObj, destinationNode);
              return nodeEdgeNode;
            } catch (Exception e) {
              throw new RuntimeException("Failed to iterate destination nodes for: "+ position, e);
            }

          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove() is not supported by this Iterator.");
          }
        };
      }
    };
  }

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
  public Iterable<NodeEdgeNodeTuple> iterateAndResolveIncomingEdgeDestPairsOld(final GraphConnection conn)
      throws GraphCursorException {
    final DbObjectMarshaller m = conn.getMarshaller();
    final BasicDBObject destinationNode = resolve(conn);
    return new Iterable<NodeEdgeNodeTuple>() {
      @Override
      public Iterator<NodeEdgeNodeTuple> iterator() {
        final DBCursor edgeItr;
        try {
          edgeItr = conn.getEdgeDao().iterateEdgesToNode(position);
        } catch (GraphModelException e) {
          throw new RuntimeException("Failed to query database", e);
        }
        return new Iterator<NodeEdgeNodeTuple>() {

          @Override
          public boolean hasNext() {
            return edgeItr.hasNext();
          }

          @Override
          public NodeEdgeNodeTuple next() {
            BasicDBObject edgeObj = (BasicDBObject) edgeItr.next();
            try {
              EntityKeys<? extends Node> fromKeys = m.deserialize(edgeObj, Edge.class).getFrom();
              BasicDBObject sourceNode = conn.getNodeDao().getByKey(fromKeys);
              if (sourceNode == null) {
                //This is probably a 'hanging' edge - we have the node reference, but no node exists.
                logger.info("Potential hanging edge found: "+fromKeys);
              }
              NodeEdgeNodeTuple nodeEdgeNode = new NodeEdgeNodeTuple(sourceNode, edgeObj, destinationNode);
              return nodeEdgeNode;
            } catch (Exception e) {
              throw new RuntimeException("Failed to iterate destination nodes for: "+ position, e);
            }

          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove() is not supported by this Iterator.");
          }
        };
      }
    };
  }

  /**
   * Returns the EntityKeys that represent the node that this cursor is currently located at. Note that the returned
   * EntityKeys contains only the UIDs and names that were originally specified in the constructor of this
   * <code>GraphCursor</code>. Therefore, the UIDs and names may be a subset of the actual EntityKeys for the node
   * as stored in the database.
   *
   * @return the EntityKeys object originally specified in the constructor for this GraphCursor.
   */
  public EntityKeys<? extends Node> getPosition() {
    return position;
  }

  /**
   * Returns the position within the graph cursor history that this GraphCursor instance is located at.
   *
   * @return
   */
  public int getCursorHistoryIdx() {
    return cursorHistoryIdx;
  }

  /**
   * The name of this graph cursor.
   * @return
   */
  public String getName() {
    return name;
  }

  public MovementTypes getMovementType() {
    return movementType;
  }

  public DestinationType getDestinationType() {
    return destinationType;
  }

  public EntityKeys<? extends Edge> getArrivedVia() {
    return arrivedVia;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

}
