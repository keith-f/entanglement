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

import static com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat.*;

import com.entanglementgraph.cursor.GraphCursor;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.irc.commands.AbstractEntanglementCursorCommand;
import com.scalesinformatics.uibot.*;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.List;

/**
 * A command that sets a new graph cursor to a specified node. The new position can be completely unrelated to the
 * existing position. This command is effectively a 'goto' statement for a cursor.
 *
 * @author Keith Flanagan
 */
public class CursorGoto extends AbstractEntanglementCursorCommand {

  private String nodeType;
  private String nodeName;
  private String nodeUid;
  private int maxUids;
  private int maxNames;

  @Override
  public String getDescription() {
    return "Sets a new position for a graph cursor. You can either specify UID or a type/name pair of the node to jump to.";
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

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    nodeType = parsedArgs.get("node-type").getStringValue();
    nodeName = parsedArgs.get("node-name").getStringValue();
    nodeUid = parsedArgs.get("node-uid").getStringValue();
    maxUids = parsedArgs.get("display-max-uids").parseValueAsInteger();
    maxNames = parsedArgs.get("display-max-names").parseValueAsInteger();


    if ((nodeType==null && nodeName==null) || nodeUid==null) {
      throw new UserException(sender, "You must specify at least a UID or a name.");
    }
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {
    EntityKeys<? extends Node> newLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);

    try {
      GraphCursor previous = cursor;
      GraphCursor current = cursor.jump(cursorContext, newLocation);

      String outputText = String.format("Cursor %s moved %s %s %s %s. Movement type %s",
          entFormat.formatCursorName(cursor.getName()).toString(),
          entFormat.customFormat("from", Colors.BOLD).toString(),
          entFormat.formatNodeKeysetShort(previous.getPosition(), maxUids, maxNames).toString(),
          entFormat.customFormat("to", Colors.BOLD).toString(),
          entFormat.formatNodeKeysetShort(current.getPosition(), maxUids, maxNames).toString(),
          entFormat.formatMovementType(current.getMovementType()).toString());

      logger.println(outputText);
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }

}
