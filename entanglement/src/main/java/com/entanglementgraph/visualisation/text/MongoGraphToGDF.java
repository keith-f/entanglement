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

package com.entanglementgraph.visualisation.text;

import com.entanglementgraph.shell.gdfexport.GdfWriter;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.graph.mongodb.GraphConnectionFactory;
import com.entanglementgraph.graph.mongodb.GraphConnectionFactoryException;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DeserialisingIterable;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.*;
import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.graph.RevisionLogException;

/**
 *
 * @author Keith Flanagan
 */
public class MongoGraphToGDF
{
  private static final ClassLoader classLoader = MongoGraphToGDF.class.getClassLoader();
  private static final Color DEFAULT_COLOR = Color.BLACK;
  
  private static final DbObjectMarshaller marshaller = ObjectMarshallerFactory.create(MongoGraphToGDF.class.getClassLoader());
  
  private static void printHelpExit(Options options)
  {
    HelpFormatter formatter = new HelpFormatter();
    String cmdSyntax = "mongo-graph-to-gdf.sh";
    String header = "";
    String footer = "";
    int width = 80;
    //formatter.printHelp( "notification.sh", options );
    formatter.printHelp(width, cmdSyntax, header, options, footer);
    System.exit(0);
  }
  
  public static void main(String[] args) throws UnknownHostException, RevisionLogException, IOException, GraphModelException, GraphConnectionFactoryException {
    CommandLineParser parser = new PosixParser();
    Options options = new Options();
    
//    options.addOption("g", "format-gdf", false,
//        "Specifies that the output format is GDF (currently the only option)");
    
    options.addOption("h", "mongo-host", true,
        "The MongoDB server host to connect to.");
    
    options.addOption("d", "mongo-database", true,
        "The name of a MongoDB database to connect to.");
    
    options.addOption("g", "graph-name", true,
        "The name of a graph to use (there may be multiple graphs per MongoDB database).");
    
    options.addOption("b", "graph-branch", true,
        "The name of a graph branch to use (there may be multiple branches per graph).");
    
    options.addOption("c", "color-properties", true,
        "An (optional) '.properties' file that contains node type -> RGB mappings.");
    
    options.addOption("o", "output-file", true, 
        "Specifies a path to a file to use ");

//    options.addOption(OptionBuilder
//        .withLongOpt("tags")
//        .withArgName( "property=value" )
//        .hasArgs(2)
//        .withValueSeparator()
//        .withDescription(
//        "used to tag files when uploading.")
//        .create( "t" ));
    


    if (args.length == 0)
    {
      printHelpExit(options);
    }
    
    String mongoHost = null;
    String mongoDatabaseName = null;
    String graphName = null;
    String graphBranch = null;
    String colorPropsFilename = null;
    String outputFilename = null;
    
    try
    {
      CommandLine line = parser.parse(options, args);
      
      if (line.hasOption("mongo-host")) {
        mongoHost = line.getOptionValue("mongo-host", null);
      } else {
        throw new IllegalArgumentException("You must specify a hostname");
      }
      
      if (line.hasOption("mongo-database")) {
        mongoDatabaseName = line.getOptionValue("mongo-database", null);
      } else {
        throw new IllegalArgumentException("You must specify a database name");
      }
      
      if (line.hasOption("graph-name")) {
        graphName = line.getOptionValue("graph-name", null);
      } else {
        throw new IllegalArgumentException("You must specify a graph name");
      }
      
      if (line.hasOption("graph-branch")) {
        graphBranch = line.getOptionValue("graph-branch", null);
      } else {
        throw new IllegalArgumentException("You must specify a graph branch name");
      }
      
      if (line.hasOption("output-file")) {
        outputFilename = line.getOptionValue("output-file", null);
      } else {
        throw new IllegalArgumentException("You must specify an output filename");
      }
      
      if (line.hasOption("color-properties")) {
        colorPropsFilename = line.getOptionValue("color-properties", null);
      }
    }
    catch(ParseException e)
    {
      e.printStackTrace();
      printHelpExit(options);
      System.exit(1);
    }

    GraphConnectionFactory connFact = new GraphConnectionFactory(classLoader, mongoHost, mongoDatabaseName);
    GraphConnection conn = connFact.connect(graphName, graphBranch);

    Map<String, Color> nodeColorMappings = new HashMap<>();
    if (colorPropsFilename != null) {
      nodeColorMappings.putAll(loadColorMappings(new File(colorPropsFilename)));
    }
    
    exportGdf(conn, nodeColorMappings, new File(outputFilename));
    System.out.println("\n\nDone.");
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
  
  
  private static void exportGdf(GraphConnection conn, Map<String, Color> nodeColorMappings, File outputFile)
      throws IOException, GraphModelException, RevisionLogException
  {
    
    /*
     * GDF file writer
     */
    GdfWriter writer = new GdfWriter(new BufferedWriter(new FileWriter(outputFile)));
    
    writer.writeNodeDef();
    Iterable<Node> nodeItr = new DeserialisingIterable<>(conn.getNodeDao().iterateAll(), marshaller, Node.class);
    for (Node node : nodeItr) {
      Color nodeColour = DEFAULT_COLOR;
      if (nodeColorMappings.containsKey(node.getKeys().getType())) {
        nodeColour = nodeColorMappings.get(node.getKeys().getType());
      }
      
      writer.writeNode(node, nodeColour);
    }
    
    writer.writeEdgeDef();
    Iterable<Edge> edgeItr = new DeserialisingIterable<>(conn.getEdgeDao().iterateAll(), marshaller, Edge.class);
    for (Edge edge : edgeItr) {
      writer.writeEdge(edge);
    }
    
    writer.flush();
    writer.close();
  }
  
  
}
