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
package com.entanglementgraph.iteration;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.cursor.GraphCursorException;
import com.entanglementgraph.graph.GraphEntityDAO;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.util.TxnUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import java.util.*;
import java.util.logging.Logger;

/**
 * A class that enables you to build graph iterators using an Entanglement <code>GraphCursor</code> to traverse
 * nodes in a source graph (or view) and copy relevant or matching nodes/edges into a destination graph.
 *
 * A <code>GraphCursor</code> is taken as the start point for the query. By default, all edges from this starting
 * point are iterated outwards and nodes/edges that are encountered are transitively added to the result set.
 *
 * It is possible to customise this iteration based on entity type and/or specific individuals in the graph.
 * For example, in the simplest case, iteration can be blocked when particular node type is encountered. The
 * iteration process will then back-track to the next available edge, and continue the iteration from there.
 *
 * More complex result modifications are also possible, such as 'skipping' a particular named node, or node of a given
 * type, resulting in a set of disjoint subgraphs in the result set.
 *
 * User: keith
 * Date: 25/07/13; 14:09
 *
 * @author Keith Flanagan
 */
public class DepthFirstGraphIterator {

  private static final Logger logger = Logger.getLogger(DepthFirstGraphIterator.class.getSimpleName());

  private static final int DEFAULT_BATCH_SIZE = 5000;

  private int batchSize = DEFAULT_BATCH_SIZE;
  private final List<EntityRule> rules;
  private final EntityRule defaultRule;


  private final EntanglementRuntime runtime;
  private final GraphConnection sourceGraph;
  private final GraphConnection destinationGraph;
  private final GraphCursor.CursorContext cursorContext;
  private final boolean addStartNode;

  private final List<GraphOperation> graphUpdates; //In-memory staging for the current block of transaction data
  private boolean killSwitchActive = false;
  private String txnId;
  private int txnPart;

  /*
   * An in-memory cache of the source graph edges that we've seen so far. This is required so that we don't end up
   * iterating over edges that we've already seen. Eventually, this information should be stored in a temporary
   * MongoDB collection because large graphs won't all fit into memory.
   */
  private final Set<String> seenEdgeUids;
  private final Map<String, Set<String>> seenEdgeNames;


  public DepthFirstGraphIterator(GraphConnection sourceGraph, GraphConnection destinationGraph,
                                 EntanglementRuntime runtime,
                                 GraphCursor.CursorContext cursorContext, boolean addStartNode) {
    this.sourceGraph = sourceGraph;
    this.destinationGraph = destinationGraph;
    this.runtime = runtime;
    this.cursorContext = cursorContext;
    this.addStartNode = addStartNode;
    rules = new LinkedList<>();
    this.graphUpdates = new LinkedList<>();


    this.seenEdgeUids = new HashSet<>();
    this.seenEdgeNames = new HashMap<>();
    this.defaultRule = new DefaultRule();
  }

  public void addRule(EntityRule rule) {
    rule.setSourceGraph(sourceGraph);
    rule.setDestinationGraph(destinationGraph);
    rule.setEntanglementRuntime(runtime);
    rule.setCursorContext(cursorContext);
    rules.add(rule);
  }


  /**
   * Iterates over every edge from the current location, performing a depth-first sweep of the source graph.
   *
   * @param start the starting position for hte iteration
   * @throws GraphCursorException
   */
  public void execute(GraphCursor start) throws GraphIteratorException {
    try {
      // Begin database transaction
      txnId = TxnUtils.beginNewTransaction(destinationGraph);
      txnPart = 0;
    } catch(Exception e) {
      throw new GraphIteratorException("Failed to start transaction", e);
    }

    // Inform all rules that we're about to start graph walking
    try {
      for (EntityRule rule : rules) {
        graphUpdates.addAll(rule.iterationStarted(start.getName(), start.getPosition()));
      }
    } catch(Exception e) {
      throw new GraphIteratorException("Failed rule initialisation step", e);
    }

    // Start iterations
    try {
      // Add the start node
      if (addStartNode) {
        BasicDBObject startObj = start.resolve(sourceGraph);
        if (startObj == null) {
          throw new GraphIteratorException("The database object corresponding to the following cursor " +
              "could not be found: "+start.getPosition());
        }
        graphUpdates.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, startObj));
      }

      // Iterate child nodes recursively
      addChildNodes(start.getName(), 0, null);

      // Inform all rules that we've finished graph walking
      try {
        for (EntityRule rule : rules) {
          graphUpdates.addAll(rule.iterationFinished(start.getName()));
        }
      } catch(Exception e) {
        throw new GraphIteratorException("Failed rule tidyup step", e);
      }

      // Write any final updates that haven't been written already
      if (!graphUpdates.isEmpty()) {
        writeUpdates();
      }

      // Commit transaction
      TxnUtils.commitTransaction(destinationGraph, txnId);
    } catch(Exception e) {
      TxnUtils.silentRollbackTransaction(destinationGraph, txnId);
      throw new GraphIteratorException("Failed to export subgraph", e);
    }
  }

  private void addChildNodes(String cursorName, int currentDepth, EntityKeys<? extends Node> previousPosition)
      throws GraphCursorException, GraphIteratorException, DbObjectMarshallerException, RevisionLogException, RuleException {
    if (killSwitchActive) {
      return;
    }
    GraphCursor current = getCurrentCursorPosition(cursorName);
    processEdges(cursorName, currentDepth, previousPosition, current.getPosition(), false,
        current.iterateAndResolveEdgeDestPairs(sourceGraph, false));
//        current.iterateAndResolveIncomingEdgeDestPairs(sourceGraph));
    current = getCurrentCursorPosition(cursorName);
    processEdges(cursorName, currentDepth, previousPosition, current.getPosition(), true,
        current.iterateAndResolveEdgeDestPairs(sourceGraph, true));
//        current.iterateAndResolveOutgoingEdgeDestPairs(sourceGraph));
  }

  private void processEdges(String cursorName, int currentDepth,
                            EntityKeys<? extends Node> previousPosition,
                            EntityKeys<? extends Node> currentPosition,
                            boolean outgoingEdges, Iterable<GraphCursor.NodeEdgeNodeTuple> edges)
      throws RevisionLogException, DbObjectMarshallerException, GraphIteratorException, GraphCursorException, RuleException {
    for (GraphCursor.NodeEdgeNodeTuple nen : edges) {
      if (killSwitchActive) {
        return;
      }
      if (graphUpdates.size() > batchSize) {
        writeUpdates();
      }

      BasicDBObject localNodeDoc = outgoingEdges ? nen.getRawSourceNode() : nen.getRawDestinationNode();
      BasicDBObject edgeDoc = nen.getRawEdge();
      BasicDBObject remoteNodeDoc = outgoingEdges ? nen.getRawDestinationNode() : nen.getRawSourceNode();

      Edge edge = sourceGraph.getMarshaller().deserialize(edgeDoc, Edge.class);
//      EntityKeys<Node> remoteNodeId = MongoUtils.parseKeyset(sourceGraph.getMarshaller(), remoteNode, GraphEntityDAO.FIELD_KEYS);
//      EntityKeys<Edge> edgeId = MongoUtils.parseKeyset(sourceGraph.getMarshaller(), edgeDoc, GraphEntityDAO.FIELD_KEYS);
      EntityKeys<Node> localNodeId = outgoingEdges ? edge.getFrom() : edge.getTo();
      EntityKeys<Node> remoteNodeId = outgoingEdges ? edge.getTo() : edge.getFrom();

      // FIXME for now, we're just caching 'seen' edges in memory. We need to store these in a temporary mongo collection for large graphs
      if (seenEdge(edge.getKeys())) {
        continue;
      }
      cacheEdgeIds(edge.getKeys());

      EntityRule.NextEdgeIteration nextIterationDecision =
          executeRules(cursorName, currentDepth, currentPosition, edge.getKeys(), outgoingEdges, remoteNodeId,
              localNodeDoc, edgeDoc, remoteNodeDoc);

      switch (nextIterationDecision) {
        case CONTINUE_AS_NORMAL:
          // Step the cursor to the next level deep
          GraphCursor current = getCurrentCursorPosition(cursorName);
          GraphCursor nextLevel = current.stepToNode(cursorContext, remoteNodeId);
          addChildNodes(cursorName, currentDepth+1, current.getPosition());
          break;
        case TERMINATE_BRANCH:
          // Simply do nothing here. We'll skip over the children of the current remote node
          break;
        case TERMINATE:
          killSwitchActive = true;
          break;

      }
    }
    // We've finished iterating the children of this node. Step back to the parent (if there is one).
    if (previousPosition != null) {
      GraphCursor current = getCurrentCursorPosition(cursorName);
      current.jump(cursorContext, previousPosition);
    }
  }

  private GraphCursor getCurrentCursorPosition(String cursorName) {
    GraphCursor current = runtime.getCursorRegistry().getCursorCurrentPosition(cursorName);
    if (current == null) {
      throw new RuntimeException("Unable to find a cursor with name: "+cursorName
          +". Did you register it with the cursor registry?");
    }
    return current;
  }

  private void cacheEdgeIds(EntityKeys<Edge> edgeKeys) {
    seenEdgeUids.addAll(edgeKeys.getUids());

    if (edgeKeys.getNames().isEmpty()) {
      return;
    }

    Set<String> names = seenEdgeNames.get(edgeKeys.getType());
    if (names == null) {
      names = new HashSet<>();
      seenEdgeNames.put(edgeKeys.getType(), names);
    }
    names.addAll(edgeKeys.getNames());
  }

  private boolean seenEdge(EntityKeys<Edge> edgeKey) {
    for (String edgeUid : edgeKey.getUids()) {
      if (seenEdgeUids.contains(edgeUid)) {
        return true;
      }
    }

    Set<String> names = seenEdgeNames.get(edgeKey.getType());
    if (names == null) {
      return false;
    }
    for (String edgeName : edgeKey.getNames()) {
      if (names.contains(edgeName)) {
        return true;
      }
    }
    return false;
  }
  
  private void writeUpdates() throws RevisionLogException {
    logger.info("Writing "+graphUpdates.size()+" graph update commands to the destination graph");

    TxnUtils.submitTxnPart(destinationGraph, txnId, txnPart, graphUpdates);
    txnPart++;
    graphUpdates.clear();
  }

  protected EntityRule.NextEdgeIteration executeRules
      (String cursorName, int currentDepth,
       EntityKeys<? extends Node> currentPosition,
       EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
       EntityKeys<? extends Node> remoteNodeId,
       BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
//      (String cursorName, int currentDepth,
//          EntityKeys<? extends Node> currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
//          boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId)
        throws GraphIteratorException, RuleException {

    for (EntityRule rule : rules) {
      if (rule.ruleMatches(cursorName, currentDepth, currentPosition, edgeId, outgoingEdge, remoteNodeId,
          rawLocalNode, rawEdge, rawRemoteNode)) {
        EntityRule.HandlerAction result =
            rule.apply(cursorName, currentDepth, currentPosition, edgeId, outgoingEdge, remoteNodeId,
                rawLocalNode, rawEdge, rawRemoteNode);
        graphUpdates.addAll(result.getOperations());


        if (result.isProcessFurtherRules() &&
            result.getNextIterationBehaviour() != EntityRule.NextEdgeIteration.CONTINUE_AS_NORMAL) {
          // We can't not 'continue as normal' AND process other rules, since further rules might conflict
          throw new GraphIteratorException(
              "A rule cannot have ProcessFurtherRules="+result.isProcessFurtherRules() +
              " at the same time as NextIterationBehaviour="+ EntityRule.NextEdgeIteration.CONTINUE_AS_NORMAL);
        }
        if (!result.isProcessFurtherRules()) {
          // No further rules need to be processed for this edge iteration.
          return result.getNextIterationBehaviour();
        }
      }
    }

    // No rule in the list matched this node. Apply the default rule
    EntityRule.HandlerAction result =
        defaultRule.apply(cursorName, currentDepth, currentPosition, edgeId, outgoingEdge, remoteNodeId,
            rawLocalNode, rawEdge, rawRemoteNode);
    graphUpdates.addAll(result.getOperations());
    return result.getNextIterationBehaviour();
  }

}
