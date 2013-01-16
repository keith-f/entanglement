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
import com.torrenttamer.util.UidGenerator;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.ncl.aries.entanglement.player.data.Node;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLog;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogDirectToMongoDbImpl;
import uk.ac.ncl.aries.entanglement.revlog.RevisionLogException;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateEdge;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNode;
import uk.ac.ncl.aries.entanglement.revlog.commands.GraphOperation;
import uk.ac.ncl.aries.entanglement.revlog.commands.SetNamedNodeProperty;
import uk.ac.ncl.aries.entanglement.revlog.commands.SetNodeProperty;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionBegin;
import uk.ac.ncl.aries.entanglement.revlog.commands.TransactionCommit;

/**
 *
 * @author Keith Flanagan
 */
public class TestGraph1
{
  public static void main(String[] args) throws UnknownHostException, RevisionLogException
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
    
    String type = "cds";
    String ds = "default";
    String et = "default";
    String txnId = UidGenerator.generateUid();
    
    int toCreate = 3;
    
    int txnSubmitId = 0;
    log.submitRevision(graphName, graphBranchName, txnId, txnSubmitId++, new TransactionBegin(txnId));
    for (int i=0; i<toCreate; i++)
    {
//      String uid = UidGenerator.generateUid();
//      String uid = i+"a";
//      CreateNodeIfNotExists n1 = new CreateNodeIfNotExistsByName(type, UidGenerator.generateUid(), i+"a", ds, et);
//      SetNamedNodeProperty p1a = new SetNamedNodeProperty(i+"a", "some-string", "foo");
//      SetNamedNodeProperty p1b = new SetNamedNodeProperty(i+"a", "some-string", "bar");
//      SetNamedNodeProperty p2 = new SetNamedNodeProperty(i+"a", "some-integer", 23);
//      
//      CreateNodeIfNotExistsByName n2 = new CreateNodeIfNotExistsByName(type, UidGenerator.generateUid(), i+"b", ds, et);
//      CreateEdgeBetweenNamedNodes e1 = new CreateEdgeBetweenNamedNodes("has-part", UidGenerator.generateUid(), n1.getName(), n2.getName());
      
      
      
      //Examples of individual submissions
      
      
//      log.submitRevision(graphName, graphBranchName, txnId, txnSubmitId++, n1);
//      log.submitRevision(graphName, graphBranchName, txnId, txnSubmitId++, p1a);
//      log.submitRevision(graphName, graphBranchName, txnId, txnSubmitId++, p1b);
      
      //Example of submitting several revisions at the same time.
      List<GraphOperation> opList = new LinkedList<>();
//      opList.add(p2);
//      opList.add(n2);
//      opList.add(e1);
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
