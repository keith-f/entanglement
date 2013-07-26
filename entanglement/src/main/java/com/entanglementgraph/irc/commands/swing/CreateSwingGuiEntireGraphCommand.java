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

package com.entanglementgraph.irc.commands.swing;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.cursor.GraphCursorException;
import com.entanglementgraph.export.jgraphx.MongoToJGraphExporter;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.revlog.RevisionLogException;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.jgraphx.EntanglementMxGraph;
import com.entanglementgraph.visualisation.jgraphx.GraphFrame;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;
import com.mxgraph.view.mxGraph;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.io.IOException;
import java.util.List;

import static com.entanglementgraph.irc.commands.cursor.CursorCommandUtils.*;

/**
 * This command opens a JFrame and tracks a specified cursor by displaying lists of incoming/outgoing edges of the
 * current node, as well as a JGraphX-rendered image of the immediate neighbourhood.
 *
 * Note; this command will only work where a graphical display is present!
 *
 * @author Keith Flanagan
 */
public class CreateSwingGuiEntireGraphCommand extends AbstractEntanglementCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Displays an entire graph. Use with caution on 'large' graphs.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();

    params.add(new OptionalParam("layout-force-constant", Double.class, "500", "The higher this value is, the more separated the nodes become."));
    params.add(new OptionalParam("layout-min-distance-limit", Double.class, "10", "Unknown..."));
    params.add(new OptionalParam("layout-initial-temp", Double.class, "10", "Unknown..."));

    return params;
  }

  public CreateSwingGuiEntireGraphCommand() {
    super(Requirements.GRAPH_CONN_NEEDED);
  }

  private double layoutForceConstant;
  private double layoutMinDistanceLimit;
  private double layoutInitialTemp;

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    layoutForceConstant = parsedArgs.get("layout-force-constant").parseValueAsDouble();
    layoutMinDistanceLimit = parsedArgs.get("layout-min-distance-limit").parseValueAsDouble();
    layoutInitialTemp = parsedArgs.get("layout-initial-temp").parseValueAsDouble();



    try {
      final GraphFrame frame = displayGraphInNewFrame(graphConn);

      Message msg = new Message(channel);
      return msg;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

  private GraphFrame displayGraphInNewFrame(GraphConnection graphConn) throws JGraphXPopulationException, GraphCursorException, DbObjectMarshallerException, RevisionLogException, GraphModelException, IOException {

    MongoToJGraphExporter exporter = new MongoToJGraphExporter();
    exporter.addEntireGraph(graphConn);
    EntanglementMxGraph mxGraph = exporter.getGraph();

    doFastOrganicLayout(mxGraph);
    GraphFrame frame = new GraphFrame(cursorName, mxGraph);
    frame.getFrame().setVisible(true);
    return frame;
  }


  private void doOrganicLayout(mxGraph graph) {
    // Define layout
    mxOrganicLayout layout = new mxOrganicLayout(graph);


    // layout graph
    layout.execute(graph.getDefaultParent());
  }

  @SuppressWarnings("UnusedDeclaration")
  private void doFastOrganicLayout(mxGraph graph) {
    // Define layout
    mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);

    //Layout with single layout
    // set some properties
    layout.setForceConstant(layoutForceConstant); // the higher, the more separated
    layout.setDisableEdgeStyle(false); // true transforms the edges and makes them direct lines

    layout.setMinDistanceLimit(layoutMinDistanceLimit);
    layout.setInitialTemp(layoutInitialTemp);
    layout.setDisableEdgeStyle(true);

    // layout graph
    layout.execute(graph.getDefaultParent());
  }

  @SuppressWarnings("UnusedDeclaration")
  private void doOrthogonalLayout(mxGraph graph) {
    // Define layout
    mxOrthogonalLayout layout = new mxOrthogonalLayout(graph);

    // layout graph
    layout.execute(graph.getDefaultParent());
  }




}
