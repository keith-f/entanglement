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
 */

package uk.ac.ncl.aries.entanglement.graph;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteResult;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DeserialisingIterable;
import com.torrenttamer.mongodb.dbobject.KeyExtractingIterable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.ObjectMarshallerFactory;
import uk.ac.ncl.aries.entanglement.graph.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public class NodeDAONodePerDocImpl
    extends AbstractGraphEntityDAO
    implements NodeDAO
{
  private static final Logger logger =
      Logger.getLogger(NodeDAO.class.getName());

  
  public NodeDAONodePerDocImpl(Mongo m, DB db, DBCollection col)
  {
    super(m, db, col);
  }
  
  
  
// @Override
//  public void store(Node node)
//      throws LogPlayerException
//  {
//    /*
//     * Storing nodes is easy since they are first-class objects in the collection
//     */
//    try {
////      logger.log(Level.INFO, "Storing node: {0}", node);
//      if (insertModeHint == InsertMode.INSERT_CONSISTENCY) {
////        logger.info("Running in consistency mode");
//        if (existsByUniqueId(node.getUid())) {
//          throw new LogPlayerException(
//              "Failed to store a node - a node with this unique ID already exists: "+node.getUid());
//        }
//        if (node.getName() != null && existsByName(node.getName())) {
//          throw new LogPlayerException(
//              "Failed to store a node - a node with the same 'well known' name already exists: "+node.getName());        
//        }
//      } 
////      else if (insertMode == InsertMode.INSERT_PERFORMANCE) {
////        logger.info("Running in performance mode");
////      }
//
//      DBObject dbObject = marshaller.serialize(node);
//      col.insert(dbObject);
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
//                  "Inserted a total of\t{0}\tNode documents. "
//                  + "Total time\t{1}\t seconds. Seconds since last block: {2}",
//                  new Object[]{insertCount, totalSeconds, secondsPerBlock});
//          timestampOfLastPerformanceMessage = now;
//        }
//      }
//      /////// DEBUG (Performance info) (end)
//    }
//    catch(Exception e)
//    {
//      throw new LogPlayerException("Failed to store item: "+node, e);
//    }
//  }
// 
  

 
//  
//  @Override
//  public Node getNodeByUid(String nodeUid)
//      throws LogPlayerException
//  {
//    DBObject query = null;
//    try {
////      logger.log(Level.INFO, "Getting node by UID: {0}", nodeUid);
//      query = new BasicDBObject();
//      query.put(FIELD_UID, nodeUid);
//
//
//      DBObject nodeObj = col.findOne(query);
//      if (nodeObj == null) {
//        return null;
//      }
//      Node node = marshaller.deserialize(nodeObj, Node.class);
//      return node;
//    }
//    catch(Exception e) {
//      throw new LogPlayerException("Failed to perform database operation: \n"
//          + "Query: "+query, e);
//    }
//  }
  

  
//  @Override
//  public Node findNodeByName(String nodeName)
//      throws LogPlayerException
//  {
//    DBObject query = null;
//    try {
////      logger.log(Level.INFO, "Getting node by name: {0}", nodeName);
//      query = new BasicDBObject();
//      query.put(FIELD_NAME, nodeName);
//
//      DBObject nodeObj = col.findOne(query);
//      if (nodeObj == null) {
//        return null;
//      }
//      Node node = marshaller.deserialize(nodeObj, Node.class);
//      return node;
//    }
//    catch(Exception e) {
//      throw new LogPlayerException("Failed to perform database operation: \n"
//          + "Query: "+query, e);
//    }
//  }
  


  

  

  

  

  

  
//  @Override
//  public Node deleteNodeByUid(String nodeUid)
//      throws LogPlayerException
//  {
//    DBObject query = null;
//    try {
////      logger.log(Level.INFO, "Deleting node by UID: {0}", nodeUid);
//      Node toDelete = getNodeByUid(nodeUid);
//      if (toDelete == null) {
//        throw new LogPlayerException(
//            "Attempted a node delete operation, but no such node exists: "+nodeUid);
//      }
//
//      /*
//       * Below code is commented for now, but we need to implement this at 
//       * some point for consistency reasons.
//       */
//      // Check that this node doesn't connect to any others
////      if (!toDelete.getOutgoingEdges().isEmpty())
////      {
////        throw new LogPlayerException(
////            "Attempted to delete node: "+nodeUid
////            + ". However, the node contains outgoing edges to other nodes."
////            + " Delete these first before attempting to remote this node.");
////      }
//      
//      // Check that this node is not connected to by others
////      if (!toDelete.getIncomingEdgeIds().isEmpty())
////      {
////        throw new LogPlayerException(
////            "Attempted to delete node: "+nodeUid
////            + ". However, the node contains incoming edges from other nodes."
////            + " Delete these first before attempting to remote this node.");
////      }
//      
//      //Delete the specified node
//      query = new BasicDBObject();
//      query.put(FIELD_UID, nodeUid);
//      
//      WriteResult result = col.remove(query);
////      logger.log(Level.INFO, "WriteResult: {0}", result.toString());
//
//      return toDelete;
//    }
//    catch(Exception e) {
//      throw new LogPlayerException("Failed to perform database operation: \n"
//          + "Query: "+query, e);
//    }    
//  }

//  @Override
//  public Iterable<Node> iterateAllNodes()
//      throws LogPlayerException
//  {
//    DBObject query = null;
//    try {
//      logger.info("Iterating all nodes");
//      //Empty 'query' selects all documents (nodes in this case)
//      query = new BasicDBObject();
//
//      final DBCursor cursor = col.find(query);
////      return new DeserialisingIterable<>(cursor, new JacksonDBObjectMarshaller<>(Node.class));
//      return new DeserialisingIterable<>(cursor, marshaller, Node.class);
//    }
//    catch(Exception e) {
//      throw new LogPlayerException("Failed to perform database operation: \n"
//          + "Query: "+query, e);
//    }
//  }
  


}
