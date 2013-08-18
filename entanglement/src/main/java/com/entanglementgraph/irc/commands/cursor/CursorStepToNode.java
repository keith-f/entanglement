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

package com.entanglementgraph.irc.commands.cursor;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.List;

import static com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat.*;

/**
 * Steps the graph cursor to a directly connected node.
 *
 * @author Keith Flanagan
 */
public class CursorStepToNode extends AbstractEntanglementCommand<EntanglementRuntime> {


  @Override
  public String getDescription() {
    return "Steps the graph cursor to a directly connected node. The node may be at the end of either an incoming or " +
        "outgoing edge. You may specify a node UID, or type/name, or even just a type or a name as long as the they " +
        "are not ambiguous.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new OptionalParam("node-type", String.class, "The type name of the node to jump to."));
    params.add(new OptionalParam("node-name", String.class, "The unique name of the node to jump to."));
    params.add(new OptionalParam("node-uid", String.class, "The UID of the node to jump to."));

    params.add(new OptionalParam("display-max-uids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("display-max-names", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    return params;
  }

  public CursorStepToNode() {
    super(AbstractEntanglementCommand.Requirements.GRAPH_CONN_NEEDED, AbstractEntanglementCommand.Requirements.CURSOR_NEEDED);
  }

  @Override
  protected Message _processLine() throws UserException, BotCommandException {
    String nodeType = parsedArgs.get("node-type").getStringValue();
    String nodeName = parsedArgs.get("node-name").getStringValue();
    String nodeUid = parsedArgs.get("node-uid").getStringValue();
    int maxUids = parsedArgs.get("display-max-uids").parseValueAsInteger();
    int maxNames = parsedArgs.get("display-max-names").parseValueAsInteger();

    if (nodeType==null && nodeName==null && nodeUid==null) {
      throw new UserException(sender, "You must specify at least a UID or a name.");
    }

    EntityKeys<? extends Node> newLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);

    try {
      GraphCursor previous = cursor;
      GraphCursor current = cursor.stepToNode(cursorContext, newLocation);

      String outputText = String.format("Cursor %s moved %s %s %s %s. Movement type %s",
          entFormat.formatCursorName(cursor.getName()),
          entFormat.customFormat("from", Colors.BOLD),
          entFormat.formatNodeKeysetShort(previous.getPosition(), maxUids, maxNames),
          entFormat.customFormat("to", Colors.BOLD),
          entFormat.formatNodeKeysetShort(current.getPosition(), maxUids, maxNames),
          entFormat.formatMovementType(current.getMovementType()));

      Message result = new Message(channel);
      result.println(outputText);
      return result;
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
