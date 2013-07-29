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

  public static enum NodeBasedModificationAction {
    /**
     * When triggered by a particular node or type of node, causes further cursor iterations to halt. The node that
     * caused the iterations to cease is included in the final result set.
     */
    BLOCK_ITERATION_AND_INCLUDE,
    /**
     * When triggered by a particular node or type of node, causes further cursor iterations to halt. The node that
     * caused the iterations to cease is NOT included in the final result set.
     */
    BLOCK_ITERATION_AND_EXCLUDE,
    /**
     * When triggered by a particular node or type of node, iterations continue as normal, but this particular node
     * is NOT included in the result set. This can be useful when you're iterating over an integrated dataset joined
     * by one or more 'hub' nodes and you want the result set to contain a discrete set of graphs to contain everything
     * except the hub nodes.
     */
    CONTINUE_AND_EXCLUDE,

    /**
     * Similar to <code>CONTINUE_AND_EXCLUDE</code>, except the two edges that would otherwise link to the common
     * hub node are merged together and given a new type and identity.
     */
    CONTINUE_AND_EXCLUDE_BUT_JOIN_CHILD_NODES
  }

  private int batchSize = DEFAULT_BATCH_SIZE;
  private final List<EntityRule> rules;
  private final EntityRule defaultRule;


  private final EntanglementRuntime runtime;
  private final GraphConnection sourceGraph;
  private final GraphConnection destinationGraph;
  private final GraphCursor.CursorContext cursorContext;

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
                                 GraphCursor.CursorContext cursorContext) {
    this.sourceGraph = sourceGraph;
    this.destinationGraph = destinationGraph;
    this.runtime = runtime;
    this.cursorContext = cursorContext;
    rules = new LinkedList<>();
    this.graphUpdates = new LinkedList<>();


    this.seenEdgeUids = new HashSet<>();
    this.seenEdgeNames = new HashMap<>();
    this.defaultRule = new DefaultRule();
  }

  public void addRule(EntityRule rule) {
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

    try {
      // Add the start node
      BasicDBObject startObj = start.resolve(sourceGraph);
      graphUpdates.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, startObj));

      // Iterate child nodes recursively
      addChildNodes(start.getName(), null);

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

  private void addChildNodes(String cursorName, EntityKeys<? extends Node> previousPosition)
      throws GraphCursorException, GraphIteratorException, DbObjectMarshallerException, RevisionLogException {
    if (killSwitchActive) {
      return;
    }
    GraphCursor current = getCurrentCursorPosition(cursorName);
    processEdges(cursorName, previousPosition, current.getPosition(), false,
        current.iterateAndResolveIncomingEdgeDestPairs(sourceGraph));
    current = getCurrentCursorPosition(cursorName);
    processEdges(cursorName, previousPosition, current.getPosition(), true,
        current.iterateAndResolveOutgoingEdgeDestPairs(sourceGraph));

//    // We've finished iterating the children of this node. Step back to the parent (if there is one).
//    if (previous != null) {
//      current.jump(cursorContext, previous.getPosition());
//    }
  }

  private void processEdges(String cursorName,
                            EntityKeys<? extends Node> previousPosition, EntityKeys<? extends Node> currentPosition,
                            boolean outgoingEdges, Iterable<GraphCursor.NodeEdgeNodeTuple> edges)
      throws RevisionLogException, DbObjectMarshallerException, GraphIteratorException, GraphCursorException {
    for (GraphCursor.NodeEdgeNodeTuple nen : edges) {
      if (killSwitchActive) {
        return;
      }
      if (graphUpdates.size() > DEFAULT_BATCH_SIZE) {
        writeUpdates();
      }

      BasicDBObject remoteNode = outgoingEdges ? nen.getRawDestinationNode() : nen.getRawSourceNode();
      BasicDBObject edge = nen.getRawEdge();

      EntityKeys<Node> remoteNodeId = MongoUtils.parseKeyset(sourceGraph.getMarshaller(), remoteNode, GraphEntityDAO.FIELD_KEYS);
      EntityKeys<Edge> edgeId = MongoUtils.parseKeyset(sourceGraph.getMarshaller(), edge, GraphEntityDAO.FIELD_KEYS);

      // FIXME for now, we're just caching 'seen' edges in memory. We need to store these in a temporary mongo collection for large graphs
      if (seenEdge(edgeId)) {
        continue;
      }
      cacheEdgeIds(edgeId);

      EntityRule.NextEdgeIteration nextIterationDecision = executeRules(currentPosition, nen, outgoingEdges, remoteNodeId, edgeId);

      switch (nextIterationDecision) {
        case CONTINUE_AS_NORMAL:
          // Step the cursor to the next level deep
          GraphCursor current = getCurrentCursorPosition(cursorName);
          GraphCursor nextLevel = current.stepToNode(cursorContext, remoteNodeId);
          addChildNodes(cursorName, current.getPosition());
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
    logger.info("Writing "+graphUpdates+" graph update commands to the destination graph");

    TxnUtils.submitTxnPart(destinationGraph, txnId, txnPart, graphUpdates);
    txnPart++;
    graphUpdates.clear();
  }

  protected EntityRule.NextEdgeIteration executeRules(
          EntityKeys<? extends Node> currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
          boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId)
        throws GraphIteratorException {

    for (EntityRule rule : rules) {
      if (rule.ruleMatches(currentPosition, nenTuple, outgoingEdge, nodeId, edgeId)) {
        EntityRule.HandlerAction result = rule.apply(currentPosition, nenTuple, outgoingEdge, nodeId, edgeId);
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
    EntityRule.HandlerAction result = defaultRule.apply(currentPosition, nenTuple, outgoingEdge, nodeId, edgeId);
    graphUpdates.addAll(result.getOperations());
    return result.getNextIterationBehaviour();
  }

}
