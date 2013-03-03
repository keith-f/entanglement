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
import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import java.io.IOException;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.revlog.RevisionLog;

/**
 *
 * @author Keith Flanagan
 */
public class NavigatorShell
{
  public static void startSubShell(NavigatorShell navShell) throws IOException
  {
    Shell shell = ShellFactory.createConsoleShell(
            "navigator", "\n\nWelcome to the Entanglement graph navigator!\n", navShell);
    shell.setDisplayTime(true);
    shell.commandLoop();
  }
  
  public NavigatorShell(RevisionLog revLog, NodeDAO nodeDao, EdgeDAO edgeDao, String startNodeUid)
          throws GraphModelException
  {
    Navigator nav = new Navigator(nodeDao, edgeDao, startNodeUid);
  }
  
  @Command()
  public void describe()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("You are currently at a node of type: ").append("id: ");
    System.out.println(sb.toString());
  }
  
  @Command
  public void step()
  {
    
  }
}
