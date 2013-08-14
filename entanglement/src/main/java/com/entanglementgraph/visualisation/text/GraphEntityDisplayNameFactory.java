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
package com.entanglementgraph.visualisation.text;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.GraphEntity;

/**
 * Graph entities may need to be displayed to users. In some cases, entities may have a combination of UIDs and/or
 * names. Some of the IDs may not be meaningful for visualisation purposes (for example, globally unique IDs, or
 * IDs from internal relational databases). This interface allows implementations to produce a customisable entity name
 * by choosing from one or more of the available bean properties, and provides an opportunity for filtering out
 * 'unfriendly' names for display purposes. The resulting display name should <i>not</i> be considered to be unique
 * in any way.
 *
 * Examples of use include: a preferred gene name, well-known chromosome name, etc.
 *
 * Two methods are provided. One generates names for entities when only the <code>EntityKeys</code> is known. A
 * second method generates names for cases where the entire data bean is available.
 *
 * User: keith
 * Date: 14/08/13; 14:41
 *
 * @author Keith Flanagan
 */
public interface GraphEntityDisplayNameFactory<T extends GraphEntity> {
  public String getDisplayName(EntityKeys<T> entityKeys);
  public String getDisplayName(T entityInstance);
}
