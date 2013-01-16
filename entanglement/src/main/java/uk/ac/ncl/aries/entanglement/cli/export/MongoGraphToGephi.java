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
package uk.ac.ncl.aries.entanglement.cli.export;

import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import javax.xml.bind.DatatypeConverter;
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
import uk.ac.ncl.aries.entanglement.ObjectMarshallerFactory;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.GraphCheckoutNamingScheme;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphDAOFactory;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.graph.data.Edge;
import uk.ac.ncl.aries.entanglement.graph.data.Node;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogDirectToMongoDbImpl;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;

/**
 *
 * @author Allyson Lister
 */
public class MongoGraphToGephi {

    private static final DbObjectMarshaller marshaller = ObjectMarshallerFactory.create();
    private static final Color DEFAULT_COLOR = Color.BLACK;

    private static void printHelpExit ( Options options ) {
        HelpFormatter formatter = new HelpFormatter();
        String cmdSyntax = "mongoGraphToGEXF.sh";
        String header = "";
        String footer = "";
        int width = 80;
        //formatter.printHelp( "notification.sh", options );
        formatter.printHelp( width, cmdSyntax, header, options, footer );
        System.exit( 0 );
    }

    public static void main ( String[] args ) throws UnknownHostException,
            RevisionLogException, IOException,
            GraphModelException {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

//    options.addOption("g", "format-gdf", false,
//        "Specifies that the output format is GDF (currently the only option)");

        options.addOption( "h", "mongo-host", true,
                "The MongoDB server host to connect to." );

        options.addOption( "d", "mongo-database", true,
                "The name of a MongoDB database to connect to." );

        options.addOption( "g", "graph-name", true,
                "The name of a graph to use (there may be multiple graphs per MongoDB database)." );

        options.addOption( "b", "graph-branch", true,
                "The name of a graph branch to use (there may be multiple branches per graph)." );

        options.addOption( "o", "output-file", true,
                "Specifies a path to a file to use " );

        if ( args.length == 0 ) {
            printHelpExit( options );
        }

        String mongoHost = null;
        String mongoDatabaseName = null;
        String graphName = null;
        String graphBranch = null;
        String outputFilename = null;

        try {
            CommandLine line = parser.parse( options, args );

            if ( line.hasOption( "mongo-host" ) ) {
                mongoHost = line.getOptionValue( "mongo-host", null );
            } else {
                throw new IllegalArgumentException(
                        "You must specify a hostname" );
            }

            if ( line.hasOption( "mongo-database" ) ) {
                mongoDatabaseName = line.
                        getOptionValue( "mongo-database", null );
            } else {
                throw new IllegalArgumentException(
                        "You must specify a database name" );
            }

            if ( line.hasOption( "graph-name" ) ) {
                graphName = line.getOptionValue( "graph-name", null );
            } else {
                throw new IllegalArgumentException(
                        "You must specify a graph name" );
            }

            if ( line.hasOption( "graph-branch" ) ) {
                graphBranch = line.getOptionValue( "graph-branch", null );
            } else {
                throw new IllegalArgumentException(
                        "You must specify a graph branch name" );
            }

            if ( line.hasOption( "output-file" ) ) {
                outputFilename = line.getOptionValue( "output-file", null );
                if ( !outputFilename.contains( ".gexf" ) && outputFilename.
                        contains( ".pdf" ) && outputFilename.contains( ".svg" )
                        && outputFilename.contains( ".gdf" ) ) {
                    throw new IllegalArgumentException(
                            "You must specify an output filename with an extension "
                            + "of [.pdf|.svg|.gexf|gdf]" );
                }
            } else {
                throw new IllegalArgumentException(
                        "You must specify an output filename with an extension "
                        + "of [.pdf|.svg|.gexf|gdf]" );
            }

        } catch ( ParseException e ) {
            printHelpExit( options );
            System.exit( 1 );
        }



        Mongo m = new Mongo();
        m.setWriteConcern( WriteConcern.SAFE );
        DB db = m.getDB( mongoDatabaseName );

        exportGexf( m, db, graphName, graphBranch, new File(
                outputFilename ) );
        System.out.println( "\n\nDone." );
    }

    private static void exportGexf ( Mongo m, DB db,
            String graphName, String graphBranch, File outputFile )
            throws IOException, GraphModelException, RevisionLogException {
        /*
         * Database access
         */
        GraphCheckoutNamingScheme collectionNamer = new GraphCheckoutNamingScheme( graphName, graphBranch );
        DBCollection nodeCol = db.getCollection(collectionNamer.getNodeCollectionName() );
        DBCollection edgeCol = db.getCollection(collectionNamer.getEdgeCollectionName() );

        NodeDAO nodeDao = GraphDAOFactory.createDefaultNodeDAO(m, db, nodeCol, edgeCol);
        EdgeDAO edgeDao = GraphDAOFactory.createDefaultEdgeDAO(m, db, nodeCol, edgeCol);

        RevisionLog log = new RevisionLogDirectToMongoDbImpl( m, db );

        // Init a gephi project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(
                ProjectController.class );
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        // Get a graph model - it exists because we have a workspace
        GraphModel graphModel = Lookup.getDefault().lookup(
                GraphController.class ).getModel();

        // Create a directed graph based on this graph model
        DirectedGraph directedGraph = graphModel.getDirectedGraph();
        // Add column for node type
        AttributeController ac = Lookup.getDefault().
                lookup( AttributeController.class );
        AttributeModel attributeModel = ac.getModel();
        AttributeColumn nodeTypeCol = attributeModel.getNodeTable().addColumn(
                "nodeType",
                AttributeType.STRING );

        // Create Gephi nodes
        Iterable<Node> nodeItr = new DeserialisingIterable<>(nodeDao.iterateAll(), marshaller, Node.class);
        for (Node node : nodeItr) {
            String uidStr = node.getUid();
            Color nodeColour = DEFAULT_COLOR;
            org.gephi.graph.api.Node gephiNode = graphModel.factory().
                    newNode( uidStr );
            if ( node.getName() == null || node.getName().
                    isEmpty() ) {
                gephiNode.getNodeData().setLabel( uidStr );
            } else {
                gephiNode.getNodeData().setLabel( node.getName() );
            }
            gephiNode.getNodeData().setColor( nodeColour.getRed(), nodeColour.
                    getGreen(), nodeColour.getBlue() );
            gephiNode.getNodeData().getAttributes().setValue( nodeTypeCol.
                    getIndex(), node.getType() );
            directedGraph.addNode( gephiNode );
        }

        // Create Gephi edges; currently with a standard weight of 1
        // and no set color
        Iterable<Edge> edgeItr = new DeserialisingIterable<>(nodeDao.iterateAll(), marshaller, Edge.class);
        for (Edge edge : edgeItr) {
            String fromUidStr = edge.getFromUid();
            String toUidStr = edge.getToUid();
            org.gephi.graph.api.Edge gephiEdge = graphModel.factory().
                    newEdge( directedGraph.getNode( fromUidStr ),
                    directedGraph.getNode( toUidStr ), 1f, true );
            gephiEdge.getEdgeData().setLabel( edge.getType() );
            directedGraph.addEdge( gephiEdge );
        }

        // Print out a summary of the full graph
        System.out.println( "Complete Nodes: " + directedGraph.getNodeCount()
                + " Complete Edges: " + directedGraph.getEdgeCount() );

        // This line is a hack to get around a weird NullPointerException
        // which crops up when exporting to gexf. See url below for details:
        // https://forum.gephi.org/viewtopic.php?f=27&t=2337
        DynamicModel dynamicModel = Lookup.getDefault().lookup(
                DynamicController.class ).getModel();

        // Export full graph in GEXF format
        ExportController ec = Lookup.getDefault().
                lookup( ExportController.class );
        System.out.println( outputFile.getAbsoluteFile() );
        ec.exportFile( outputFile );

    }
}
