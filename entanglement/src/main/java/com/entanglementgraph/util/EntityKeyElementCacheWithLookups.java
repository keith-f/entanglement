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

import com.entanglementgraph.graph.EntityKeys;

/**
 * A cache for recently seen EntityKey instances, and their elements.
 * There is quite often a need to 'remember' that a given node or edge has been 'seen'. For instance, in graph
 * iteration or export operations, it is useful to keep track of which nodes have been visited and which have not.
 * However, it is insufficient to simply keep hold of a set of <code>EntityKey</code> references, since the
 * equivalence of two <code>EntityKey</code> instances can not necessarily be determined by <code>.equals()</code>.
 * The question is not whether two <code>EntityKey</code> instances are equal, but rather, whether they refer to
 * the same entity (either by overlapping UID or type/name pair). Remember that:
 * <ul>
 *   <li>The <code>EntityKey</code> content of a node may not be identical to the <code>EntityKey</code>
 *   content of a linking edge, and so cannot be directly compared by <code>.equals()</code></li>
 *   <li>The above is especially likely to occur in cases where different graphs have been combined into an
 *   integrated view. Here, graph entities that may have been considered to be separate, independent entities may
 *   be combined if one or more of the <code>EntityKey</code> elements are found to overlap.</li>
 * </ul>
 *
 * This interface defines a utility that can be used to temporarily store the elements of <code>EntityKey</code>
 * instances in terms of the UID and name elements. Operations are defined that allow a program to query whether
 * a graph entity has been seen before based on one or more of the <code>EntityKey</code> elements.
 *
 * Multiple implementations are possible, ranging from in-memory caches for 'small', short-term datasets. A database
 * backed implementation may be more appropriate if your graph operations need to keep track of millions of entities.
 *
 * In addition to the functionality provided by <code>EntityKeyElementCache</code>, this interface also provides
 * mappings between Entanglement entity key elements and arbitrary user objects. This functionality is useful in
 * several export operation.
 *
 * User: keith
 * Date: 13/08/13; 16:24
 *
 * @author Keith Flanagan
 */
public interface EntityKeyElementCacheWithLookups<T, U> {
  public void cacheElementsAndAssociateWithObject(EntityKeys<T> key, U userObject);
  public U getUserObjectFor(EntityKeys<T> key);
}
