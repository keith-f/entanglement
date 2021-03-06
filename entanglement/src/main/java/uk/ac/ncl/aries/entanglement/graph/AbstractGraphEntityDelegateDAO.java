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

package uk.ac.ncl.aries.entanglement.graph;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.List;

/**
 * This is useful if you already have a NodeDAO or EdgeDAO in your application 
 * and wish to create a data-specific subclass DAO, but make use of the same 
 * internal configuration / database connections, etc.
 * 
 * Simply subclass this class in your application, and then add app-specific
 * methods to that. 
 * 
 * TODO: we may need further delegate classes that implement NodeDAO and EdgeDAO,
 * but this shouldn't be difficult, since those delegates can extend this one.
 * 
 * @author Keith Flanagan
 */
abstract public class AbstractGraphEntityDelegateDAO
    implements GraphEntityDAO
{
  private final GraphEntityDAO delegate;

  public AbstractGraphEntityDelegateDAO(GraphEntityDAO delegate) {
    this.delegate = delegate;
  }

  @Override
  public InsertMode getInsertModeHint() {
    return delegate.getInsertModeHint();
  }

  @Override
  public void setInsertModeHint(InsertMode mode) {
    delegate.setInsertModeHint(mode);
  }

  @Override
  public DBCollection getCollection() {
    return delegate.getCollection();
  }

  @Override
  public void store(BasicDBObject entity) throws GraphModelException {
    delegate.store(entity);
  }

  @Override
  public void setPropertyByUid(String uid, String propertyName, Object propertyValue) throws GraphModelException {
    delegate.setPropertyByUid(uid, propertyName, propertyValue);
  }

  @Override
  public void setPropertyByName(String entityType, String entityName, String propertyName, Object propertyValue) throws GraphModelException {
    delegate.setPropertyByName(entityType, entityName, propertyName, propertyValue);
  }

  @Override
  public String lookupUniqueIdForName(String entityType, String entityName) throws GraphModelException {
    return delegate.lookupUniqueIdForName(entityType, entityName);
  }

  @Override
  public DBObject getByUid(String uid) throws GraphModelException {
    return delegate.getByUid(uid);
  }

  @Override
  public DBObject getByName(String entityType, String entityName) throws GraphModelException {
    return delegate.getByName(entityType, entityName);
  }

  @Override
  public boolean existsByUid(String uniqueId) throws GraphModelException {
    return delegate.existsByUid(uniqueId);
  }

  @Override
  public boolean existsByName(String entityType, String entityName) throws GraphModelException {
    return delegate.existsByName(entityType, entityName);
  }

  @Override
  public DBObject deleteByUid(String uid) throws GraphModelException {
    return delegate.deleteByUid(uid);
  }

  @Override
  public DBCursor iterateAll() throws GraphModelException {
    return delegate.iterateAll();
  }

  @Override
  public List<String> listTypes() throws GraphModelException {
    return delegate.listTypes();
  }

  @Override
  public Iterable<DBObject> iterateByType(String typeName) throws GraphModelException {
    return delegate.iterateByType(typeName);
  }

  @Override
  public Iterable<String> iterateIdsByType(String typeName, int offset, int limit) throws GraphModelException {
    return delegate.iterateIdsByType(typeName, offset, limit);
  }

  @Override
  public Iterable<String> iterateNamesByType(String typeName, int offset, int limit) throws GraphModelException {
    return delegate.iterateNamesByType(typeName, offset, limit);
  }

  @Override
  public long countByType(String typeName) throws GraphModelException {
    return delegate.countByType(typeName);
  }

  @Override
  public long count() throws GraphModelException {
    return delegate.count();
  }

}
