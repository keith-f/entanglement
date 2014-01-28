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

package com.entanglementgraph.graph.mongodb;

import com.entanglementgraph.graph.EntityKeys;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.logging.Logger;

import static com.entanglementgraph.graph.EdgeDAO.*;
import static com.entanglementgraph.graph.mongodb.MongoUtils.list;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 29/04/2013
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
public class EdgeQueries {
  private static final Logger logger = Logger.getLogger(EdgeQueries.class.getName());

  /**
   * Graph entities can be uniquely identified either by one of their UID strings, or by their type combined with one
   * of their names. Edge queries involving both the 'from' and 'to' nodes must therefore perform several database
   * queries in order to ensure that all appropriate edge documents are found. These are:
   * <ul>
   * <li>from (uid) + to (uid)</li>
   * <li>from (uid) + to (type+name)</li>
   * <li>from (type+name) + to (uid)</li>
   * <li>from (type+name) + to (type+name)</li>
   * </ul>
   * <p/>
   * Helpfully, MongoDB provides the <code>$or</code> operator. Not only does this operator get around the usual
   * limit of one index per query, but each part of the 'or' operation executes in parallel.
   * <p/>
   * This method constructs a query along the lines of the following, which can be used to query the edges between two
   * nodes with all possible graph entity addressing combinations:
   * <pre>
   * $or: [ { from.uids: AAA, to.uids: BBB }, { from.names: YYY, from.type: ZZZ, to.names: WWW, to.type : XXX }, ..., ... ]
   * </pre>
   *
   * @return
   */
  public static DBObject buildFromToNodeQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);

    or.add(buildFromUidToUidQuery(from, to));
    or.add(buildFromUidToNameQuery(from, to));
    or.add(buildFromNameToUidQuery(from, to));
    or.add(buildFromNameToNameQuery(from, to));

    return query;
  }

  /**
   * Similar to <code>buildFromToNodeQuery</code> but returns a query that will find edges from <code>first</code>
   * to <code>second</code> as well as edges from <code>second</code> to <code>first.</code>
   * @param first the first node
   * @param second the second node
   * @return
   */
  public static DBObject buildBidirectionalFromToNodeQuery(EntityKeys first, EntityKeys second) {
    DBObject query = new BasicDBObject();

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);

    // From the first node to the second
    or.add(buildFromUidToUidQuery(first, second));
    or.add(buildFromUidToNameQuery(first, second));
    or.add(buildFromNameToUidQuery(first, second));
    or.add(buildFromNameToNameQuery(first, second));

    // From the second node to the first
    or.add(buildFromUidToUidQuery(second, first));
    or.add(buildFromUidToNameQuery(second, first));
    or.add(buildFromNameToUidQuery(second, first));
    or.add(buildFromNameToNameQuery(second, first));

    return query;
  }

  /**
   * There are two ways to address a graph entity - by UID or by type+name. This method builds a query that returns
   * edges starting at a specified <code>from</code> node, using either identification method. MongoDB executes
   * both parts of this $or query in parallel.
   *
   * @param from
   * @return
   */
  public static DBObject buildFromNodeQuery(EntityKeys from) {
    DBObject query = new BasicDBObject();

    DBObject uidQuery = new BasicDBObject();
    uidQuery.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));

    DBObject nameQuery = new BasicDBObject();
    nameQuery.put(FIELD_FROM_KEYS_NAMES, new BasicDBObject("$in", list(from.getNames())));
    nameQuery.put(FIELD_FROM_KEYS_TYPE, from.getType());

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);
    or.add(uidQuery);
    or.add(nameQuery);

    return query;
  }

  /**
   * There are two ways to address a graph entity - by UID or by type+name. This method builds a query that returns
   * edges ending at a specified <code>to</code> node, using either identification method. MongoDB executes
   * both parts of this $or query in parallel.
   *
   * @param to
   * @return
   */
  public static DBObject buildToNodeQuery(EntityKeys to) {
    DBObject query = new BasicDBObject();

    DBObject uidQuery = new BasicDBObject();
    uidQuery.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));

    DBObject nameQuery = new BasicDBObject();
    nameQuery.put(FIELD_TO_KEYS_NAMES, new BasicDBObject("$in", list(to.getNames())));
    nameQuery.put(FIELD_TO_KEYS_TYPE, to.getType());

    //The outer '$or':
    BasicDBList or = new BasicDBList();
    query.put("$or", or);
    or.add(uidQuery);
    or.add(nameQuery);
    logger.fine("buildToNodeQuery: " + query);

    return query;
  }

  public static DBObject buildFromUidToUidQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));
    return query;
  }

  public static DBObject buildFromUidToNameQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_UIDS, new BasicDBObject("$in", list(from.getUids())));
    query.put(FIELD_TO_KEYS_NAMES, new BasicDBObject("$in", list(to.getNames())));
    query.put(FIELD_TO_KEYS_TYPE, to.getType());
    return query;
  }

  public static DBObject buildFromNameToUidQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_NAMES, new BasicDBObject("$in", list(from.getNames())));
    query.put(FIELD_FROM_KEYS_TYPE, from.getType());
    query.put(FIELD_TO_KEYS_UIDS, new BasicDBObject("$in", list(to.getUids())));
    return query;
  }

  public static DBObject buildFromNameToNameQuery(EntityKeys from, EntityKeys to) {
    DBObject query = new BasicDBObject();
    query.put(FIELD_FROM_KEYS_NAMES, new BasicDBObject("$in", list(from.getNames())));
    query.put(FIELD_FROM_KEYS_TYPE, from.getType());
    query.put(FIELD_TO_KEYS_NAMES, new BasicDBObject("$in", list(to.getNames())));
    query.put(FIELD_TO_KEYS_TYPE, to.getType());
    return query;
  }

}
