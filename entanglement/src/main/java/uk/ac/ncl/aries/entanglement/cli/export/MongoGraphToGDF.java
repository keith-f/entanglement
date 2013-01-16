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

package uk.ac.ncl.aries.entanglement.cli.export;

import uk.ac.ncl.aries.entanglement.shell.gdfexport.GdfWriter;
import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.cli.*;
import uk.ac.ncl.aries.entanglement.ObjectMarshallerFactory;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.GraphCheckoutNamingScheme;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphDAOFactory;
import uk.ac.ncl.aries.entanglement.graph.data.Edge;
import uk.ac.ncl.aries.entanglement.graph.data.Node;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogDirectToMongoDbImpl;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateEdge;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNode;
import uk.ac.ncl.aries.entanglement.revlog.commands.GraphOperation;

/**
 *
 * @author Keith Flanagan
 */
public class MongoGraphToGDF
{
  private static final Color DEFAULT_COLOR = Color.BLACK;
  
  private static final DbObjectMarshaller marshaller = ObjectMarshallerFactory.create();
  
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
  
  public static void main(String[] args) throws UnknownHostException, RevisionLogException, IOException, LogPlayerException
  {
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

    
    
    Mongo m = new Mongo();
//    Mongo m = new Mongo( "localhost" );
    // or
//    Mongo m = new Mongo( "localhost" , 27017 );
    // or, to connect to a replica set, supply a seed list of members
//    Mongo m = new Mongo(Arrays.asList(new ServerAddress("localhost", 27017),
//                                          new ServerAddress("localhost", 27018),
//                                          new ServerAddress("localhost", 27019)));
    m.setWriteConcern(WriteConcern.SAFE);
    DB db = m.getDB(mongoDatabaseName);
//    boolean auth = db.authenticate(myUserName, myPassword);
    
    Map<String, Color> nodeColorMappings = new HashMap<>();
    if (colorPropsFilename != null) {
      nodeColorMappings.putAll(loadColorMappings(new File(colorPropsFilename)));
    }
    
    exportGdf(m, db, nodeColorMappings, graphName, graphBranch, new File(outputFilename));
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
  
  
  private static void exportGdf(Mongo m, DB db, Map<String, Color> nodeColorMappings,
          String graphName, String graphBranch, File outputFile)
      throws IOException, LogPlayerException, RevisionLogException
  {
    /*
     * Database access
     */
    GraphCheckoutNamingScheme collectionNamer = new GraphCheckoutNamingScheme(graphName, graphBranch);
    DBCollection nodeCol = db.getCollection(collectionNamer.getNodeCollectionName());
    DBCollection edgeCol = db.getCollection(collectionNamer.getEdgeCollectionName());
    
    NodeDAO nodeDao = GraphDAOFactory.createDefaultNodeDAO(m, db, nodeCol, edgeCol);
    EdgeDAO edgeDao = GraphDAOFactory.createDefaultEdgeDAO(m, db, nodeCol, edgeCol);
    
    RevisionLog log = new RevisionLogDirectToMongoDbImpl(m, db);
    
    
    /*
     * GDF file writer
     */
    GdfWriter writer = new GdfWriter(new BufferedWriter(new FileWriter(outputFile)));
    
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
    writer.close();
  }
  
  
}
