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
package com.entanglementgraph.irc.commands.iteration;

import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalker;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import org.jibble.pircbot.Colors;

import java.util.List;

/**
 *
 * @author Keith Flanagan
 */
public class ListKnownGraphWalkersCommand extends AbstractEntanglementCommand {

  @Override
  public String getDescription() {
    StringBuilder txt = new StringBuilder();
    txt.append("Lists all known "+ CursorBasedGraphWalker.class.getSimpleName()+" implementations. These implementations " +
        "can be used by the "+RunCursorBasedGraphWalkerCommand.class.getSimpleName()+" command.");
    return txt.toString();
  }

  @Override
  public List<Param> getParams() {
    List<Param> params = super.getParams();

    return params;
  }

  public ListKnownGraphWalkersCommand() {
  }


  @Override
  protected void processLine() throws UserException, BotCommandException {
    int count = 0;
    try {
      CursorBasedGraphWalker.Provider provider = new CursorBasedGraphWalker.Provider(entRuntime.getClassLoader());
      for (CursorBasedGraphWalker walker : provider.getLoader()) {
        count++;
        entFormat.bullet(2).append(" ").customFormat(walker.getClass().getSimpleName(), Colors.TEAL);
        logger.println(entFormat.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BotCommandException(
          "Failed to iterate implementations of: "+CursorBasedGraphWalker.class.getSimpleName(), e);
    }

    logger.println("Iteration completed. %s implementations found.", entFormat.format(count).toString());
  }

}
