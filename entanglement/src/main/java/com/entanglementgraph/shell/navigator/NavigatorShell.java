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

package com.entanglementgraph.shell.navigator;

import asg.cliche.Command;
import asg.cliche.Param;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import java.io.IOException;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.util.GraphConnection;

/**
 *
 * @author Keith Flanagan
 */
public class NavigatorShell
{
  private GraphConnection graphConn;


  public static void startSubShell(NavigatorShell navShell) throws IOException
  {
    Shell shell = ShellFactory.createConsoleShell(
            "navigator", "\n\nWelcome to the Entanglement graph navigator!\n", navShell);
    shell.setDisplayTime(true);
    shell.commandLoop();
  }
  
  public NavigatorShell(GraphConnection graphConn, String startNodeUid)
          throws GraphModelException
  {
    Navigator nav = new Navigator(graphConn, startNodeUid);
  }

  @Command(description = "Prints general statistics about the current graph.")
  public void printGraphSummary() {

  }

  @Command(description = "Prints a description of your current location")
  public void describeSample() {
    StringBuilder sb = new StringBuilder();
    sb.append("You are currently at a node of type:  Encode450kReadData");
    sb.append("Node info:\n");
    sb.append("  * type = Encode450kReadData\n");
    sb.append("  * uids = [a99db1eaa7b44327991a9772495bb6bc]\n");
    sb.append("  * names = [wgEncodeHaibMethyl450Gm12878SitesRep1.cg00000622]\n");
    sb.append("Properties:\n");
    sb.append("  * encode_chromosome_id = chr15\n");
    sb.append("  * probe_id = cg00000622\n");
    sb.append("  * methylation_score = 30\n");
    sb.append("Direct outgoing edges:\n");
    sb.append("  * Edge type: ReadingOfProbe TO node type: Probe; uids = [3bc82af963bf4760bbda336948d22ff8]; names = {cg00000622}\n");
    sb.append("Links to active bookmarks:\n");
    sb.append("  * Bookmarked node 'cg00000769' is 3 edge steps away from here.\n");

    System.out.println(sb.toString());
  }

  @Command(description = "Bookmarks the current cursor location. Bookmarks can be used later as graph entry points, or for selecting the boundaries subgraphs.")
  public void bookmark(@Param(name="bookmarkSet") String bookmarkSet, @Param(name="bookmarkName") String bookmarkName) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are currently at a node of type: ").append("id: ");
    System.out.println(sb.toString());
  }
  
  @Command(description = "Moves the graph cursor location to the specified node")
  public void stepToNode(@Param(name="nodeUid") String nodeUid) {
    
  }

  @Command(description = "Moves the graph cursor location to the specified node")
  public void stepToNode(@Param(name="nodeType") String nodeType, @Param(name="nodeName") String nodeName) {

  }

  @Command(description = "Lists the properties of the specified edge")
  public void inspectEdge(@Param(name="edgeUid") String edgeUid) {

  }

  @Command(description = "Exports a siubgraph ")
  public void exportSubgraph(@Param(name="newGraphName") String newGraphName) {

  }
}
