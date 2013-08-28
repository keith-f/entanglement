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

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.irc.commands.AbstractEntanglementCommand;
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalker;
import com.entanglementgraph.iteration.walkers.CursorBasedGraphWalkerRunnable;
import com.entanglementgraph.iteration.walkers.GraphWalkerException;
import com.entanglementgraph.specialistnodes.CategoryChartNode;
import com.entanglementgraph.specialistnodes.XYChartNode;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.visualisation.jung.JungGraphFrame;
import com.entanglementgraph.visualisation.jung.MongoToJungGraphExporter;
import com.entanglementgraph.visualisation.jung.TrackingVisualisation;
import com.entanglementgraph.visualisation.jung.imageexport.ImageUtil;
import com.entanglementgraph.visualisation.jung.imageexport.JungToBufferedImage;
import com.entanglementgraph.visualisation.jung.imageexport.OutputFileUtil;
import com.entanglementgraph.visualisation.jung.renderers.CategoryDatasetChartRenderer;
import com.entanglementgraph.visualisation.jung.renderers.CustomRendererRegistry;
import com.entanglementgraph.visualisation.jung.renderers.XYDatasetChartRenderer;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.uibot.Message;
import com.scalesinformatics.uibot.OptionalParam;
import com.scalesinformatics.uibot.Param;
import com.scalesinformatics.uibot.RequiredParam;
import com.scalesinformatics.uibot.commands.BotCommandException;
import com.scalesinformatics.uibot.commands.UserException;
import edu.uci.ics.jung.graph.Graph;
import org.jibble.pircbot.Colors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;

/**
 *
 * @author Keith Flanagan
 */
public class ListKnownGraphWalkersCommand extends AbstractEntanglementCommand<EntanglementRuntime> {


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
  protected Message _processLine() throws UserException, BotCommandException {

    int count = 0;
    try {
      CursorBasedGraphWalker.Provider provider = new CursorBasedGraphWalker.Provider(state.getUserObject().getClassLoader());
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

    Message msg = new Message(channel);
    msg.println("Iteration completed. %s implementations found.", entFormat.format(count).toString());
    return msg;
  }

}
