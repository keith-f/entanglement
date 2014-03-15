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
import com.entanglementgraph.couchdb.testdata.Pillow;
import com.entanglementgraph.couchdb.testdata.Sofa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import com.scalesinformatics.mongodb.jackson.JacksonDBObjectMarshaller;
import com.scalesinformatics.util.UidGenerator;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Keith Flanagan
 */
public class TestPropertyRename {
  private static final DbObjectMarshaller m = new JacksonDBObjectMarshaller();

  public static void main(String[] args) throws Exception {

    String serverUrl = args[0]; // "eg: http://localhost:5984"
    String databaseName = args[1]; // "my-db"

    System.setProperty("org.ektorp.support.AutoUpdateViewOnChange", "true");


    HttpClient httpClient = new StdHttpClient.Builder()
        .url(serverUrl)
//        .username("admin")
//        .password("secret")
        .build();

    ExtStdObjectMapperFactory omFactory = new ExtStdObjectMapperFactory();
    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient, omFactory);
// if the second parameter is true, the database will be created if it doesn't exists
    CouchDbConnector db = dbInstance.createConnector(databaseName, true);
    ObjectMapper om = omFactory.getLastCreatedObjectMapper();

    for (String docId : db.getAllDocIds()) {
      if (docId.startsWith("_design")) {
        continue;
      }
//      InputStream docStream = db.getAsStream(docId);
//      JsonNode doc = db.get(JsonNode.class, docId);
      ObjectNode doc = db.get(ObjectNode.class, docId);

      String oldName = "names";
      String newName = "uids";

      // Update
      for (JsonNode nodeUpdate : doc.get("nodeUpdates")) {
        System.out.println("Class: " + nodeUpdate.getClass().getName());
        System.out.println("Before: " + nodeUpdate);
        rename(oldName, newName, (ObjectNode) nodeUpdate.get("node").get("keys"));
        System.out.println("After: " + nodeUpdate);

        // Save back to DB:
        db.update(doc);
      }
      for (JsonNode edgeUpdate : doc.get("edgeUpdates")) {
        System.out.println("Class: "+edgeUpdate.getClass().getName());
        System.out.println("Before: " + edgeUpdate);
        rename(oldName, newName,  (ObjectNode) edgeUpdate.get("edge").get("keys"));
        rename(oldName, newName, (ObjectNode) edgeUpdate.get("edge").get("from"));
        rename(oldName, newName, (ObjectNode) edgeUpdate.get("edge").get("to"));

        System.out.println("After: " + edgeUpdate);

        // Save back to DB:
        db.update(doc);
      }
    }

  }

  private static void rename(String oldName, String newName, ObjectNode keysNode) {
    if (keysNode == null) {
      return;
    }
    JsonNode list = keysNode.get(oldName);
    if (list == null) {
      return;
    }
    keysNode.remove(oldName);
    keysNode.put(newName, list);
  }

}
