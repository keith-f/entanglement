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
 * File created: 28-Nov-2012, 13:25:18
 */

package com.entanglementgraph.jiti;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.jiti.NodeMerger;
import com.entanglementgraph.revlog.commands.MergePolicy;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.util.MongoUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * Provides a single Iterable over multiple DBCursors, for use in the case where we're integrating over several
 * datasets in different graphs. In this case:
 * <ul>
 *   <li>We have multiple DBCursor objects (because we've sent the same query to all graphs we wish to integrate)</li>
 *   <li>We need to (in turn) iterate over each DBCursor in order to ensure that we return results from all graphs.</li>
 *   <li>When iterating any given DBCursor, we need to query all graphs involved (in case the same entity, or
 *   different parts of the same entity exist in different graphs). If more than one document exists in this result
 *   set, we need to merge them before returning them to the user.</li>
 * </ul>
 *
 * In order to ensure that the user receives each merged entity once (rather than multiple times, if documents with
 * matching EntityKeys exist in multiple result sets), we store the iterated EntityKeys in memory. If an entity has
 * already been returned to the user, it won't be returned again. For large result sets, this currently requires a
 * large in-memory list of EntityKeys. A future implementation may store this set of 'seen' keys in a temporary
 * MongoDB collection.
 * TODO this implementation isn't yet suitable for massive result sets. For now, make use of offset/limit in queries.
 *
 * This class also allows the use of 'offset' and 'limit' over multiple graphs.
 * Here, we assume that the provided <code>GraphConnection</code> objects are ordered.
 *
 * @author Keith Flanagan
 */
public class MultiDBCursorIterable
    implements Iterable<DBObject>, Closeable
{
  public static enum EntityType {
    NODE, EDGE;
  }


  private final SortedSet<GraphConnection> graphs;
  private MergePolicy mergePolicy;
  private final NodeMerger nodeMerger;

  private final Set<DBCursor> dbCursors;
  private final EntityType type;

  private final DbObjectMarshaller marshaller;
//  private final Class<T> type;

//  private final Set<EntityKeys> seenKeys;

  private final Set<String> seenUids;
  private final Map<String, Set<String>> typeToNames;


  public MultiDBCursorIterable(DbObjectMarshaller marshaller, SortedSet<GraphConnection> graphs,
                               Set<DBCursor> dbCursors, EntityType type )
//                               Class<T> type)
  {
    this.graphs = graphs;
    this.dbCursors = dbCursors;
    this.marshaller = marshaller;
    nodeMerger = new NodeMerger(marshaller);

    this.type = type;

//    this.seenKeys = new HashSet<>();
    this.seenUids = new HashSet<>();
    this.typeToNames = new HashMap<>();
  }

  @Override
  public Iterator<DBObject> iterator()
  {
//    final Iterator<DBObject> dbItr = dbIterable.iterator();
    // An Iterator over the Iterables...
    final Iterator<DBCursor> cursorItr = dbCursors.iterator();
    
    return new Iterator<DBObject>() {
      Iterator<DBObject> dbItr = null;// = dbIterable.iterator();

      private void checkCursors() {
        // If there is a current cursor, and there are more elements, then everything is fine
        if (dbItr != null && dbItr.hasNext()) {
          return;
        }

        // Choose the first available cursor that has elements to return
        while(cursorItr.hasNext()) {
          dbItr = cursorItr.next().iterator();
          if (dbItr.hasNext()) {
            return;
          }
        }

        //We reached the end of DBCursors
        dbItr = null;
      }

      @Override
      public boolean hasNext()
      {
        checkCursors();
        return dbItr != null;
      }
      
      @Override
      public DBObject next()
      {
        checkCursors();
        if (dbItr == null) {
          throw new RuntimeException("No more elements in the resultset!");
        }
        try {
//          DBObject dbObject = dbItr.next();
//
//          //Find and merge with objects in other graphs
//          EntityKeys existingKeyset = MongoUtils.parseKeyset(marshaller, dbObject.getString(FIELD_KEYS));

        }
        catch(Exception e) {
//          throw new RuntimeException(
//            "Failed to obtain / deserialize the next object in the iterator. \n"
//            + "Deserializer implementation was: "+marshaller.getClass().getName()+"\n"
//            + "DBObject to be deserialized was: "+dbObject, e);
        }
        return null;
      }
      
      @Override
      public void remove()
      {
        throw new UnsupportedOperationException("This iterator does not support remove()");
      }



      /**
       * This method takes an EntityKeys (presumably extracted from the current DBCursor). It then queries
       * every source graph for the EntityKeys
       * @return
       */
      private DBObject searchAndMergeNextElement(DBObject next) throws DbObjectMarshallerException, GraphModelException {
        // i. Extract the EntityKeys from the DBObject returned from the current DBCursor
        EntityKeys nextKeyset = MongoUtils.parseKeyset(marshaller, next);

        // ii. Check the current (unmerged) EntityKeys to see if we've encountered at least one of its IDs before
        if (keyElementsSeenBefore(nextKeyset)) {
          // If yes, skip to the next element.
          return null;
        }

        // iii. Perform a lookup query using the EntityKeys over all source graphs
        Map<GraphConnection, BasicDBObject> resultsFromAllGraphs = new HashMap<>();
        //TODO we could parallelise the lookup operations in this loop
        for (GraphConnection conn : graphs) {
          if (type == EntityType.NODE) {
            BasicDBObject found = conn.getNodeDao().getByKey(nextKeyset);
            resultsFromAllGraphs.put(conn, found);
          } else {
            BasicDBObject found = conn.getEdgeDao().getByKey(nextKeyset);
            resultsFromAllGraphs.put(conn, found);
          }
        }

        BasicDBObject merged = mergeNodeDBObjects(resultsFromAllGraphs);
        EntityKeys mergedKeyset = MongoUtils.parseKeyset(marshaller, merged);

        // Are there any more UIDs and/or names in the mergedKeyset compared to the nextKeyset?
        // If no, then we're done. Return the merged object.
        if (nextKeyset.getUids().size() == mergedKeyset.getUids().size() &&
            nextKeyset.getNames().size() == mergedKeyset.getNames().size()) {
          return merged;
        }

        // If yes, and any of the new elements have been seen before (Iterator cache), then we're done. Return null.
        // Otherwise (yes + new elements haven't been seen before), repeat the query with the new elements.
        // ^^^ Both these cases are handled by recursively calling this method with the merged keyset.
        return searchAndMergeNextElement(merged);
      }

      private BasicDBObject mergeNodeDBObjects(Map<GraphConnection, BasicDBObject> results) throws GraphModelException {
        BasicDBObject merged = null;
        for (GraphConnection conn : graphs) {
          BasicDBObject result = results.get(conn);
          if (result == null) {
            // No result from this graph
            continue;
          }
          if (merged == null) {
            //First result populates 'merged'
            merged = result;
          } else {
            //Further results need to be merged into the existing result.
            merged = nodeMerger.mergeNodes(mergePolicy, merged, result);
          }
        }
        return merged;
      }

      /**
       * This method checks to see if any of the elements in the entity keys have been seen before by this Iterator
       * @param keys
       * @return
       */
      private boolean keyElementsSeenBefore(EntityKeys keys) {
        if (seenUids.containsAll(keys.getUids())) {
          return true;
        }
        Set<String> namesForType = typeToNames.get(keys.getType());
        if (namesForType != null && namesForType.containsAll(keys.getNames())) {
          return true;
        }
        return false;
      }
    };
  }

  @Override
  public void close() throws IOException {
    for (DBCursor cursor : dbCursors) {
      cursor.close();
    }

  }
}
