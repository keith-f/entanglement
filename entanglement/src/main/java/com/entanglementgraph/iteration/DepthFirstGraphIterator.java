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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

  public static class TypeBasedModifier {
    private String typeName;
    private NodeBasedModificationAction actionType;

    public TypeBasedModifier(String typeName, NodeBasedModificationAction actionType) {
      this.typeName = typeName;
      this.actionType = actionType;
    }

    public String getTypeName() {
      return typeName;
    }

    public void setTypeName(String typeName) {
      this.typeName = typeName;
    }

    public NodeBasedModificationAction getActionType() {
      return actionType;
    }

    public void setActionType(NodeBasedModificationAction actionType) {
      this.actionType = actionType;
    }
  }

//  public static class IndividualBasedModifier {
//
//  }

  private int batchSize = DEFAULT_BATCH_SIZE;
  private final List<EntityHandlerRule> rules;

  private GraphConnection sourceGraph;
  private GraphConnection destinationGraph;
  private GraphCursor.CursorContext cursorContext;
  private final Map<String, TypeBasedModifier> nodeTypeModifiers;
  private GraphCursor cursor;

  private final List<GraphOperation> graphUpdates;
  private boolean killSwitchActive = false;

  public DepthFirstGraphIterator(GraphConnection sourceGraph, GraphConnection destinationGraph, GraphCursor.CursorContext cursorContext) {
    this.sourceGraph = sourceGraph;
    this.destinationGraph = destinationGraph;
    this.cursorContext = cursorContext;
    rules = new LinkedList<>();
    nodeTypeModifiers = new HashMap<>();
    this.graphUpdates = new LinkedList<>();
  }


  public void addNodeTypeModifier(TypeBasedModifier modifier) {
    nodeTypeModifiers.put(modifier.getTypeName(), modifier);
  }

  /**
   * Iterates over every edge from the current location, performing a depth-first sweep of the source graph.
   *
   * @param start the starting position for hte iteration
   * @throws GraphCursorException
   * @throws DbObjectMarshallerException
   */
  public void execute(GraphCursor start) throws GraphCursorException, DbObjectMarshallerException, RevisionLogException, GraphIteratorException {
    // Begin database transaction
    String txnId = TxnUtils.beginNewTransaction(destinationGraph);

    // Add the start node
    BasicDBObject startObj = start.resolve(sourceGraph);
    graphUpdates.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, startObj));

    // Iterate child nodes recursively
    addChildNodes(null, start);

    // Commit transaction
    TxnUtils.commitTransaction(destinationGraph, txnId);
  }

  private void addChildNodes(GraphCursor previous, GraphCursor current)
      throws GraphCursorException, DbObjectMarshallerException, GraphIteratorException {
    if (killSwitchActive) {
      return;
    }
    for (GraphCursor.NodeEdgeNodeTuple nen : current.iterateAndResolveIncomingEdgeDestPairs(sourceGraph)) {
      if (killSwitchActive) {
        return;
      }

      DBObject remoteNode = nen.getRawSourceNode();
      DBObject edge = nen.getRawEdge();
      DBObject localNode = nen.getRawDestinationNode();

      EntityKeys<Node> remoteNodeId = MongoUtils.parseKeyset(sourceGraph.getMarshaller(), remoteNode, GraphEntityDAO.FIELD_KEYS);
      EntityKeys<Edge> edgeId = MongoUtils.parseKeyset(sourceGraph.getMarshaller(), edge, GraphEntityDAO.FIELD_KEYS);

      // TODO check if we've seen this edge before (exists in DB, or exists in pending edge update). If yes, skip it.

      EntityHandlerRule.NextEdgeIteration nextIterationDecision = processEdge(cursor, nen, false, remoteNodeId, edgeId);

      switch (nextIterationDecision) {
        case CONTINUE_AS_NORMAL:
          // Step the cursor to the next level deep
          GraphCursor nextLevel = cursor.stepToNode(cursorContext, remoteNodeId);
          addChildNodes(current, nextLevel);
          break;
        case TERMINATE_BRANCH:
          // Simply do nothing here. We'll skip over the children of the current remote node
          break;
        case TERMINATE:
          killSwitchActive = true;
          break;

      }
    }

    // We've finished iterating the children of this node means we need to step back to the parent.
    if (previous != null) {
      current.jump(cursorContext, previous.getPosition());
    }
  }

  protected EntityHandlerRule.NextEdgeIteration processEdge(
          GraphCursor currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
          boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId)
        throws GraphIteratorException {

    for (EntityHandlerRule rule : rules) {
      if (rule.ruleMatches(currentPosition, nenTuple, outgoingEdge, nodeId, edgeId)) {
        EntityHandlerRule.HandlerAction result = rule.apply(currentPosition, nenTuple, outgoingEdge, nodeId, edgeId);
        graphUpdates.addAll(result.getOperations());


        if (result.isProcessFurtherRules() &&
            result.getNextIterationBehaviour() != EntityHandlerRule.NextEdgeIteration.CONTINUE_AS_NORMAL) {
          // We can't not 'continue as normal' AND process other rules, since further rules might conflict
          throw new GraphIteratorException(
              "A rule cannot have ProcessFurtherRules="+result.isProcessFurtherRules() +
              " at the same time as NextIterationBehaviour="+EntityHandlerRule.NextEdgeIteration.CONTINUE_AS_NORMAL);
        }
        if (!result.isProcessFurtherRules()) {
          // No further rules need to be processed for this edge iteration.
          return result.getNextIterationBehaviour();
        }
      }
    }
    throw new GraphIteratorException("No valid rule found for processing edge: "+nenTuple +
        " from node: "+currentPosition);
  }

}
