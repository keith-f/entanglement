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
import com.mongodb.DBObject;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshallerException;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 17/03/2013
 * Time: 17:32
 * To change this template use File | Settings | File Templates.
 */
public class MongoObjectParsers {

  public static EntityKeys parseKeyset(DbObjectMarshaller marshaller, String jsonKeyset)
      throws DbObjectMarshallerException {
    EntityKeys keyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);
    return keyset;
  }

  public static EntityKeys parseKeyset(DbObjectMarshaller marshaller, DBObject dbObject)
      throws DbObjectMarshallerException {
    String jsonKeyset = dbObject.get(FIELD_KEYS).toString();
    EntityKeys keyset = marshaller.deserialize(jsonKeyset, EntityKeys.class);
    return keyset;
  }

}
