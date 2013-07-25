package com.entanglementgraph.iteration;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;

import java.util.LinkedList;
import java.util.List;

/**
 * User: keith
 * Date: 25/07/13; 16:05
 *
 * @author Keith Flanagan
 */
public interface EntityHandlerRule {

  public static enum NextEdgeIteration {
    CONTINUE_AS_NORMAL,
    TERMINATE_AND_BACKTRACK,
    TERMINATE,
    JUMP_TO_NODE
  }

  public static class HandlerAction {
    private final List<GraphOperation> operations;
    private final NextEdgeIteration nextIterationBehaviour;
    /**
     * Set true if you want further rules to run after this rule has finished, or false if the iterator should
     * move onto the next edge after this rule has completed. Default is false.
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

  public boolean canHandleNodeEdgePair(GraphConnection sourceGraph, GraphConnection destinationGraph,
                                       GraphCursor currentPosition, BasicDBObject edge, BasicDBObject remoteNode);

  /**
   * Called when a <code>DepthFirstGraphIterator</code> decides that this rule implementation should process the
   * next edge/node iteration step.
   *
   * @param sourceGraph the connection to the source graph or view that is being iterated
   * @param destinationGraph the connection to the graph that nodes/edges are being written to
   * @param currentPosition the current position within the source graph
   * @param edge the edge being iterated
   * @param remoteNode the remote node connected to <code>edge</code>.
   * @return an action object that determines: a) what graph operations should be sent to <code>destinationGraph</code>;
   * b) whether to change the iteration behaviour of the caller.
   */
  public HandlerAction apply(GraphConnection sourceGraph, GraphConnection destinationGraph,
                             GraphCursor currentPosition, BasicDBObject edge, BasicDBObject remoteNode);
}
