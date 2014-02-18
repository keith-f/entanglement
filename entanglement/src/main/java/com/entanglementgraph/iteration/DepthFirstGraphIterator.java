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
import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.graph.RevisionLogException;
import com.entanglementgraph.graph.commands.GraphOperation;
import com.entanglementgraph.util.*;
import com.mongodb.BasicDBObject;
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

  private final List<EntityRule> highPriorityRules;
  private final List<EntityRule> normalRules;
  private final List<EntityRule> lowPriorityRules;

  private final List<EntityRule> allRules; // The combined list of high priority, normal, and low priority rules
//  private final EntityRule defaultRule;


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
//  private final Set<String> seenEdgeUids;
//  private final Map<String, Set<String>> seenEdgeNames;


  public DepthFirstGraphIterator(GraphConnection sourceGraph, GraphConnection destinationGraph,
                                 EntanglementRuntime runtime,
                                 GraphCursor.CursorContext cursorContext, boolean addStartNode) {
    this.sourceGraph = sourceGraph;
    this.destinationGraph = destinationGraph;
    this.runtime = runtime;
    this.cursorContext = cursorContext;
    this.addStartNode = addStartNode;
    highPriorityRules = new LinkedList<>();
    normalRules = new LinkedList<>();
    lowPriorityRules = new LinkedList<>();
    allRules = new LinkedList<>();
    this.graphUpdates = new LinkedList<>();


//    this.seenEdgeUids = new HashSet<>();
//    this.seenEdgeNames = new HashMap<>();


    // Add default rules
    addHighPriorityRule(new StopIfSeenEdgeOrDestNodeRule());
    addLowPriorityRule(new AddByDefaultRule());
  }

  public void addHighPriorityRule(EntityRule rule) {
    rule.setSourceGraph(sourceGraph);
    rule.setDestinationGraph(destinationGraph);
    rule.setEntanglementRuntime(runtime);
    rule.setCursorContext(cursorContext);
    highPriorityRules.add(rule);
  }

  public void addRule(EntityRule rule) {
    rule.setSourceGraph(sourceGraph);
    rule.setDestinationGraph(destinationGraph);
    rule.setEntanglementRuntime(runtime);
    rule.setCursorContext(cursorContext);
    normalRules.add(rule);
  }

  public void addLowPriorityRule(EntityRule rule) {
    rule.setSourceGraph(sourceGraph);
    rule.setDestinationGraph(destinationGraph);
    rule.setEntanglementRuntime(runtime);
    rule.setCursorContext(cursorContext);
    lowPriorityRules.add(rule);
  }


  /**
   * Iterates over every edge from the current location, performing a depth-first sweep of the source graph.
   *
   * @param start the starting position for hte iteration
   * @throws GraphCursorException
   */
  public void execute(GraphCursor start) throws GraphIteratorException {
    // Re-create 'all rules', by adding the rules from the other datasets
    allRules.clear();
    allRules.addAll(highPriorityRules);
    allRules.addAll(normalRules);
    allRules.addAll(lowPriorityRules);

    // Inform all rules that we're about to start graph walking
    try {
      for (EntityRule rule : allRules) {
        graphUpdates.addAll(rule.iterationStarted(start.getName(), start.getPosition()));
      }
    } catch(Exception e) {
      throw new GraphIteratorException("Failed rule initialisation step", e);
    }

    try {
      // Begin database transaction
      txnId = TxnUtils.beginNewTransaction(destinationGraph);
      txnPart = 0;
    } catch(Exception e) {
      throw new GraphIteratorException("Failed to start transaction", e);
    }

    // Start iterations
    try {
      // Add the start node
      if (addStartNode) {
        //TODO reimplement
//        BasicDBObject startObj = start.resolve(sourceGraph);
//        graphUpdates.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, startObj));
      }

      // Iterate child nodes recursively
      addChildNodes(start.getName(), 0);

      // Inform all rules that we've finished graph walking
      try {
        for (EntityRule rule : allRules) {
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

  private void addChildNodes(String cursorName, int currentDepth)
      throws GraphCursorException, GraphIteratorException, DbObjectMarshallerException, RevisionLogException, RuleException {
    if (killSwitchActive) {
      return;
    }
    GraphCursor current = getCurrentCursorPosition(cursorName);
    logger.info(String.format("We are here: %s, with an edge depth of: %d. Running rule 'pre edge iteration' checks.",
        current.getPosition(), currentDepth));
    EntityRule.NextEdgeIteration preIterationInstruction =
        executePreIterationRules(cursorName, currentDepth, current.getPosition());
    if (preIterationInstruction != EntityRule.NextEdgeIteration.CONTINUE_AS_NORMAL) {
      return;
    }

    logger.info(String.format("We are here: %s, with an edge depth of: %d. Going to iterate edges from this node.",
        current.getPosition(), currentDepth));
    processEdges(cursorName, currentDepth, current.getPosition(), false,
        current.iterateAndResolveEdgeDestPairs(sourceGraph, false));
    current = getCurrentCursorPosition(cursorName);
    processEdges(cursorName, currentDepth, current.getPosition(), true,
        current.iterateAndResolveEdgeDestPairs(sourceGraph, true));
  }

  private void processEdges(String cursorName, int currentDepth,
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

      //TODO reimplement this
//      Edge edge = sourceGraph.getMarshaller().deserialize(edgeDoc, Edge.class);
//      EntityKeys<Node> localNodeId = outgoingEdges ? edge.getFrom() : edge.getTo();
//      EntityKeys<Node> remoteNodeId = outgoingEdges ? edge.getTo() : edge.getFrom();
      EntityKeys remoteNodeId = null;

      // FIXME for now, we're just caching 'seen' edges in memory. We need to store these in a temporary mongo collection for large graphs
//      logger.info("Seen edge? "+seenEdge(edge.getKeys())+" (current depth: "+currentDepth+"): "+edge
//          +"\n:  * Known IDs: "+seenEdgeUids
//          +"\n:  * Known names: "+seenEdgeNames.get(edge.getKeys().getType()));
//      if (seenEdge(edge.getKeys())) {
//        continue;
//      }
//      cacheEdgeIds(edge.getKeys());
//
//      EntityRule.NextEdgeIteration nextIterationDecision =
//          executeRules(cursorName, currentDepth, currentPosition, edge.getKeys(), outgoingEdges, remoteNodeId,
//              localNodeDoc, edgeDoc, remoteNodeDoc);
//
//      switch (nextIterationDecision) {
//        case CONTINUE_AS_NORMAL:
//          // Step the cursor from 'here' to the next level down
//          //TODO reimplement this
//          GraphCursor here = getCurrentCursorPosition(cursorName);
//          GraphCursor child = here.stepToNode(cursorContext, remoteNodeId); // Child ref not currently needed
//          addChildNodes(cursorName, currentDepth+1);
//          // After we've done with this child, jump back to its parent (i.e., 'here')
//          getCurrentCursorPosition(cursorName).jump(cursorContext, here.getPosition());
//          break;
//        case TERMINATE_BRANCH:
//          // Simply do nothing here. We'll skip over the children of the remote node
//          break;
//        case TERMINATE:
//          killSwitchActive = true;
//          break;
//      }
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
  
  private void writeUpdates() throws RevisionLogException {
    logger.info(String.format("Writing %d graph update commands to the destination graph", graphUpdates.size()));

    TxnUtils.submitTxnPart(destinationGraph, txnId, txnPart, graphUpdates);
    txnPart++;
    graphUpdates.clear();
  }

  protected EntityRule.NextEdgeIteration executePreIterationRules(
      String cursorName, int currentEdgeDepth, EntityKeys<? extends Node> currentPosition) throws RuleException, GraphIteratorException {
    // If there are no rules, or no rules have special requirements, then go ahead with edge iteration.
    EntityRule.NextEdgeIteration instruction = EntityRule.NextEdgeIteration.CONTINUE_AS_NORMAL;

    // Override the default if a rule requires it
    for (EntityRule rule : allRules) {
      EntityRule.HandlerAction result = rule.preEdgeIteration(cursorName, currentEdgeDepth, currentPosition);
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
        instruction = result.getNextIterationBehaviour();
      }
    }

    switch (instruction) {
      case CONTINUE_AS_NORMAL:
      case TERMINATE_BRANCH:
        // Simply do nothing for these cases.
        break;
      case TERMINATE:
        // We need to flip the kill switch if a rule told us to stop all iteration.
        killSwitchActive = true;
        break;
    }

    return instruction;
  }

  protected EntityRule.NextEdgeIteration executeRules
      (String cursorName, int currentDepth,
       EntityKeys<? extends Node> currentPosition,
       EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
       EntityKeys<? extends Node> remoteNodeId,
       BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
        throws GraphIteratorException, RuleException {

    for (EntityRule rule : allRules) {
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
    throw new GraphIteratorException("No rule matched the current edge. Depth: "+currentDepth
        + ". Current location: "+currentPosition
        + ". Edge: " + edgeId
        + ". Remote node: "+remoteNodeId
        + ".\nPerhaps you forgot to include a default 'catch all' rule?");
  }

}
