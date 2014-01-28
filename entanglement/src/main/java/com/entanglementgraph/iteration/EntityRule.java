package com.entanglementgraph.iteration;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.graph.commands.GraphOperation;
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
  public void setEntanglementRuntime(EntanglementRuntime entanglementRuntime);
  public void setCursorContext(GraphCursor.CursorContext cursorContext);

  /**
   * A method that is triggered when the graph iterator is about to start iterating. You should use this method to
   * perform any initialisation that needs to be performed.
   *
   * @param cursorName the name of the <code>GraphCursor</code> that will be used to step over the graph.
   * @param currentPosition the starting node for the cursor.
   */
  public List<GraphOperation> iterationStarted(String cursorName, EntityKeys<? extends Node> currentPosition)
      throws RuleException;

  /**
   * Called when the iterator has reached a new node, but before any edges have been iterated. Your rule implementations
   * can use this method to (for example) increase efficiency by terminating the current branch in cases where it
   * would be pointless to iterate the edges of a particular node.
   *
   * @param cursorName the name of the <code>GraphCursor</code> whose movements led to the current position. The
   *                   name is provided only, which should be sufficient for most purposes. You can use this name to
   *                   obtain the movement history of the cursor if your implementation requires it. Remember that if
   *                   your rule implementation requires to perform its own graph walking, it should do so on its own
   *                   local cursor.
   * @param currentDepth the depth (as measured from the start node) of the node whose edges will be iterated.
   *                     The start node has a depth of '0'. Nodes that are one edge away have a depth of '1', etc.
   * @param currentPosition the EntityKeys of the node whose edges are currently being iterated
   * @return an action object that determines: a) what graph operations should be sent to <code>destinationGraph</code>;
   * b) whether to change the iteration behaviour of the caller.
   * @throws RuleException
   */
  public HandlerAction preEdgeIteration(String cursorName, int currentDepth,
                                        EntityKeys<? extends Node> currentPosition) throws RuleException;

  /**
   * This method should return true if the rule implementation matches the data presented in the graph iterator's
   * current location.
   *
   * Two sets of location object are provided here:
   * <ol>
   *   <li>The three <code>EntityKeys</code> objects are obtained from the edge currently being iterated. The
   *   edge may be either incoming or outgoing from the current node, so the <code>currentPosition</code>, and
   *   <code>remoteNodeId</code> are set accordingly, for convenience.</li>
   *   <li>Additionally, the raw <code>BasicDBObject</code> instances are provided (if they actually exist in the
   *   database), for manual parsing into whatever data bean(s) they represent.</li>
   * </ol>
   *
   * @param cursorName the name of the <code>GraphCursor</code> whose movements led to the current position. The
   *                   name is provided only, which should be sufficient for most purposes. You can use this name to
   *                   obtain the movement history of the cursor if your implementation requires it. Remember that if
   *                   your rule implementation requires to perform its own graph walking, it should do so on its own
   *                   local cursor.
   * @param currentDepth the depth (as measured from the start node) of the node whose edges are being iterated.
   *                     The start node has a depth of '0'. Nodes that are one edge away have a depth of '1', etc.
   * @param currentPosition the EntityKeys of the node whose edges are currently being iterated
   * @param edgeId       the deserialised EntityKeys of the edge, provided for convenience.
   * @param outgoingEdge true if <code>edge</code> represents an outgoing edge wrt <code>currentPosition</code>
   * @param remoteNodeId the deserialised EntityKeys of the remote node, provided for convenience.
   * @param rawLocalNode the complete MongoDB document representing <code>currentPosition</code> node, or NULL if no
   *                     such node document exists (i.e., this edge is 'hanging').
   * @param rawEdge      the complete MongoDB document representing the current edge being iterated.
   * @param rawRemoteNode the complete MongoDB document representing <code>currentPosition</code> node, or NULL if no
   *                      such node document exists (i.e., this edge is 'hanging').
   * @return true if this rule matches and could be applied, or false if this rule doesn't make sense for the current
   * data.
   */
  public boolean ruleMatches(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
                             throws RuleException;

  /**
   * Called when a <code>DepthFirstGraphIterator</code> decides that this rule implementation should process the
   * next edge/node iteration step.
   *
   * Two sets of location object are provided here:
   * <ol>
   *   <li>The three <code>EntityKeys</code> objects are obtained from the edge currently being iterated. The
   *   edge may be either incoming or outgoing from the current node, so the <code>currentPosition</code>, and
   *   <code>remoteNodeId</code> are set accordingly, for convenience.</li>
   *   <li>Additionally, the raw <code>BasicDBObject</code> instances are provided (if they actually exist in the
   *   database), for manual parsing into whatever data bean(s) they represent.</li>
   * </ol>
   *
   * @param cursorName the name of the <code>GraphCursor</code> whose movements led to the current position. The
   *                   name is provided only, which should be sufficient for most purposes. You can use this name to
   *                   obtain the movement history of the cursor if your implementation requires it. Remember that if
   *                   your rule implementation requires to perform its own graph walking, it should do so on its own
   *                   local cursor.
   * @param currentDepth the depth (as measured from the start node) of the node whose edges are being iterated.
   *                     The start node has a depth of '0'. Nodes that are one edge away have a depth of '1', etc.
   * @param currentPosition the EntityKeys of the node whose edges are currently being iterated
   * @param edgeId       the deserialised EntityKeys of the edge, provided for convenience.
   * @param outgoingEdge true if <code>edge</code> represents an outgoing edge wrt <code>currentPosition</code>
   * @param remoteNodeId the deserialised EntityKeys of the remote node, provided for convenience.
   * @param rawLocalNode the complete MongoDB document representing <code>currentPosition</code> node, or NULL if no
   *                     such node document exists (i.e., this edge is 'hanging').
   * @param rawEdge      the complete MongoDB document representing the current edge being iterated.
   * @param rawRemoteNode the complete MongoDB document representing <code>currentPosition</code> node, or NULL if no
   *                      such node document exists (i.e., this edge is 'hanging').
   * @return an action object that determines: a) what graph operations should be sent to <code>destinationGraph</code>;
   * b) whether to change the iteration behaviour of the caller.
   */
  public HandlerAction apply(String cursorName, int currentDepth,
                             EntityKeys<? extends Node> currentPosition,
                             EntityKeys<? extends Edge> edgeId, boolean outgoingEdge,
                             EntityKeys<? extends Node> remoteNodeId,
                             BasicDBObject rawLocalNode, BasicDBObject rawEdge, BasicDBObject rawRemoteNode)
                             throws RuleException;


  /**
   * A method that is triggered when the graph iterator has completed its iteration. You could use this method to
   * perform tidy-up operations that must be performed before the iterator commits the destination graph.
   *
   * @param cursorName the name of the <code>GraphCursor</code> that will be used to step over the graph.
   */
  public List<GraphOperation> iterationFinished(String cursorName)
      throws RuleException;
}
