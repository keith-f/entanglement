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
 * File created: 15-Nov-2012, 16:43:38
 */

package uk.ac.ncl.aries.entanglement.player;

import com.mongodb.*;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.ObjectMarshallerFactory;
import uk.ac.ncl.aries.entanglement.player.data.Edge;

/**
 *
 * @author Keith Flanagan
 */
public class EdgeDAOSeparateDocImpl
    extends AbstractGraphEntityDAO
    implements EdgeDAO
{
  private static final Logger logger =
      Logger.getLogger(EdgeDAOSeparateDocImpl.class.getName()); 
  
  private final DBCollection nodeCol;
  
  ////////// DEBUG / TEST - Performance info stuff (end)
  
  public EdgeDAOSeparateDocImpl(Mongo m, DB db, DBCollection nodeCol, DBCollection edgeCol)
  {
    super(m, db, edgeCol);
    
    this.nodeCol = nodeCol;
  }

  
  @Override
  public DBCollection getNodeCol() {
    return nodeCol;
  }
  
  
//  @Override
//  public void store(Edge edge)
//      throws LogPlayerException
//  {
//    try {
////      logger.log(Level.INFO, "Storing edge: {0}", edge);
//      if (insertModeHint == InsertMode.INSERT_CONSISTENCY) {
//        if (existsByUniqueId(edge.getUid())) {
//          throw new LogPlayerException(
//              "Failed to store an edge - an edge with this unique ID already exists: "+edge.getUid());
//        }
//      }
//
//      DBObject dbObject = marshaller.serialize(edge);
//      edgeCol.insert(dbObject);
//      
//      
//      /////// DEBUG (Performance info)
//      if (printPeriodicPerformanceInfo) {        
//        insertCount++;
//        if (timestampOfLastPerformanceMessage < 0) {
//          //First ever insert
//          long now = System.currentTimeMillis();
//          timestampOfLastPerformanceMessage = now;
//          timestampOfFirstInsert = now;
//          return;
//        }
//        if (insertCount % PRINT_PERF_INFO_EVERY == 0) {
//          long now = System.currentTimeMillis();
//          double secondsPerBlock = (now - timestampOfLastPerformanceMessage);
//          secondsPerBlock = secondsPerBlock / 1000;
//          double totalSeconds = (now - timestampOfFirstInsert);
//          totalSeconds = totalSeconds / 1000;
//          logger.log(Level.INFO,
//                  "Inserted a total of\t{0}\tEdge documents. "
//                  + "Total time\t{1}\t seconds. Seconds since last block: {2}",
//                  new Object[]{insertCount, totalSeconds, secondsPerBlock});
//          timestampOfLastPerformanceMessage = now;
//        }
//      }
//      /////// DEBUG (Performance info) (end)
//    }
//    catch(Exception e)
//    {
//      throw new LogPlayerException("Failed to store item: "+edge, e);
//    }
//  }
  

  
  
  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(String fromNodeUid, String toNodeUid)
      throws LogPlayerException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges between nodes: {0} --> {1}", 
              new Object[]{fromNodeUid, toNodeUid});
      //Empty 'query' selects all documents (nodes in this case)
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);
      query.put(FIELD_TO_NODE_UID, toNodeUid);

      final DBCursor cursor = col.find(query);

//      return new DeserialisingIterable<>(cursor, marshaller, Edge.class);
      return cursor;
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }

  @Override
  public Iterable<DBObject> iterateEdgesFromNode(String fromNodeUid)
          throws LogPlayerException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges starting from node: {0}", 
              new Object[]{fromNodeUid});
      //Empty 'query' selects all documents (nodes in this case)
      query = new BasicDBObject();
      query.put(FIELD_FROM_NODE_UID, fromNodeUid);

      final DBCursor cursor = col.find(query);
//      return new DeserialisingIterable<>(cursor, marshaller, Edge.class);
      return cursor;
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }
  
  @Override
  public Iterable<DBObject> iterateEdgesToNode(String fromNodeUid)
          throws LogPlayerException
  {
    DBObject query = null;
    try {
      logger.log(Level.INFO, "Iterating edges ending at node: {0}", 
              new Object[]{fromNodeUid});
      //Empty 'query' selects all documents (nodes in this case)
      query = new BasicDBObject();
      query.put(FIELD_TO_NODE_UID, fromNodeUid);

      final DBCursor cursor = col.find(query);
      return cursor;
    }
    catch(Exception e) {
      throw new LogPlayerException("Failed to perform database operation:\n"
          + "Query: "+query, e);
    }
  }


}
