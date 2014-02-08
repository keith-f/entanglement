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

package com.entanglementgraph.couchdb;

import com.entanglementgraph.couchdb.revlog.RevisionLog;
import com.entanglementgraph.couchdb.revlog.RevisionLogCouchDBImpl;
import com.entanglementgraph.couchdb.revlog.RevisionsCouchDbDAO;
import com.entanglementgraph.couchdb.revlog.commands.EdgeModification;
import com.entanglementgraph.couchdb.revlog.commands.GraphOperation;
import com.entanglementgraph.couchdb.revlog.commands.MergePolicy;
import com.entanglementgraph.couchdb.revlog.commands.NodeModification;
import com.entanglementgraph.couchdb.revlog.data.RevisionItemContainer;
import com.entanglementgraph.couchdb.testdata.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.mongodb.jackson.JacksonDBObjectMarshaller;
import com.scalesinformatics.util.UidGenerator;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Keith Flanagan
 */
public class HelloCouch2 {
  private static final DbObjectMarshaller m = new JacksonDBObjectMarshaller();

  public static void main(String[] args) throws Exception {

    System.setProperty("org.ektorp.support.AutoUpdateViewOnChange", "true");


    HttpClient httpClient = new StdHttpClient.Builder()
        .url("http://localhost:5984")
//        .username("admin")
//        .password("secret")
        .build();

    ExtStdObjectMapperFactory omFactory = new ExtStdObjectMapperFactory();
    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient, omFactory);
// if the second parameter is true, the database will be created if it doesn't exists
    CouchDbConnector db = dbInstance.createConnector("my_first_database", true);
    ObjectMapper om = omFactory.getLastCreatedObjectMapper();

    // Write sample document
    RevisionLog revLog = new RevisionLogCouchDBImpl(db);
    System.out.println("Connected");
    System.out.println("Committing objects");
    List<GraphOperation> ops = createTestGraph();
    String patchUid = UidGenerator.generateUid();
    String revDocDbId = revLog.submitRevisions("my-graph", patchUid, 0, ops);
    System.out.println("Written patch set");


    // Read document back from DB
    System.out.println("Reading document: "+revDocDbId);

//    RevisionsCouchDbDAO dao = new RevisionsCouchDbDAO(db);
    RevisionsCouchDbDAO dao = new RevisionsCouchDbDAO(db);
    RevisionItemContainer container = dao.get(revDocDbId);


//    JsonNode doc = db.get(JsonNode.class, revDocDbId);
//    JsonNode address = doc.findPath("address");

    System.out.println("Loaded succesfully: "+container);
    System.out.println("NodeModifications:");
    for (NodeModification nodeModification : container.getNodeUpdates()) {
      System.out.println(" * "+nodeModification.getNode().getClass().getName());
      System.out.println(" * " + nodeModification.getNode().getKeys().getClass().getName());
      System.out.println(" * "+nodeModification.getNode().getContent().getClass().getName());
      System.out.println(" * content: "+nodeModification.getNode().getContent());
      System.out.println("----");
    }

    System.out.println("\n\nNow testing our first node view:\n");

//    NodeByTypeNameView nbtnView = new NodeByTypeNameView(db);
//    List<NodeWithContent> allNodes = nbtnView.getAllNodes();
    List<Node> allNodes = dao.getAllNodes2();
    System.out.println("Found nodes: "+allNodes.size());
    for (Node node : allNodes) {
      System.out.println(" * "+node);
    }

    System.out.println("\n\nTesting resolving full keysets:\n");

    NodeDAOCouchDbImpl nodeDAO = new NodeDAOCouchDbImpl(db, om);
    for (NodeModification mod : nodeDAO.getAllNodes3()) {
      EntityKeys<?> modKeys = mod.getNode().getKeys();
      for (String uid : modKeys.getUids()) {
        EntityKeys<?> partial = new EntityKeys();
        partial.setType(modKeys.getType());
        partial.addUid(uid);
        System.out.println("Resolving full keyset for UID: " + uid + ": " + nodeDAO.populateFullKeyset(partial));
      }

      for (String name : modKeys.getNames()) {
        EntityKeys<?> partial = new EntityKeys(modKeys.getType(), name);
        System.out.println("Resolving full keyset for name: "+name+": "+nodeDAO.populateFullKeyset(partial));
      }
    }

    System.out.println("\n\nTesting node querying:\n");
    for (NodeModification mod : nodeDAO.getAllNodes3()) {
      EntityKeys<? extends Content> partial = mod.getNode().getKeys();
      EntityKeys<? extends Content> full = nodeDAO.populateFullKeyset(partial);
      System.out.println("#"+full);
      Node<? extends Content> node = nodeDAO.getByKey(full, true);
      if (node == null) {
        System.out.println("  * Node was NULL");
      } else {
        System.out.println("  * Found node: "+node);
        System.out.println("    - with content type: "+node.getContent().getClass().getName());
      }

    }


  }

  private static List<GraphOperation> createTestGraph() throws DbObjectMarshallerException {
    List<GraphOperation> ops = new LinkedList<>();

    Node<Sofa> sofaNode1 = new Node<>();
    sofaNode1.getKeys().setType("Sofa");
    sofaNode1.getKeys().addNames("a", "b", "c");
    Sofa sofa1 = new Sofa();
    sofa1.setColor("blue");
//    sofa.setNumSeats(3);
    sofaNode1.setContent(sofa1);
    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, sofaNode1));

    Node<Sofa> sofaNode2 = new Node<>();
    sofaNode2.getKeys().setType("Sofa");
    sofaNode2.getKeys().addNames("b", "d");
    Sofa sofa2= new Sofa();
//    sofa.setColor("blue");
    sofa2.setNumSeats(3);
    sofaNode2.setContent(sofa2);
    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, sofaNode2));


    Node<Sofa> sofaNode3 = new Node<>();
    sofaNode3.getKeys().setType("Sofa");
    sofaNode3.getKeys().addNames("d", "e");
    Sofa sofa3 = new Sofa();
//    sofa.setColor("blue");
//    sofa.setNumSeats(3);
    sofaNode3.setContent(sofa3);
    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, sofaNode3));



    Node<Pillow> pillowNode1 = new Node<>();
    pillowNode1.getKeys().setType("Pillow");
    pillowNode1.getKeys().addNames("f", "g");
    Pillow pillow1 = new Pillow();
    pillow1.setSoftness(Pillow.Softness.FIRM);
    pillowNode1.setContent(pillow1);
    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, pillowNode1));


    Node<Pillow> pillowNode2 = new Node<>();
    pillowNode2.getKeys().setType("Pillow");
    pillowNode2.getKeys().addNames("g", "h");
    Pillow pillow2 = new Pillow();
    pillow2.setSoftness(Pillow.Softness.FIRM);
    pillowNode2.setContent(pillow2);
    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, pillowNode2));



    Edge edge1 = new Edge<>();
    edge1.getKeys().setType("has-pillow");
    edge1.getKeys().addNames("j", "k");
    edge1.setFrom(new EntityKeys("Sofa", "b"));
    edge1.setTo(new EntityKeys("Pillow", "f"));
    ops.add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, edge1));

    Edge edge2 = new Edge<>();
    edge2.getKeys().setType("has-pillow");
    edge2.getKeys().addNames("k", "l", "m");
    edge2.setFrom(new EntityKeys("Sofa", "d"));
    edge2.setTo(new EntityKeys("Pillow", "h"));
    ops.add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, edge2));

    Edge edge3 = new Edge<>();
    edge3.getKeys().setType("has-pillow");
    edge3.getKeys().addNames("m", "n", "o");
    edge3.setFrom(new EntityKeys("Sofa", "e"));
    edge3.setTo(new EntityKeys("Pillow", "h"));
    ops.add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, edge3));

    return ops;
  }
}
