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

package com.entanglementgraph.cli.export;

import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.mongodb.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.cli.*;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.player.GraphCheckoutNamingScheme;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.GraphDAOFactory;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.RevisionLogDirectToMongoDbImpl;
import com.entanglementgraph.revlog.RevisionLogException;

/**
 *
 * @author Keith Flanagan
 */
public class MongoGraphToAscii
{
  private static final ClassLoader classLoader = MongoGraphToAscii.class.getClassLoader();
  private static void printHelpExit(Options options)
  {
    HelpFormatter formatter = new HelpFormatter();
    String cmdSyntax = "mongo-graph-to-ascii.sh";
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
    }
    catch(ParseException e)
    {
      e.printStackTrace();
      printHelpExit(options);
      System.exit(1);
    }

    GraphConnectionFactory connFact = new GraphConnectionFactory(classLoader, mongoHost, mongoDatabaseName);
    GraphConnection conn = connFact.connect(graphName, graphBranch);
    
    exportAscii(conn, new File(outputFilename));
    System.out.println("\n\nDone.");
  }

  
  
  private static void exportAscii(GraphConnection conn, File outputFile)
      throws IOException, GraphModelException, RevisionLogException
  {
    /*
     * File writer
     */
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
    
    writer.append("Nodes:\n");
    for (DBObject node : conn.getNodeDao().iterateAll()) {
      writer.append(node.toString()).append("\n");
    }
    
    writer.append("Edges:\n");
    for (DBObject edge : conn.getEdgeDao().iterateAll()) {
      writer.append(edge.toString()).append("\n");
    }
    
    writer.flush();
    writer.close();
  }
  
  
}