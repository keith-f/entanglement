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
 * File created: 13-Nov-2012, 14:47:59
 */

package com.entanglementgraph.revlog;

import com.mongodb.*;

/**
 *
 * @author Keith Flanagan
 */
public class Counter
{
  private static final String COL_COUNTERS = "counters";
  private static final String FIELD_COUNTER = "c";
  private static final long INCREMENT_BY = 1;
  
  private final Mongo m;
  private final DB db;
  
  private final DBCollection col;
  
  private final String counterName;
  
  public Counter(Mongo m, DB db, String counterName)
  {
    this.m = m;
    this.db = db;
    this.col = db.getCollection(COL_COUNTERS);
    this.counterName = counterName;
  }
  
  public long next()
  {
    boolean upsert = true;
    boolean returnNew = true;
    boolean remove = false;
    
    DBObject sort = new BasicDBObject();
    
    DBObject query = new BasicDBObject();
    query.put("_id", counterName);
    
    // Same as: update: {$inc: {c: 1}}
    DBObject update = new BasicDBObject(
        "$inc", new BasicDBObject(FIELD_COUNTER, INCREMENT_BY)); 
//    System.out.println("update: "+update);
    
    DBObject fields = new BasicDBObject();
//    fields.put(FIELD_COUNTER, 1);
    
    
    DBObject result = col.findAndModify(
        query, fields, sort, remove, update, returnNew, upsert);
    
    return (Long) result.get(FIELD_COUNTER);
  }

}
