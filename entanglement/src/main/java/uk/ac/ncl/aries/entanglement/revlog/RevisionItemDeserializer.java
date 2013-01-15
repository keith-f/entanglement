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

package uk.ac.ncl.aries.entanglement.revlog;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.torrenttamer.util.GenericServiceLoader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNode;
import uk.ac.ncl.aries.entanglement.revlog.commands.GraphOperation;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 * This class is a Gson deserializer for instances of <code>RevisionItem</code>.
 * We need a custom deserializer here because <code>RevisionItem</code> has a
 * property of type <code>GraphOperation</code>, which is an abstract type. 
 * Therefore, the generic deserializer is unable to tell which subclass
 * should be used.
 * 
 * This deserializer uses additional information (a type name) stored as a 
 * property on the <code>RevisionItem</code> in order to instantiate the 
 * correct class form an SPI lookup. 
 * 
 * The following is an example of a <code>RevisionItem</code> subdocument 
 * parsed by this deserializer. These are typically a list on another class,
 * such as a <code>RevisionItemContainer</code>.
 * 
 * <pre>
 *		{
 *			"type" : "CreateNode2IfNotExistsByName",
 *			"op" : {
 *				"some_operation_specific property" : {
 *             .....
 *				},
 *				"pTags" : [ ],
 *				"pStrings" : [ ]
 *			}
 *		},
 * </pre>
 * 
 * @author Keith Flanagan
 */
public class RevisionItemDeserializer 
    implements JsonDeserializer<RevisionItem>
{ 
  // SPI loader for GraphOperation. Used for lookup up type names.
  private final GenericServiceLoader<GraphOperation> loader;
  // A cache of typename -> graph operation type to avoid loader lookps.
  private final Map<String, Class<? extends GraphOperation>> opTypeCache;
  
  public RevisionItemDeserializer()
  {
    loader = new GenericServiceLoader<>(GraphOperation.class);
    opTypeCache = new HashMap<>();
  }
  
  @Override
  public RevisionItem deserialize(JsonElement je, Type type, JsonDeserializationContext jdc)
          throws JsonParseException {
    String typeName = null;
    try {
//      System.out.println("je: "+je);
//      System.out.println("Type: "+type);
//      System.out.println("Context: "+jdc);

      JsonObject rootObj = je.getAsJsonObject();
      typeName = rootObj.get("type").getAsString();
      Class cl = getOperationTypeForName(typeName);
      GraphOperation graphOp = jdc.deserialize(rootObj.get("op"), cl);
      
      RevisionItem revItem = new RevisionItem();
      revItem.setType(typeName);
      revItem.setOp(graphOp);
      
      return revItem;
    }
    catch(Exception e) {
      throw new JsonParseException(
              "Failed to parse subdocument of abstract type: "+type
              + " to: " + typeName + ". Document was:\n" + je, e);
    }
  }
  
  /**
   * Uses an SPI of <code>GraphOperation</code> to locate a Java class for the
   * deserializer that represents the actual graph operation bean type. 
   * The type name, <code>typeName</code> may either be the fully qualified 
   * classname, or optionally, just the class 'simple name'. The reason we allow
   * both is to reduce database disk space consumption for large graphs where
   * billions of operations might be present. When using a 'short'.
   * 
   * This method uses an in-memory cache, so expensive lookup operations are
   * only done once per operation type.
   * 
   * <code>typeName</code>, you should ensure that the names are unique.
   * 
   * @param typeName Either the fully qualified classname, or the class 'simple
   * name' of the graph operation type.
   * @return the Class of the operation type that matches the string <code>typeName</code>.
   */
  private Class<? extends GraphOperation> getOperationTypeForName(String typeName)
          throws ClassNotFoundException
  {
    if (opTypeCache.keySet().contains(typeName)) {
      return opTypeCache.get(typeName);
    }
    
    GraphOperation op = loader.findByClassNameOrSimpleName(typeName);
    if (op == null) {
      throw new ClassNotFoundException("No "+GraphOperation.class.getName() 
              + " inplmentation could be found to match type name: " + typeName
              + ". Please check your SPI definition file.");
    }
    opTypeCache.put(typeName, op.getClass());
    return op.getClass();
  }

}
