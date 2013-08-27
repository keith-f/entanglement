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
package com.entanglementgraph.iteration.walkers;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalker;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.visualisation.jung.JungGraphFrame;
import com.entanglementgraph.visualisation.jung.MongoToJungGraphExporter;
import com.entanglementgraph.visualisation.jung.TrackingVisualisation;
import com.entanglementgraph.visualisation.jung.renderers.CustomRendererRegistry;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.hazelcast.concurrent.ThreadUtils;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.util.UidGenerator;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.PickableEdgePaintTransformer;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.util.Animator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * A runnable for executing a graph walker. This class takes a <code>CursorBasedGraphWalker</code> and manages
 * its execution with a provided Entanglement environment. Data is taken from a source graph, and written to
 * a destination graph. This class also manages the creation and removal of a temporary graph cursor to be used
 * for the iteration.
 *
 * This class is convenient for situations where you need to run multiple graph walkers in parallel. The extension of
 * the <code>Runnable</code> interface means that a configured <code>CursorBasedGraphWalkerRunnable</code> can be added
 * to a thread pool for scheduled execution.
 *
 * User: keith
 * Date: 19/08/13; 15:38
 *
 * @author Keith Flanagan
 */
public class CursorBasedGraphWalkerRunnable implements Runnable {
//  private static final Logger logger = Logger.getLogger(CursorBasedGraphWalkerRunnable.class.getName());

  private final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();
  
  private final BotLogger logger;

  protected final EntanglementRuntime runtime;
  protected final GraphConnection sourceGraph;
  protected final GraphConnection destinationGraph;
  protected final CursorBasedGraphWalker walker;
  protected final EntityKeys<? extends Node> startPosition;

  public CursorBasedGraphWalkerRunnable(
      BotLogger logger, EntanglementRuntime runtime, GraphConnection sourceGraph, GraphConnection destinationGraph,
      CursorBasedGraphWalker walker, EntityKeys<? extends Node> startPosition) {
    this.logger = logger;
    this.runtime = runtime;
    this.sourceGraph = sourceGraph;
    this.destinationGraph = destinationGraph;
    this.walker = walker;
    this.startPosition = startPosition;
  }

  @Override
  public void run() {
    try {
      //Initialise a temporary graph cursor.
      GraphCursor tmpCursor = new GraphCursor(UidGenerator.generateUid(), startPosition);
      runtime.getCursorRegistry().addCursor(tmpCursor);

      //Configure walker
      walker.setRuntime(runtime);
      GraphCursor.CursorContext cursorContext = new GraphCursor.CursorContext(sourceGraph, runtime.getHzInstance());
      walker.setCursorContext(cursorContext);
      walker.setSourceGraph(sourceGraph);
      walker.setDestinationGraph(destinationGraph);
      walker.setStartPosition(tmpCursor);

      // Go for a walk
      logger.println("Exporting a subgraph from %s to %s", sourceGraph.getGraphName(), destinationGraph.getGraphName());
      walker.initialise();
      walker.execute();
      logger.println("Iteration of %s by %s completed. Destination graph is: %s/%s/%s",
          sourceGraph.getGraphName(), walker.getClass().getName(),
          destinationGraph.getPoolName(), destinationGraph.getDatabaseName(), destinationGraph.getGraphName());

      //Remove temporary graph cursor
      runtime.getCursorRegistry().removeCursor(tmpCursor);
      // At this point, we've run the walker. The destination graph should contain the required nodes/edges.
    } catch (Exception e) {
      e.printStackTrace();
      logger.getBot().printException(logger.getChannel(), "Graph walker failure", e);
    }

  }

}
