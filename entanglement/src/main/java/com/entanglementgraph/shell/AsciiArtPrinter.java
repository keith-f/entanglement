/*
 * Copyright 2012 Keith Flanagan
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
 * File created: 07-Dec-2012, 14:45:26
 */

package com.entanglementgraph.shell;

import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.commands.EdgeModification;
import com.entanglementgraph.revlog.commands.NodeModification;
import com.entanglementgraph.revlog.commands.GraphOperation;
import com.entanglementgraph.revlog.data.RevisionItem;
import com.entanglementgraph.revlog.data.RevisionItemContainer;

/**
 *
 * @author Keith Flanagan
 */
public class AsciiArtPrinter
{
  
  public static void printGraphAsAsciiArt(
      String graphName, String graphBranchName, RevisionLog revLog)
  {
    System.out.println(graphName+"/"+graphBranchName);
    int indent = 1;
    for (RevisionItemContainer revContainer : revLog.iterateCommittedRevisionsForGraph(graphName, graphBranchName))
    {
      StringBuilder sb = new StringBuilder(_generateIndent(indent));
      for (RevisionItem item : revContainer.getItems()) {
        String commandType = item.getOp().getClass().getSimpleName();
        sb.append(commandType)
//            .append(": rev: ").append(item.getRevisionId())
            .append(", ");

        GraphOperation op = item.getOp();

        if (op instanceof NodeModification) {
          sb.append(_printItem((NodeModification) item.getOp()));
        } else if (op instanceof EdgeModification) {
          sb.append(_printItem((EdgeModification) item.getOp()));
        } else {
          sb.append("Unsupported type: ").append(item.getType());
        }
      }
      
      //sb.append("\n");
      System.out.println(sb.toString());
    }
    

    
    System.out.println("\n\nDone.");
  }
  
  private static String _generateIndent(int indentLevel)
  {
    int spacesPerIndent = 2;
    String spaceStr = new String(
        new char[indentLevel * spacesPerIndent]).replace('\0', ' ');
    return spaceStr;
  }
  
  private static String _printItem(NodeModification node)
  {
    StringBuilder sb = new StringBuilder();
//    sb.append(node.getType()).append(", ");
//    sb.append(node.getUid());
    return sb.toString();
  }
  
  private static String _printItem(EdgeModification edge)
  {
    StringBuilder sb = new StringBuilder();
//    sb.append(edge.getFromUid());
//    sb.append(" --- ").append(edge.getType()).append(" ---> ");
//    sb.append(edge.getToUid());
//    
//    sb.append(" (").append(edge.getUid()).append(")");
    return sb.toString();
  }  
}
