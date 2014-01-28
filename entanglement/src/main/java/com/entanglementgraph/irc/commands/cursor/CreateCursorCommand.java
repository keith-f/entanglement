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
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

/**
 * Creates a new named graph cursor object.
 *
 * @author Keith Flanagan
 */
public class CreateCursorCommand extends AbstractEntanglementCommand {
  private final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();

  private String cursorName;
  private String nodeType;
  private String nodeName;
  private String nodeUid;
  private int maxUids;
  private int maxNames;

  @Override
  public String getDescription() {
    return "Creates a new graph cursor of the specified name. You must choose a start node for this cursor; this may " +
        "EITHER specified by a node UID, OR by a node's type and name properties. You should not normally specify both " +
        "a UID *and* a type+name.";
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("cursor", String.class, "Name of the new cursor"));
    params.add(new OptionalParam("node-type", String.class, "Type of initial node"));
    params.add(new OptionalParam("node-name", String.class, "Name of initial node"));
    params.add(new OptionalParam("node-uid", String.class, "UID of initial node"));

    params.add(new OptionalParam("display-max-uids", Integer.class, "0", "Specifies the maximum number of UIDs to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    params.add(new OptionalParam("display-max-names", Integer.class, "2", "Specifies the maximum number of names to display for graph entities. Reduce this number for readability, increase this number for more detail."));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();
    cursorName = parsedArgs.get("cursor").getStringValue();
    nodeType = parsedArgs.get("node-type").getStringValue();
    nodeName = parsedArgs.get("node-name").getStringValue();
    nodeUid = parsedArgs.get("node-uid").getStringValue();
    maxUids = parsedArgs.get("display-max-uids").parseValueAsInteger();
    maxNames = parsedArgs.get("display-max-names").parseValueAsInteger();
  }

  @Override
  protected void processLine() throws UserException, BotCommandException {

    EntityKeys<? extends Node> nodeLocation = new EntityKeys<>(nodeType, nodeUid, nodeName);


    logger.infoln("Created new graph cursor: %s at location: %s",
        entFormat.formatCursorName(cursorName).toString(),
        entFormat.formatNodeKeysetShort(nodeLocation, maxUids, maxNames));

    try {
      GraphCursor newCursor = new GraphCursor(cursorName, nodeLocation);
      /*
       * Add the cursor to the runtime. It will then be accessible to other distributed processes.
       * The EntanglementRuntime will also receive GraphCursorListener updates.
       */
      entRuntime.getCursorRegistry().addCursor(newCursor);

      String outputText = String.format("New cursor %s created at node: %s",
          entFormat.formatCursorName(cursorName).toString(),
          entFormat.formatNodeKeysetShort(nodeLocation, maxUids, maxNames)).toString();

      logger.println(outputText);
    } catch (Exception e) {
      throw new BotCommandException("WARNING: an Exception occurred while processing.", e);
    }
  }
}
