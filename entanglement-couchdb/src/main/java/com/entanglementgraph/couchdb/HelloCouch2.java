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

import com.entanglementgraph.couchdb.testdata.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.jackson.JacksonDBObjectMarshaller;
import com.scalesinformatics.util.UidGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.SerializationConfig;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.impl.StdObjectMapperFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
public class HelloCouch2 {
  private static final DbObjectMarshaller m = new JacksonDBObjectMarshaller();

  public static void main(String[] args) throws Exception {
    HttpClient httpClient = new StdHttpClient.Builder()
        .url("http://localhost:5984")
//        .username("admin")
//        .password("secret")
        .build();


//    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient, new ExtStdObjectMapperFactory());
// if the second parameter is true, the database will be created if it doesn't exists
    CouchDbConnector db = dbInstance.createConnector("my_first_database", true);
    System.out.println("Connected");

//    db.create("testobj2", new Sofa2());
//    Sofa2DAO dao = new Sofa2DAO(db);
//    Sofa2 sofa2 = new Sofa2();
//    sofa2.setColor("blue");
//    sofa2.setId(UidGenerator.generateSimpleUid());
//    dao.add(sofa2);



    // Write sample document
    System.out.println("Writing objects");
    NodeCouchDbDAO dao = new NodeCouchDbDAO(db);
    List<String> uids = new LinkedList<>();

    ANode aNode = new ANode();
    aNode.getKeys().addUid(UidGenerator.generateSimpleUid());

//    dao.add(new ANode(newUid(uids), 4.4, "hello world!<>"));
//    dao.add(new BNode(newUid(uids), 5.5));

//    Map<String, Object> nodeAsMap = new HashMap<>();
//    nodeAsMap.put("+jcl", "a-node3");
//    nodeAsMap.put("keys.type", "a-node3");
//    nodeAsMap.put("foo", "foo");
//    nodeAsMap.put("boo", "boo");
//    db.create(newUid(uids), nodeAsMap);





    // Read documents back from DB
    for (String dbUid : uids) {
      System.out.println("Reading document: "+dbUid);

      // Read back as a JsonNode
      NewNode doc = db.get(NewNode.class, dbUid);
      System.out.println("Java class: "+doc.getClass().getName());
      System.out.println("toString: "+doc);
      System.out.println("As Map: "+db.get(Map.class, dbUid));
      System.out.println();
//      JsonNode b = doc.findPath("b");
//      System.out.println("b: "+a);
    }

  }

  private static String newUid(List<String> uids) {
    String uid = UidGenerator.generateSimpleUid();
    uids.add(uid);
    return uid;
  }

}
