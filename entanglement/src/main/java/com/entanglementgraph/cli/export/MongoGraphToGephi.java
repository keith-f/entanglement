/*
 * Copyright 2012 Allyson Lister
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

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.GraphConnectionFactory;
import com.entanglementgraph.util.GraphConnectionFactoryException;
import com.entanglementgraph.util.MongoObjectParsers;
import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.*;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import com.entanglementgraph.ObjectMarshallerFactory;
import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.player.GraphCheckoutNamingScheme;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.graph.GraphDAOFactory;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.Edge;
import com.entanglementgraph.revlog.RevisionLog;
import com.entanglementgraph.revlog.RevisionLogDirectToMongoDbImpl;
import com.entanglementgraph.revlog.RevisionLogException;

/**
 *
 * @author Allyson Lister
 */
public class MongoGraphToGephi {

  private static final Logger logger = Logger.getLogger(MongoGraphToGephi.class.getName());
  private static final ClassLoader classLoader = MongoGraphToGephi.class.getClassLoader();
  private static final Color DEFAULT_COLOR = Color.BLACK;
  
  private static final DbObjectMarshaller marshaller = ObjectMarshallerFactory.create(MongoGraphToGephi.class.getClassLoader());
  
  private final NodeDAO nodeDao;
  private final EdgeDAO edgeDao;
  
  //Optional file containing node type --> Color
  private File colorPropsFile;

  public MongoGraphToGephi(GraphConnection conn) {
    this.nodeDao = conn.getNodeDao();
    this.edgeDao = conn.getEdgeDao();
  }

  private static void printHelpExit(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    String cmdSyntax = "mongoGraphToGEXF.sh";
    String header = "";
    String footer = "";
    int width = 80;
    //formatter.printHelp( "notification.sh", options );
    formatter.printHelp(width, cmdSyntax, header, options, footer);
    System.exit(0);
  }

  public static void main(String[] args) throws UnknownHostException,
      RevisionLogException, IOException,
      GraphModelException, GraphConnectionFactoryException, DbObjectMarshallerException {
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

    if (args.length == 0) {
      printHelpExit(options);
    }

    String mongoHost = null;
    String mongoDatabaseName = null;
    String graphName = null;
    String graphBranch = null;
    String outputFilename = null;

    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("mongo-host")) {
        mongoHost = line.getOptionValue("mongo-host", null);
      } else {
        throw new IllegalArgumentException(
                "You must specify a hostname");
      }

      if (line.hasOption("mongo-database")) {
        mongoDatabaseName = line.
                getOptionValue("mongo-database", null);
      } else {
        throw new IllegalArgumentException(
                "You must specify a database name");
      }

      if (line.hasOption("graph-name")) {
        graphName = line.getOptionValue("graph-name", null);
      } else {
        throw new IllegalArgumentException(
                "You must specify a graph name");
      }

      if (line.hasOption("graph-branch")) {
        graphBranch = line.getOptionValue("graph-branch", null);
      } else {
        throw new IllegalArgumentException(
                "You must specify a graph branch name");
      }

      if (line.hasOption("output-file")) {
        outputFilename = line.getOptionValue("output-file", null);
        if (!outputFilename.contains(".gexf") && outputFilename.
                contains(".pdf") && outputFilename.contains(".svg")
                && outputFilename.contains(".gdf")) {
          throw new IllegalArgumentException(
                  "You must specify an output filename with an extension "
                  + "of [.pdf|.svg|.gexf|gdf]");
        }
      } else {
        throw new IllegalArgumentException(
                "You must specify an output filename with an extension "
                + "of [.pdf|.svg|.gexf|gdf]");
      }

    } catch (ParseException e) {
      printHelpExit(options);
      System.exit(1);
    }


    GraphConnectionFactory connFact = new GraphConnectionFactory(classLoader, mongoHost, mongoDatabaseName);
    GraphConnection conn = connFact.connect(graphName, graphBranch);

    MongoGraphToGephi exporter = new MongoGraphToGephi(conn);
    exporter.exportGexf(new File(outputFilename));
    System.out.println("\n\nDone.");
  }

  private static String keysetToId(EntityKeys keyset) {
    if (keyset.getUids().isEmpty()) {
      throw new IllegalArgumentException("An entity must have at least one UID");
    }
    return keyset.getUids().iterator().next();
  }

  public void exportGexf(File outputFile)
      throws IOException, GraphModelException, RevisionLogException, DbObjectMarshallerException {
    //Load colour mappings, if any
    Map<String, Color> nodeColorMappings = new HashMap<>();
    if (colorPropsFile != null) {
      nodeColorMappings.putAll(loadColorMappings(colorPropsFile));
    }

    // Init a gephi project - and therefore a workspace
    ProjectController pc = Lookup.getDefault().lookup(
            ProjectController.class);
    pc.newProject();
    Workspace workspace = pc.getCurrentWorkspace();

    // Get a graph model - it exists because we have a workspace
    GraphModel graphModel = Lookup.getDefault().lookup(
            GraphController.class).getModel();

    // Create a directed graph based on this graph model
    DirectedGraph directedGraph = graphModel.getDirectedGraph();
    // Add column for node type
    AttributeController ac = Lookup.getDefault().
            lookup(AttributeController.class);
    AttributeModel attributeModel = ac.getModel();


//        AttributeColumn nodeTypeCol = attributeModel.getNodeTable().addColumn(
//                "nodeType",
//                AttributeType.STRING );

    Map<String, AttributeColumn> nodeAttrNameToAttributeCol = new HashMap<>();

    // Create Gephi nodes

    for (DBObject node : nodeDao.iterateAll()) {
      EntityKeys keyset = marshaller.deserialize(node.get(NodeDAO.FIELD_KEYS).toString(), EntityKeys.class);


      String type = (String) node.get(NodeDAO.FIELD_TYPE);
      Color nodeColour = DEFAULT_COLOR;
      if (nodeColorMappings.containsKey(type)) {
        nodeColour = nodeColorMappings.get(type);
      }

      String uidStr = keysetToId(keyset);
      org.gephi.graph.api.Node gephiNode = graphModel.factory().newNode(uidStr);
      if (keyset.getNames().isEmpty()) {
        gephiNode.getNodeData().setLabel(uidStr);
      } else {
        gephiNode.getNodeData().setLabel(keyset.getNames().toString()); //TODO ugly name
      }
      float[] rgbColorComp = nodeColour.getRGBColorComponents(null);
      gephiNode.getNodeData().setColor(rgbColorComp[0], rgbColorComp[1], rgbColorComp[2]);
//      gephiNode.getNodeData().setColor(nodeColour.getRed(), nodeColour.
//              getGreen(), nodeColour.getBlue());
//            gephiNode.getNodeData().getAttributes().setValue( nodeTypeCol.getIndex(), node.getType() );

      for (String nodeAttrName : node.keySet()) {

        Object val = node.get(nodeAttrName);
        if (nodeAttrName.equals("_id")) {
          continue;
        }
        if (val instanceof BasicDBList) {
          val = val.toString();
        } else if (val instanceof BasicDBObject) {
          logger.info("Replacing value of type BasicDBObject with String.");
          val = val.toString();
        }
        if (val == null) {
          logger.info("Skipping node attribute with null value: "+nodeAttrName);
          continue;
        }
        AttributeColumn attrCol = nodeAttrNameToAttributeCol.get(nodeAttrName);
        if (attrCol == null) {
          attrCol = attributeModel.getNodeTable().addColumn(
                  nodeAttrName, AttributeType.parse(val));
          nodeAttrNameToAttributeCol.put(nodeAttrName, attrCol);
        }
//        logger.info("nodeAttrName: " + nodeAttrName + ", val: " + val + ", type: " + val.getClass().getName());
//        logger.info("attrCol: " + attrCol);
        System.out.println("Gephi node: "+gephiNode);
        System.out.println("Node data: "+gephiNode.getNodeData());
        System.out.println("Attributes: "+gephiNode.getNodeData().getAttributes());
        System.out.println("Attributes col: "+attrCol.getIndex());
        System.out.println("Node attr name: "+nodeAttrName);
        System.out.println("Val: "+val);
        System.out.println("Val type: "+val.getClass().getName());
        gephiNode.getNodeData().getAttributes().setValue(attrCol.getIndex(), val);
      }

      directedGraph.addNode(gephiNode);
    }

    // Create Gephi edges; currently with a standard weight of 1
    // and no set color
    Iterable<Edge> edgeItr = new DeserialisingIterable<>(
            edgeDao.iterateAll(), marshaller, Edge.class);
    for (Edge edge : edgeItr) {
      BasicDBObject fromObj = nodeDao.getByKey(edge.getFrom());
      String fromId = keysetToId(MongoObjectParsers.parseKeyset(marshaller, fromObj));

      BasicDBObject toObj = nodeDao.getByKey(edge.getTo());
      String toId = keysetToId(MongoObjectParsers.parseKeyset(marshaller, toObj));


      org.gephi.graph.api.Edge gephiEdge = graphModel.factory().
              newEdge(directedGraph.getNode(fromId), directedGraph.getNode(toId), 1f, true);
      gephiEdge.getEdgeData().setLabel(edge.getKeys().getType());
      directedGraph.addEdge(gephiEdge);
    }

    // Print out a summary of the full graph
    System.out.println("Complete Nodes: " + directedGraph.getNodeCount()
            + " Complete Edges: " + directedGraph.getEdgeCount());

    // This line is a hack to get around a weird NullPointerException
    // which crops up when exporting to gexf. See url below for details:
    // https://forum.gephi.org/viewtopic.php?f=27&t=2337
    DynamicModel dynamicModel = Lookup.getDefault().lookup(
            DynamicController.class).getModel();

    // Export full graph in GEXF format
    ExportController ec = Lookup.getDefault().
            lookup(ExportController.class);
    System.out.println(outputFile.getAbsoluteFile());
    ec.exportFile(outputFile);

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
  
}
