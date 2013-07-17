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
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.util.GraphConnection;
import com.scalesinformatics.uibot.BotLogger;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.mongodb.dbobject.KeyExtractingIterable;
import com.scalesinformatics.util.generics.MakeIterable;

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
 * @author Keith Flanagan
 */
public class GraphCursor implements Serializable {
  private static final Logger logger = Logger.getLogger(GraphCursor.class.getName());

  public static class EdgeDestPair implements Serializable {
    private DBObject rawEdge;
    private DBObject rawDestNode;

    public EdgeDestPair(DBObject rawEdge, DBObject rawDestNode) {
      this.rawEdge = rawEdge;
      this.rawDestNode = rawDestNode;
    }

    public DBObject getRawEdge() {
      return rawEdge;
    }

    public void setRawEdge(DBObject rawEdge) {
      this.rawEdge = rawEdge;
    }

    public DBObject getRawDestNode() {
      return rawDestNode;
    }

    public void setRawDestNode(DBObject rawDestNode) {
      this.rawDestNode = rawDestNode;
    }
  };

  public static enum MovementTypes {
    START_POSITION,
    JUMP,
    STEP_TO_FIRST_NODE_OF_TYPE,
    STEP_VIA_FIRST_EDGE_OF_TYPE;
  }

  public static enum DestinationType {
    NODE,
    DEAD_END;
  }

  /**
   * An item in a list of graph location histories. A history item represents a graph cursor movement from one node
   * to another node. The following fields are present:
   * <ul>
   *   <li>movementType - the reason why this history item exists. Examples: the initial starting point;
   *   a step via a particular edge type; a step to a particular node type.</li>
   *   <li>parameters - any parameters that were specified at the time of the move. This might be a node or edge type
   *   name, or a custom MongoDB query.</li>
   *   <li>destination - the coordinate of the node that we eventually ended up at.</li>
   *   <li>via - the keyset of the Edge that the cursor travelled along in order to reach the <code>destination</code>
   *   (assuming that the cursor travelled via an edge, and didn't 'jump' to its destination).</li>
   *   <li>associatedCursor (optional) - the GraphCursor object associated with this move.</li>
   * </ul>
   */
  public static class HistoryItem implements Serializable {
    private MovementTypes movementType;
    private Map<String, Object> parameters;
    private EntityKeys<? extends Edge> via;
    private DestinationType destinationType;
    private EntityKeys<? extends Node> destination;
    private GraphCursor associatedCursor;


    public HistoryItem(MovementTypes movementType, Map<String, Object> parameters) {
      this.movementType = movementType;
      this.parameters = parameters;
      if (this.parameters == null) {
        this.parameters = new HashMap<>();
      }
      setDestination(null);
    }

    public HistoryItem(MovementTypes movementType, Map<String, Object> parameters,
                       EntityKeys<? extends Edge> via, EntityKeys<? extends Node> destination) {
      this.movementType = movementType;
      this.parameters = parameters;
      if (this.parameters == null) {
        this.parameters = new HashMap<>();
      }
      this.via = via;
      setDestination(destination);

    }

    public void putParameter(String name, Object value) {
      parameters.put(name, value);
    }

    @Override
    public String toString() {
      return "HistoryItem{" +
          "movementType=" + movementType +
          ", destination=" + destination +
          ", parameters=" + parameters +
          '}';
    }

    public MovementTypes getMovementType() {
      return movementType;
    }

    public void setMovementType(MovementTypes movementType) {
      this.movementType = movementType;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
      this.parameters = parameters;
    }

    public EntityKeys<? extends Node> getDestination() {
      return destination;
    }

    public void setDestination(EntityKeys<? extends Node> destination) {
      this.destination = destination;
      if (destination == null) {
        destinationType = DestinationType.DEAD_END;
      } else {
        destinationType = DestinationType.NODE;
      }
    }

    public GraphCursor getAssociatedCursor() {
      return associatedCursor;
    }

    public void setAssociatedCursor(GraphCursor associatedCursor) {
      this.associatedCursor = associatedCursor;
    }

    public EntityKeys<? extends Edge> getVia() {
      return via;
    }

    public void setVia(EntityKeys<? extends Edge> via) {
      this.via = via;
    }

    public DestinationType getDestinationType() {
      return destinationType;
    }

  }

  private final String name;
  private final List<HistoryItem> history;
  private final int cursorHistoryIdx;
  private final EntityKeys<? extends Node> currentNode;

  public GraphCursor(String cursorName, EntityKeys<? extends Node> startNode)
      throws GraphCursorException {
    this.name = cursorName;
    this.currentNode = startNode;
    this.history = new LinkedList<>();
    cursorHistoryIdx = 0;
    addHistoryItemForThisLocation(null, new HistoryItem(MovementTypes.START_POSITION, null, null, currentNode));
  }


  protected GraphCursor(String cursorName, GraphCursor previousLocation, HistoryItem currentLocation)
      throws GraphCursorException {
    this.name = cursorName;
    this.currentNode = currentLocation.getDestination();
    this.history = previousLocation.getHistory();
    cursorHistoryIdx = previousLocation.getCursorHistoryIdx() + 1;
    addHistoryItemForThisLocation(previousLocation, currentLocation);
  }

  private void addHistoryItemForThisLocation(GraphCursor previousLocation, HistoryItem currentLocation)
      throws GraphCursorException {

    if (previousLocation != null && cursorHistoryIdx != history.size()) {
      throw new GraphCursorException("You have attempted to a step from the same GraphCursor ("+previousLocation+") " +
          "instance more than once, which is not currently possible with current linear history implementation.");
    }

    this.history.add(currentLocation);
    currentLocation.setAssociatedCursor(this);
  }


  /*
   * Utilities
   */
//  public <T> T deserialise(DBObject rawObject, Class<T> beanType) throws GraphCursorException {
//    try {
//      return m.deserialize(rawObject, beanType);
//    } catch (DbObjectMarshallerException e) {
//      throw new GraphCursorException("Failed to deserialise to type: "
//          +beanType.getName()+" from doc: "+rawObject.toString(), e);
//    }
//  }

  /*
   * Cursor movement
   */

  public GraphCursor jump(EntityKeys<? extends Node> destinationNode) throws GraphCursorException {
    HistoryItem historyItem = new HistoryItem(MovementTypes.JUMP, null, null, destinationNode);
    GraphCursor cursor = new GraphCursor(name, this, historyItem);
    return cursor;

  }

  public GraphCursor stepToFirstNodeOfType(GraphConnection conn, String nodeType) throws GraphCursorException {
    DbObjectMarshaller m = conn.getMarshaller();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("nodeType", nodeType);
    HistoryItem historyItem = new HistoryItem(MovementTypes.STEP_TO_FIRST_NODE_OF_TYPE, parameters);

    try (DBCursor edgeItr = conn.getEdgeDao().iterateEdgesFromNodeToNodeOfType(currentNode, nodeType)) {
      if (!edgeItr.hasNext()) {
        return new GraphCursor(name, this, historyItem);
      }
      DBObject edgeObj = edgeItr.next();
      edgeItr.close();

      Edge edge = m.deserialize(edgeObj, Edge.class);
      historyItem.setVia(edge.getKeys());
      historyItem.setDestination(edge.getTo());
      return new GraphCursor(name, this, historyItem);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    } catch (DbObjectMarshallerException e) {
      throw new GraphCursorException("Failed to deserialise object.", e);
    }
  }

  public GraphCursor stepViaFirstEdgeOfType(GraphConnection conn, String edgeType) throws GraphCursorException {
    DbObjectMarshaller m = conn.getMarshaller();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("edgeType", edgeType);
    HistoryItem historyItem = new HistoryItem(MovementTypes.STEP_VIA_FIRST_EDGE_OF_TYPE, parameters);


    try (DBCursor edgeItr = conn.getEdgeDao().iterateEdgesFromNode(edgeType, currentNode)) {
      if (!edgeItr.hasNext()) {
        return new GraphCursor(name, this, historyItem);
      }
      DBObject edgeObj = edgeItr.next();

      Edge edge = m.deserialize(edgeObj, Edge.class);
      historyItem.setVia(edge.getKeys());
      historyItem.setDestination(edge.getTo());
      return new GraphCursor(name, this, historyItem);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    } catch (DbObjectMarshallerException e) {
      throw new GraphCursorException("Failed to deserialise object.", e);
    }
  }

  /*
   * Cursor queries
   */

  public boolean isAtDeadEnd() {
    return currentNode == null;
  }

  public boolean exists(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getNodeDao().existsByKey(currentNode);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database", e);
    }
  }

  public DBObject resolve(GraphConnection conn)  throws GraphCursorException {
    try {
      DBObject nodeDoc = conn.getNodeDao().getByKey(currentNode);
      return nodeDoc;
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query database", e);
    }
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
      return conn.getEdgeDao().iterateEdgesToNode(currentNode);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }
  public long countIncomingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().countEdgesToNode(currentNode);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }
  public DBCursor iterateOutgoingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().iterateEdgesFromNode(currentNode);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }
  public long countOutgoingEdges(GraphConnection conn) throws GraphCursorException {
    try {
      return conn.getEdgeDao().countEdgesFromNode(currentNode);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }

  /*
   * Edge by type queries
   */
  public DBCursor iterateOutgoingEdgesOfType(GraphConnection conn, String edgeType) throws GraphCursorException {
    try {
      return conn.getEdgeDao().iterateEdgesFromNode(edgeType, currentNode);
    } catch (GraphModelException e) {
      throw new GraphCursorException("Failed to query graph.", e);
    }
  }


  public Iterable<EntityKeys<? extends Node>> iterateOutgoingNodeRefs(GraphConnection conn) throws GraphCursorException {
    try {
      DbObjectMarshaller m = conn.getMarshaller();
      DBCursor edgeResults = conn.getEdgeDao().iterateEdgesFromNode(currentNode);
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

  public Iterable<EdgeDestPair> iterateAndResolveOutgoingEdgeDestPairs(final GraphConnection conn)
      throws GraphCursorException {
    final DbObjectMarshaller m = conn.getMarshaller();
    return new Iterable<EdgeDestPair>() {
      @Override
      public Iterator<EdgeDestPair> iterator() {
        final DBCursor edgeItr;
        try {
          edgeItr = conn.getEdgeDao().iterateEdgesFromNode(currentNode);
        } catch (GraphModelException e) {
          throw new RuntimeException("Failed to query database", e);
        }
        return new Iterator<EdgeDestPair>() {

          @Override
          public boolean hasNext() {
            return edgeItr.hasNext();
          }

          @Override
          public EdgeDestPair next() {
            DBObject edgeObj = edgeItr.next();
            try {
              EntityKeys<? extends Node> destinationKeys = m.deserialize(edgeObj, Edge.class).getTo();
              DBObject destinationNode = conn.getNodeDao().getByKey(destinationKeys);
              if (destinationNode == null) {
                //This is probably a 'hanging' edge - we have the node reference, but no node exists.
                logger.info("Potential hanging edge found: "+destinationKeys);
              }
              EdgeDestPair edgeDestPair = new EdgeDestPair(edgeObj, destinationNode);
              return edgeDestPair;
            } catch (Exception e) {
              throw new RuntimeException("Failed to iterate destination nodes for: "+ currentNode, e);
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
  public EntityKeys<? extends Node> getCurrentNode() {
    return currentNode;
  }

  /**
   * Returns the entire path that the named cursor has taken.
   *
   * @return
   */
  public List<HistoryItem> getHistory() {
    return history;
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
}
