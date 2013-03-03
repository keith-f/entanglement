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

import java.net.UnknownHostException;

import com.entanglementgraph.revlog.Counter;
import com.entanglementgraph.revlog.RevisionLogException;

/**
 *
 * @author Keith Flanagan
 */
public class TestCounter
{
  public static void main(String[] args) throws UnknownHostException, RevisionLogException
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
    
//    DBCollection testCol = db.getCollection("testCollection");
    
    
//    BasicDBObject newDoc = new BasicDBObject();
//    newDoc.
//    testCol.insert(newDoc);
    
    System.out.println("Starting auto-increment counter test");
    
    Counter counters = new Counter(m, db, "test-counter");
    
    int toCreate = 1000000;
    long start = System.currentTimeMillis();
    for (int i=0; i<toCreate; i++)
    {
      long next = counters.next();
//      System.out.println(next);
    }
    long end = System.currentTimeMillis();
    double durationSecs = (end - start) / 1000d;
    
    double opsPerSec = toCreate / durationSecs;
    System.out.println("Took: "+durationSecs+" seconds to perform "+toCreate
        +" operations = "+opsPerSec+" operations per second");
    
    System.out.println("\n\nDone.");
  }
  
}
