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

package uk.ac.ncl.aries.entanglement;

import com.mongodb.*;
import com.mongodb.util.JSON;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;
import com.torrenttamer.util.UidGenerator;
import com.torrenttamer.util.serialization.JsonSerializerException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogDirectToMongoDbImpl;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateEdgeFromObjBetweenNamedNodes;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNodeFromObjIfNotExistsByName;
import uk.ac.ncl.aries.entanglement.revlog.commands.GraphOperation;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionBegin;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionCommit;

/**
 *
 * @author Keith Flanagan
 */
public class TestGraph2
{
  public static void main(String[] args) throws UnknownHostException, RevisionLogException, JsonSerializerException, DbObjectMarshallerException
  {
    if (args.length != 3) {
      System.out.println("USAGE:\n"
          + "  * database name\n"
          + "  * graph name\n"
          + "  * graph branch name\n"
      );
      System.exit(1);
    }
    
    String dbName = args[0];
    String graphName = args[1];
    String graphBranchName = args[2];
    
    
    
    Mongo m = new Mongo();
    
//    Mongo m = new Mongo( "localhost" );
    // or
//    Mongo m = new Mongo( "localhost" , 27017 );
    // or, to connect to a replica set, supply a seed list of members
//    Mongo m = new Mongo(Arrays.asList(new ServerAddress("localhost", 27017),
//                                          new ServerAddress("localhost", 27018),
//                                          new ServerAddress("localhost", 27019)));
    
    m.setWriteConcern(WriteConcern.SAFE);

    DB db = m.getDB( dbName );
//    boolean auth = db.authenticate(myUserName, myPassword);
    
    
    RevisionLog log = new RevisionLogDirectToMongoDbImpl(m, db);
    
//    JsonUtils jsonSer = new JsonUtils();
//    jsonSer.setExcludeNullAndEmptyValues(true);
//    jsonSer.setIgnoreUnknownProperties(true);
    DbObjectMarshaller objUtil = ObjectMarshallerFactory.create();
    
    String type = "cds";
    String ds = "default";
    String et = "default";
    String txnId = UidGenerator.generateUid();
    
    int toCreate = 3;
    
    int txnSubmitId = 0;
    log.submitRevision(graphName, graphBranchName, txnId, txnSubmitId++, new TransactionBegin(txnId));
    for (int i=0; i<toCreate; i++)
    {
      TestNode1 nodeType1 = new TestNode1();
      nodeType1.setUid(UidGenerator.generateUid());
      nodeType1.setName("TestNode1 unique name: "+UidGenerator.generateSimpleUid());
      nodeType1.setNumber(i);
      nodeType1.setString1("First string: "+i);
      nodeType1.setString2("Second string: "+i);
      nodeType1.getListOfThings().add("Item: "+i+"a");
      nodeType1.getListOfThings().add("Item: "+i+"b");
      nodeType1.getListOfThings().add("Item: "+i+"c");
      
      
      
      TestNode2 nodeType2 = new TestNode2();
      nodeType2.setName("TestNode2 unique name: "+UidGenerator.generateSimpleUid());
      nodeType2.setDoubleNumber(Math.random());
      nodeType2.setAnotherNumber((float) Math.random());
      nodeType2.setString1("First string: "+i);
      nodeType2.setString2("Second string: "+i);
      nodeType2.getMapOfThings().put("First item", i);
      nodeType2.getMapOfThings().put("Second item", i);
      nodeType2.getMapOfThings().put("Third item", i);
      
      TestEdge1 edgeType1 = new TestEdge1();
      edgeType1.setSomeProperty("Some edge string: "+1);
      
      
      //Example of submitting several revisions at the same time.
      List<GraphOperation> opList = new LinkedList<>();
//      BasicDBObject nodeType1DbObject = (BasicDBObject) JSON.parse(
//          JsonSerializer.serializeToString(nodeType1));
//      
//      BasicDBObject nodeType2DbObject = (BasicDBObject) JSON.parse(
//          JsonSerializer.serializeToString(nodeType2));
  
      
      opList.add(new CreateNodeFromObjIfNotExistsByName(objUtil.serialize(nodeType1)));
      opList.add(new CreateNodeFromObjIfNotExistsByName(objUtil.serialize(nodeType2)));
      opList.add(new CreateEdgeFromObjBetweenNamedNodes(objUtil.serialize(edgeType1), nodeType1.getName(), nodeType2.getName()));
//      opList.add(new CreateNode2IfNotExistsByName(jsonSer, nodeType1));
//      opList.add(new CreateNode2IfNotExistsByName(jsonSer, nodeType2));
//      opList.add(new CreateEdge2BetweenNamedNodes(jsonSer, edgeType1, nodeType1.getName(), nodeType2.getName()));
//      opList.add(new CreateNode2IfNotExistsByName(nodeType1));
//      opList.add(new CreateNode2IfNotExistsByName(nodeType2));
      log.submitRevisions(graphName, graphBranchName, txnId, txnSubmitId++, opList);
      
    }
    log.submitRevision(graphName, graphBranchName, txnId, txnSubmitId++, new TransactionCommit(txnId));
    
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
