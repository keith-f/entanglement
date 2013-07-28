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

package com.entanglementgraph.util;

import static com.entanglementgraph.graph.GraphEntityDAO.FIELD_KEYS;
import com.entanglementgraph.graph.data.EntityKeys;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import javax.lang.model.type.PrimitiveType;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 17/03/2013
 * Time: 17:32
 * To change this template use File | Settings | File Templates.
 */
public class MongoUtils {
  private static final Logger logger = Logger.getLogger(MongoUtils.class.getName());

  public static void createIndexes(DBCollection col, Iterable<DBObject> indexDefns) {
    for (DBObject idx : indexDefns) {
      col.ensureIndex(idx);
    }
  }

  public static BasicDBList singleton(String item) {
    BasicDBList list = new BasicDBList();
    list.add(item);
    return list;
  }

//  public static BasicDBList list(Collection<String> items) {
//    BasicDBList list = new BasicDBList();
//    list.addAll(items);
//    return list;
//  }

  public static BasicDBList list(Collection items) {
    BasicDBList list = new BasicDBList();
    list.addAll(items);
    return list;
  }

  /**
   * Extracts an EntityKeys object by directly parsing the JSON-format string, <code>jsonKeyset</code>.
   *
   * @param marshaller the object marshaller to use for deserialisation.
   * @param jsonKeyset the JSON String that encodes an <code>EntityKeys</code> bean.
   * @return an <code>EntityKeys</code> object parsed from the JSON text.
   * @throws DbObjectMarshallerException
   */
  public static EntityKeys parseKeyset(DbObjectMarshaller marshaller, String jsonKeyset)
      throws DbObjectMarshallerException {
    EntityKeys keyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);
    return keyset;
  }

  /**
   * Extracts an EntityKeys object from a DBObject. The value of <code>FIELD_KEYS</code> (usually "keys") is
   * obtained from the DBObject. This value is assumed to be a JSON text String that encodes an EntityKeys object.
   * The deserialised object is then returned.
   * @param marshaller the object marshaller to use for deserialisation.
   * @param dbObject the database object from which the "keys" field should be extracted, and its value parsed.
   * @return an <code>EntityKeys</code> object parsed from a JSON string obtained from the "keys" field of the specified
   * database object.
   * @throws DbObjectMarshallerException
   */
  public static EntityKeys parseKeyset(DbObjectMarshaller marshaller, BasicDBObject dbObject)
      throws DbObjectMarshallerException {
//    String jsonKeyset = dbObject.get(FIELD_KEYS).toString();
    String jsonKeyset = dbObject.getString(FIELD_KEYS);
    EntityKeys keyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);
    return keyset;
  }

  /**
   * Extracts an EntityKeys object from a user-specified field of a DBObject. The value of the DBObject field is
   * assumed to be a JSON text String that encodes an EntityKeys object. This deserialised object obtained from this
   * JSON value is then returned.
   *
   * This method is convenient when extracting a specific EntityKey instance from objects with multiple EntityKeys
   * instances, such as edge documents. Here, suitable values for <code>fieldName</code> include:
   * <code>EdgeDAO.FIELD_FROM_KEYS</code> and <code>EdgeDAO.FIELD_TO_KEYS</code>
   *
   * @param marshaller the object marshaller to use for deserialisation.
   * @param dbObject the database object from which the <code>fieldName</code> field should be extracted, and its value parsed.
   * @param fieldName the name of the DBObject field whose value is to be used when parsing the <code>EntityKeys</code>.
   * @return an <code>EntityKeys</code> object parsed from a JSON string obtained from the <code>fieldName</code> field
   * of the specified database object.
   * @throws DbObjectMarshallerException
   */
  public static EntityKeys parseKeyset(DbObjectMarshaller marshaller, DBObject dbObject, String fieldName)
      throws DbObjectMarshallerException {
    String jsonKeyset = com.mongodb.util.JSON.serialize(dbObject.get(fieldName));
    logger.info(String.format("Extracted keyset DBObject and serialised to: %s", jsonKeyset));
    EntityKeys keyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);
    return keyset;
  }

}
