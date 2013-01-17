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

package uk.ac.ncl.aries.entanglement.shell.navigator;

import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import java.io.IOException;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;

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
}
