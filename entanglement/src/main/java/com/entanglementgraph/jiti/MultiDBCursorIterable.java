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
import java.util.logging.Logger;

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
  private static final Logger logger = Logger.getLogger(MultiDBCursorIterable.class.getName());
  private static final MergePolicy DEFAULT_MERGE_POLICY = MergePolicy.APPEND_NEW__LEAVE_EXISTING;

  public static enum EntityType {
    NODE, EDGE;
  }

  /**
   * the set of graphs that must be integrated
   */
  private final SortedSet<GraphConnection> graphs;

  /**
   * Where documents in different graphs need to be merged, AND those documents contain the same fields, then this
   * policy specifies which conflict resolution strategy is used.
   */
  private MergePolicy mergePolicy;
  private final EntityKeysMerger keyMerger;
  private final NodeMerger nodeMerger;

  private final Set<DBCursor> dbCursors;
  private final EntityType type;

  private final DbObjectMarshaller marshaller;
//  private final Class<T> type;

//  private final Set<EntityKeys> seenKeys;

  private final Set<String> seenUids;
  private final Map<String, Set<String>> typeToNames;


  /**
   * An Iterable that is capable of providing an integrated view of graph entities whose content may be spread across
   * multiple documents stored in multiple independent graphs.
   *
   * @param marshaller a marshaller to use for serialising/deserialising objects/JSON strings.
   * @param graphs the independent graphs containing data to be integrated. These graph connections are queried while
   *               iterating the source DBCursors in order to obtain and merge relevant documents.
   * @param dbCursors a set of MongoDB DBCursor objects, one for each independent graph forming the integrated view.
   *                  We assume that each item in the <code>dbCursors</code> set is a result set of the <i>same</i>
   *                  query, executed over <i>different</i> independent graphs.
   */
  public MultiDBCursorIterable(DbObjectMarshaller marshaller, SortedSet<GraphConnection> graphs,
                               Set<DBCursor> dbCursors, EntityType type)
  {
    this.graphs = graphs;
    this.dbCursors = dbCursors;
    this.marshaller = marshaller;
    nodeMerger = new NodeMerger(marshaller);
    keyMerger = new EntityKeysMerger();

    this.type = type;

//    this.seenKeys = new HashSet<>();
    this.seenUids = new HashSet<>();
    this.typeToNames = new HashMap<>();

    this.mergePolicy = DEFAULT_MERGE_POLICY;
  }

//  private EntityKeys findAndMergeEntityKeys(EntityKeys initialKeys) {
//    // Query all connections for this key. Merge results
//  }

  /**
   * This method takes a keyset (<code>initialKeys</code>), which is presumably an item of a result set.
   * The purpose of this method is to present a unified view of the graph entity identified by <code>initialKeys</code>.
   * To construct this view, multiple documents from the set of graphs that form the integrated view may need to be
   * combined where there is UID or name overlap between the entities.
   *
   * However, as documents from different graphs are merged together, it is possible that new UIDs and/or names for the
   * entity are found (IDs that weren't in the original <code>initialKeys</code> keyset. If new forms of identification
   * are found, we must then go back and repeat the lookup over all graphs with these new names. If new documents are
   * found as a result of adding these new names, then these documents must also be merged into the view. Again, if
   * new IDs/names are found along with the new documents, we need to iteratively query and merge until no more documents
   * and no more IDs/names are found.
   *
   * In the most extreme case, it is possible that several documents from the <b>same</b> graph may need to be
   * combined, due to UID or name overlap. This occurs when the integrated set of graphs give us increased information.
   * Two or more documents in a given collection that, when examined in isolation, appear as separate graph entities may
   * in fact be merged into a single object when data from an additional graph provides a link between the documents.
   *
   * @param initialKeys the initial EntityKeys to search for. This initial keyset might get merged with other keysets
   *                    if results from querying find new IDs or names that refer to the same graph entity.
   * @return a merged graph entity that represents an integration of all appropriate documents stored in all the graphs
   * that are currently being integrated.
   * @throws GraphModelException
   * @throws DbObjectMarshallerException
   */
  private BasicDBObject findAndMergeAllMatchingObjects(EntityKeys initialKeys) throws GraphModelException, DbObjectMarshallerException {
    // Query all connections for this key. Merge results
    boolean done = false; // Set true when all documents have been retrieved and merged.

    EntityKeys currentKeyset = initialKeys.clone();
    BasicDBObject mergedNode = null;
    startAgainWithMergedKeyset:
    while (!done) {
      /*
       * For each graph to be integrated in this view, find and merge documents matching 'currentKeyset'. During this
       * process, if new entity identification keys are found then we need to merge the EntityKeys, and retry the
       * document queries from the beginning.
       */
      for (GraphConnection graphConn : graphs) {
        BasicDBObject graphDocument = graphConn.getNodeDao().getByKey(currentKeyset);
        EntityKeys graphKeyset = MongoUtils.parseKeyset(marshaller, graphDocument);
        if (!areKeysetsIdentical(currentKeyset, graphKeyset)) {
          mergedNode = null; // Reset document
          currentKeyset = keyMerger.merge(currentKeyset, graphKeyset); //Merge keysets
          logger.info("Found new keys for this graph entity, repeating query from the start with the new, merged " +
              "EntityKeys to be sure we integrate everything: "+currentKeyset);
          continue startAgainWithMergedKeyset;
        } else {
          logger.info("Found new document for the current keyset. Merging this document for the integrated view.");
          if (mergedNode == null) {
            mergedNode = graphDocument;
          } else {
            mergedNode = nodeMerger.merge(mergePolicy, mergedNode, graphDocument);
          }
        }
      }
      done = true; // We successfully queried all graphs, and didn't find any new keyset items.
    }
    return mergedNode;
  }

  /**
   * Returns true if two keysets are of the same type, and have identical UID and name content.
   * @param first
   * @param second
   * @return
   */
  private boolean areKeysetsIdentical(EntityKeys first, EntityKeys second) throws IllegalArgumentException {
    if (!first.getType().equals(second.getType())) {
      throw new IllegalArgumentException("Attempt to compare keysets with different type names: "+first+"; "+second);
    }
    return first.getUids().equals(second.getUids())
        && first.getNames().equals(second.getNames());

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
