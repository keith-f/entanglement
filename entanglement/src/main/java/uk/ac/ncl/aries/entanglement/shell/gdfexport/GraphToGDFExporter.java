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
 * File created: 15-Nov-2012, 12:29:45
 */

package uk.ac.ncl.aries.entanglement.shell.gdfexport;

import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import uk.ac.ncl.aries.entanglement.shell.gdfexport.GdfWriter;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.graph.data.Edge;
import uk.ac.ncl.aries.entanglement.graph.data.Node;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;

/**
 *
 * @author Keith Flanagan
 */
public class GraphToGDFExporter
{
  private static final Color DEFAULT_COLOR = Color.BLACK;
  
  private final DbObjectMarshaller marshaller;
  private final NodeDAO nodeDao;
  private final EdgeDAO edgeDao;
    
  private final RevisionLog revLog;
  
  private File colorPropsFile;
  private File outputFile;
  
  public GraphToGDFExporter(DbObjectMarshaller marshaller, RevisionLog revLog, NodeDAO nodeDao, EdgeDAO edgeDao)
  {
    this.marshaller = marshaller;
    this.nodeDao = nodeDao;
    this.edgeDao = edgeDao;
    this.revLog = revLog;
  }
  
  public void writeToFile() throws IOException, GraphModelException, RevisionLogException
  {
    Map<String, Color> nodeColorMappings = new HashMap<>();
    if (colorPropsFile != null) {
      nodeColorMappings.putAll(loadColorMappings(colorPropsFile));
    }
    
    BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
    exportToGdf(nodeColorMappings, bw);
  }

  
  public String writeToString() throws IOException, GraphModelException, RevisionLogException
  {
    Map<String, Color> nodeColorMappings = new HashMap<>();
    if (colorPropsFile != null) {
      nodeColorMappings.putAll(loadColorMappings(colorPropsFile));
    }
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos));
    exportToGdf(nodeColorMappings, bw);
    return new String(baos.toByteArray());
  }
  
  private void exportToGdf(Map<String, Color> nodeColorMappings, BufferedWriter bw)
      throws IOException, GraphModelException, RevisionLogException
  {    
    try (GdfWriter writer = new GdfWriter(bw)) {
      writer.writeNodeDef();
      Iterable<Node> nodeItr = new DeserialisingIterable<>(nodeDao.iterateAll(), marshaller, Node.class);
      for (Node node : nodeItr) {
        Color nodeColour = DEFAULT_COLOR;
        if (nodeColorMappings.containsKey(node.getType())) {
          nodeColour = nodeColorMappings.get(node.getType());
        }
        
        writer.writeNode(node, nodeColour);
      }
      
      writer.writeEdgeDef();
      Iterable<Edge> edgeItr = new DeserialisingIterable<>(nodeDao.iterateAll(), marshaller, Edge.class);
      for (Edge edge : edgeItr) {
        writer.writeEdge(edge);
      }
      
      writer.flush();
    }
  }
  
  
  private static Map<String, Color> loadColorMappings(File propFile)
          throws IOException
  {
    FileInputStream is = new FileInputStream(propFile);
    Properties props = new Properties();
    props.load(is);
    is.close();
    
    Map<String, Color> nodeTypeToColour = new HashMap<>();
    for (String nodeType : props.stringPropertyNames()) {
      String colorString = props.getProperty(nodeType);
      Color color;
      
      switch(colorString) {
        case "BLACK" :
          color = Color.BLACK;
          break;
        case "BLUE" :
          color = Color.BLUE;
          break;
        case "CYAN" :
          color = Color.CYAN;
          break;
        case "DARK_GRAY" :
          color = Color.DARK_GRAY;
          break;
        case "GRAY" :
          color = Color.GRAY;
          break;
        case "GREEN" :
          color = Color.GREEN;
          break;
        case "LIGHT_GRAY" :
          color = Color.LIGHT_GRAY;
          break;
        case "MAGENTA" :
          color = Color.MAGENTA;
          break;
        case "ORANGE" :
          color = Color.ORANGE;
          break;
        case "PINK" :
          color = Color.PINK;
          break;
        case "RED" :
          color = Color.RED;
          break;
        case "WHITE" :
          color = Color.WHITE;
          break;
        case "YELLOW" :
          color = Color.YELLOW;
          break;
        default:
          color = DEFAULT_COLOR;
      }
      
      
      nodeTypeToColour.put(nodeType, color);
    }
    return nodeTypeToColour;
  }

  public File getColorPropsFile() {
    return colorPropsFile;
  }

  public void setColorPropsFile(File colorPropsFile) {
    this.colorPropsFile = colorPropsFile;
  }

  public File getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(File outputFile) {
    this.outputFile = outputFile;
  }

  public NodeDAO getNodeDao() {
    return nodeDao;
  }

  public EdgeDAO getEdgeDao() {
    return edgeDao;
  }

  public RevisionLog getRevLog() {
    return revLog;
  }
  
}
