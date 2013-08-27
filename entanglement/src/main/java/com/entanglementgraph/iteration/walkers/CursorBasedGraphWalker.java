package com.entanglementgraph.iteration.walkers;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.iteration.DepthFirstGraphIterator;
import com.entanglementgraph.util.GraphConnection;

import java.util.ServiceLoader;

/**
 * A Service Provider Interface for graph walkers. If your graph walker/iterator implementations use this interface,
 * then you can make use of several generic Entanglement IRC commands that deal with graph iteration.
 *
 * User: keith
 * Date: 19/08/13; 14:53
 *
 * @author Keith Flanagan
 */
public interface CursorBasedGraphWalker {

  public static class Provider {
    private final ServiceLoader<CursorBasedGraphWalker> loader;

    public Provider(ClassLoader cl)
    {
      loader = ServiceLoader.load(CursorBasedGraphWalker.class, cl);
    }

    public ServiceLoader<CursorBasedGraphWalker> getLoader() {
      return loader;
    }

    public CursorBasedGraphWalker getForName(String name)
        throws GraphWalkerException
    {
      for (CursorBasedGraphWalker impl : loader) {
        String className = impl.getClass().getName();
        if (className.equals(name) || className.endsWith(name)) {
          return impl;
        }
      }

      throw new GraphWalkerException(
          "No item player implementation could be found for log item type: "+name);
    }
  }


  public void setRuntime(EntanglementRuntime runtime);
  public void setCursorContext(GraphCursor.CursorContext cursorContext);
//  public void setGraphIterator(DepthFirstGraphIterator graphIterator);
  public void setSourceGraph(GraphConnection sourceGraph);
  public void setDestinationGraph(GraphConnection destinationGraph);
  public void setStartPosition(GraphCursor startPosition);

  public void initialise() throws GraphWalkerException;
  public void execute() throws GraphWalkerException;
}
