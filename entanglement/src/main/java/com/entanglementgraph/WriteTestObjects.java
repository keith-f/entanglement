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
 * File created: 08-Nov-2012, 13:49:25
 */

package com.entanglementgraph;

import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.util.UidGenerator;
import com.torrenttamer.util.serialization.JsonSerializerException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.entanglementgraph.graph.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public class WriteTestObjects
{
  public static void main(String[] args) throws UnknownHostException, JsonSerializerException, DbObjectMarshallerException
  {
    Mongo m = new Mongo();
    
//    Mongo m = new Mongo( "localhost" );
    // or
//    Mongo m = new Mongo( "localhost" , 27017 );
    // or, to connect to a replica set, supply a seed list of members
//    Mongo m = new Mongo(Arrays.asList(new ServerAddress("localhost", 27017),
//                                          new ServerAddress("localhost", 27018),
//                                          new ServerAddress("localhost", 27019)));
    
    m.setWriteConcern(WriteConcern.SAFE);

    DB db = m.getDB( "aries-test" );
//    boolean auth = db.authenticate(myUserName, myPassword);
    
    
    DBCollection testCol = db.getCollection("testCollection");
    
    
//    BasicDBObject newDoc = new BasicDBObject();
//    newDoc.
//    testCol.insert(newDoc);
    
    
    Node node = new Node();
//    node.setNumericalId(3);
    node.setUid(UidGenerator.generateUid());
//    node.setGraphUniqueId(UidGenerator.generateUid());
    
//    node.getProperties().put("An_integer", 1);
//    node.getProperties().put("A String", "Foo");
    
    Map<Integer, String> subMap = new HashMap<>();
    subMap.put(1, "one");
    subMap.put(2, "two");
    subMap.put(3, "three");
//    node.getProperties().put("A map", subMap);
    DbObjectMarshaller marshaller = ObjectMarshallerFactory.create(WriteTestObjects.class.getClassLoader());
    DBObject dbObject = marshaller.serialize(node);
    testCol.insert(dbObject);
 
    
    
    System.out.println("Listing collections:");
    listCollections(db);
    
    
    System.out.println("\n\nFindOne:");
    findOne(testCol);
    
    System.out.println("\n\nCursor iteration:");
    cursorIterateAllDocs(testCol);
    
    System.out.println("\n\nDone.");
  }
  
  private static void listCollections(DB db)
  {
    Set<String> colls = db.getCollectionNames();

    for (String s : colls) {
        System.out.println(s);
    }
  }
  
  private static void findOne(DBCollection collection)
  {
    DBObject myDoc = collection.findOne();
    System.out.println(myDoc);
  }
  
  private static void cursorIterateAllDocs(DBCollection collection)
  {
    DBCursor cursor = collection.find();
    try
    {
      while (cursor.hasNext())
      {
        System.out.println(cursor.next());
      }
    }
    finally
    {
      cursor.close();
    }
  }
}
