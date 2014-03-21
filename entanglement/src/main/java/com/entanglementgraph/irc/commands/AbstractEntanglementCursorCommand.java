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

package com.entanglementgraph.irc.commands;

import com.entanglementgraph.cursor.GraphCursor;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;

import java.util.List;

/**
 * An Entanglement command implementation that requires that a graph cursor name is specified.
 * Extends <code>AbstractEntanglementGraphCommand</code> because graph and Hazelcast objects are required too.
 *
 * @author Keith Flanagan
 */
abstract public class AbstractEntanglementCursorCommand extends AbstractEntanglementGraphCommand {
  protected String cursorName;
  protected GraphCursor cursor;
//  protected GraphCursor.CursorContext cursorContext;

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();
    params.add(new RequiredParam("cursor", String.class, "Name of the graph cursor to use"));
    return params;
  }

  @Override
  protected void preProcessLine() throws UserException, BotCommandException {
    super.preProcessLine();

    cursorName = parsedArgs.get("cursor").getStringValue();
    cursor = new GraphCursor(logger, entRuntime.getHzInstance(), graphConn, cursorName);
  }

}
