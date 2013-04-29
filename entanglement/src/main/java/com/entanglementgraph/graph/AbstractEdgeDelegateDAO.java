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

package com.entanglementgraph.graph;

import com.entanglementgraph.graph.data.EntityKeys;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is useful if you already have a EdgeDAO in your application
 * and wish to create a data-specific subclass DAO, but make use of the same 
 * internal configuration / database connections, etc.
 * 
 * Simply subclass this class in your application, and then add app-specific
 * methods to that.
 * 
 * @author Keith Flanagan
 */
 public class AbstractEdgeDelegateDAO
    extends AbstractGraphEntityDelegateDAO
    implements EdgeDAO
{
  private final EdgeDAO delegate;

  public AbstractEdgeDelegateDAO(EdgeDAO delegate) {
    super(delegate);
    this.delegate = delegate;
  }

  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(EntityKeys fromNode, EntityKeys to) throws GraphModelException {
    return delegate.iterateEdgesBetweenNodes(fromNode, to);
  }

  @Override
  public Iterable<DBObject> iterateEdgesBetweenNodes(String edgeType, EntityKeys from, EntityKeys to) throws GraphModelException {
    return delegate.iterateEdgesBetweenNodes(edgeType, from, to);
  }

  @Override
  public Iterable<DBObject> iterateEdgesFromNode(EntityKeys from) throws GraphModelException {
    return delegate.iterateEdgesFromNode(from);
  }

  @Override
  public Iterable<DBObject> iterateEdgesFromNode(String edgeType, EntityKeys from) throws GraphModelException {
    return delegate.iterateEdgesFromNode(edgeType, from);
  }

  @Override
  public Iterable<DBObject> iterateEdgesToNode(EntityKeys to) throws GraphModelException {
    return delegate.iterateEdgesToNode(to);
  }

  @Override
  public boolean existsEdgeToNodeOfType(EntityKeys from, String toNodeType) throws GraphModelException {
    return delegate.existsEdgeToNodeOfType(from, toNodeType);
  }

  @Override
  public Long countEdgesFromNode(EntityKeys from) throws GraphModelException {
    return delegate.countEdgesFromNode(from);
  }

  @Override
  public Long countEdgesOfTypeFromNode(String edgeType, EntityKeys from) throws GraphModelException {
    return delegate.countEdgesOfTypeFromNode(edgeType, from);
  }

  @Override
  public Long countEdgesToNode(EntityKeys to) throws GraphModelException {
    return delegate.countEdgesToNode(to);
  }

  @Override
  public Long countEdgesOfTypeToNode(String edgeType, EntityKeys to) throws GraphModelException {
    return delegate.countEdgesOfTypeToNode(edgeType, to);
  }

  @Override
  public Long countEdgesOfTypeBetweenNodes(String edgeType, EntityKeys from, EntityKeys to) throws GraphModelException {
    return delegate.countEdgesOfTypeBetweenNodes(edgeType, from, to);
  }

  @Override
  public Map<String, Long> countEdgesByTypeFromNode(EntityKeys from) throws GraphModelException {
    return delegate.countEdgesByTypeFromNode(from);
  }

  @Override
  public Map<String, Long> countEdgesByTypeToNode(EntityKeys to) throws GraphModelException {
    return delegate.countEdgesByTypeToNode(to);
  }
}
