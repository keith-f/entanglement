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

package uk.ac.ncl.aries.entanglement;

import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import com.torrenttamer.mongodb.gson.GsonDBObjectMarshaller;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import uk.ac.ncl.aries.entanglement.revlog.RevisionItemDeserializer;
import uk.ac.ncl.aries.entanglement.revlog.RevisionItemSerializer;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public class ObjectMarshallerFactory
{
//  public static DbObjectMarshaller create()
//  {
//    ClassLoader defaultCl = ObjectMarshallerFactory.class.getClassLoader();
//    return create(defaultCl);
//  }
  
  public static DbObjectMarshaller create(ClassLoader classLoader)
  {
//    DbObjectMarshaller marshaller = new JacksonDBObjectMarshaller();
//    return marshaller;
    
//    GsonDBObjectMarshaller marshaller = new GsonDBObjectMarshaller();
//    return marshaller;
    
    Map<Type, Object> adapters = new HashMap<>();
//    adapters.put(GraphOperation.class, new GraphOperationDeserializer(defaultCl));
    adapters.put(RevisionItem.class, new RevisionItemSerializer());
    adapters.put(RevisionItem.class, new RevisionItemDeserializer(classLoader));
//    adapters.put(byte[].class, new ByteArraySerializer());
//    adapters.put(RevisionItem.class, new RevisionItemDeserializer(defaultCl));
    GsonDBObjectMarshaller marshaller = new GsonDBObjectMarshaller(adapters);
    return marshaller;
  }
}
