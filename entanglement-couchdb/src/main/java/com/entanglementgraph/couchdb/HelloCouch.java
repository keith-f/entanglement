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
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.mongodb.jackson.JacksonDBObjectMarshaller;
import com.scalesinformatics.util.UidGenerator;
import org.codehaus.jackson.JsonNode;
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
public class HelloCouch {
  private static final DbObjectMarshaller m = new JacksonDBObjectMarshaller();

  public static void main(String[] args) throws Exception {
    HttpClient httpClient = new StdHttpClient.Builder()
        .url("http://localhost:5984")
//        .username("admin")
//        .password("secret")
        .build();

    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient, new ExtStdObjectMapperFactory());
// if the second parameter is true, the database will be created if it doesn't exists
    CouchDbConnector db = dbInstance.createConnector("my_first_database", true);

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
    }
  }

  private static List<GraphOperation> createTestGraph() throws DbObjectMarshallerException {
    List<GraphOperation> ops = new LinkedList<>();

    Sofa sofa = new Sofa();
//    sofa.setUid(UidGenerator.generateUid());
    sofa.getKeys().addName("blue-{sofa}");
    sofa.setColor("blue");

    Pillow firmPillow = new Pillow();
    firmPillow.setSoftness(Pillow.Softness.FIRM);
    firmPillow.getKeys().addName("\"firm\"-pillow");

    HasPillow hasFirmPillow = new HasPillow();
    hasFirmPillow.getKeys().addUid(UidGenerator.generateUid());
    hasFirmPillow.setFrom(sofa.getKeys());
    hasFirmPillow.setTo(firmPillow.getKeys());

    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, sofa));
    ops.add(new NodeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, firmPillow));
    ops.add(new EdgeModification(MergePolicy.APPEND_NEW__LEAVE_EXISTING, hasFirmPillow));

//    ops.add(NodeModification.create(m, MergePolicy.APPEND_NEW__LEAVE_EXISTING, sofa));
//    ops.add(NodeModification.create(m, MergePolicy.APPEND_NEW__LEAVE_EXISTING, firmPillow));
//    ops.add(EdgeModification.create(m, MergePolicy.APPEND_NEW__LEAVE_EXISTING, hasFirmPillow));

//    ops.add(NodeModification.create())

    return ops;
  }
}
