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

package com.entanglementgraph.revlog;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import com.entanglementgraph.revlog.data.RevisionItem;

/**
 * A Gson serializer for the type <code>RevisionItem</code>. Not quite sure
 * why this is needed, but it seems to be when we specify a custom deserializer
 * <code>RevisionItemDeserializer</code>.
 * 
 * This class does nothing other than pass on the bean to the serialisation
 * context provided by the caller.
 * 
 * @author Keith Flanagan
 */
public class RevisionItemSerializer
      implements JsonSerializer<RevisionItem>
{
  @Override
  public JsonElement serialize(RevisionItem t, Type type, JsonSerializationContext jsc) {
    return jsc.serialize(t);
  }
}
