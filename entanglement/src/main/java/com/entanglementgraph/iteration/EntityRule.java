package com.entanglementgraph.iteration;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Defines a rule that is executed if it matches a node-edge-node tuple as a graph iterator executes.
 * Rules are responsible for defining:
 * <ul>
 *   <li>Which nodes / edges get added (if any), or whether nodes/edges need to be rewritten</li>
 *   <li>Determining the next iteration path: continue unaffected, terminate the current branch recursion, or
 *   terminate all recursion.</li>
 * </ul>
 *
 * User: keith
 * Date: 25/07/13; 16:05
 *
 * @author Keith Flanagan
 */
public interface EntityRule {

  public static enum NextEdgeIteration {
    CONTINUE_AS_NORMAL,
    TERMINATE_BRANCH,
    TERMINATE
  }

  public static class HandlerAction {
    private final List<GraphOperation> operations;
    private final NextEdgeIteration nextIterationBehaviour;
    /**
     * Set true if you want further rules to run for this graph iteration step after this rule has finished, or false
     * if the iterator should move onto the next edge after this rule has completed. Default is false.
     */
    private boolean processFurtherRules;

    public HandlerAction() {
      this.operations = new LinkedList<>();
      processFurtherRules = false;
      nextIterationBehaviour = NextEdgeIteration.CONTINUE_AS_NORMAL;
    }

    public HandlerAction(List<GraphOperation> operations, NextEdgeIteration nextIterationBehaviour) {
      this.operations = operations;
      this.nextIterationBehaviour = nextIterationBehaviour;
      processFurtherRules = false;
    }

    public HandlerAction(NextEdgeIteration nextIterationBehaviour) {
      this.operations = new LinkedList<>();
      this.nextIterationBehaviour = nextIterationBehaviour;
      processFurtherRules = false;
    }

    public List<GraphOperation> getOperations() {
      return operations;
    }

    public NextEdgeIteration getNextIterationBehaviour() {
      return nextIterationBehaviour;
    }

    public boolean isProcessFurtherRules() {
      return processFurtherRules;
    }

    public void setProcessFurtherRules(boolean processFurtherRules) {
      this.processFurtherRules = processFurtherRules;
    }
  }

  public void setSourceGraph(GraphConnection sourceGraph);
  public void setDestinationGraph(GraphConnection destinationGraph);


  /**
   * Returns true if this rule implementation matches the data presented in the graph iterator's current location.
   *
   * @param currentPosition a GraphCursor that represents the current node whose edges are being iterated
   * @param nenTuple a node-edge-node tuple that contains the MongoDB objects representing the source, edge and
   *                 destination node, respectively.
   * @param outgoingEdge true if <code>nenTuple</code> represents an outgoing edge
   * @param nodeId the deserialised EntityKeys of the remote node, provided for convenience.
   * @param edgeId the deserialised EntityKeys of the edge, provided for convenience.
   * @return true if this rule matches and could be applied, or false if this rule doesn't make sense for the current
   * data.
   */
  public boolean ruleMatches(EntityKeys<? extends Node> currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
                             boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId);

  /**
   * Called when a <code>DepthFirstGraphIterator</code> decides that this rule implementation should process the
   * next edge/node iteration step.
   *
   * @param currentPosition a GraphCursor that represents the current node whose edges are being iterated
   * @param nenTuple a node-edge-node tuple that contains the MongoDB objects representing the source, edge and
   *                 destination node, respectively.
   * @param outgoingEdge true if <code>nenTuple</code> represents an outgoing edge
   * @param nodeId the deserialised EntityKeys of the remote node, provided for convenience.
   * @param edgeId the deserialised EntityKeys of the edge, provided for convenience.
   * @return an action object that determines: a) what graph operations should be sent to <code>destinationGraph</code>;
   * b) whether to change the iteration behaviour of the caller.
   */
  public HandlerAction apply(EntityKeys<? extends Node> currentPosition, GraphCursor.NodeEdgeNodeTuple nenTuple,
                             boolean outgoingEdge, EntityKeys<Node> nodeId, EntityKeys<Edge> edgeId);
}
