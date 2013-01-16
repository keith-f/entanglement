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
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;

/**
 * Query methods that are common to both Nodes and Edges.
 * 
 * @author Keith Flanagan
 */
public interface GraphEntityDAO 
{
  public static final String FIELD_UID = "uid";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_TYPE = "type";
  
  
  public InsertMode getInsertModeHint();
  public void setInsertModeHint(InsertMode mode);
  public DBCollection getCollection();
//  public DBCollection getNodeCol();
//  public DBCollection getEdgeCol();
  
  
  
  
  public void store(BasicDBObject entity)
      throws GraphModelException;
  
  /**
   * Given a node's UID, sets the named property to the given value. Note that
   * any arbitrary property name can be given here, even if this node was
   * originally serialized from a Java bean that doesn't contain this property. 
   * 
   * @param uid the ID of the node to update
   * @param propertyName the property to set.
   * @param propertyValue a data bean to be JSON-serialised and converted into
   * a DBObject.
   * @throws LogPlayerException 
   */
  public void setPropertyByUid(String uid, String propertyName, Object propertyValue)
      throws GraphModelException;
  
  public void setPropertyByName(String entityType, String entityName, String propertyName, Object propertyValue)
      throws GraphModelException;
  
  public String lookupUniqueIdForName(String entityType, String entityName)
      throws GraphModelException;
  
  /**
   * Returns an instance by its unique ID. If no entity with the specified ID
   * exists, then null is returned.
   * @param nodeUid the ID of the entity to return
   * @return the entity with the unique ID, or null if no such entity exists.
   * @throws LogPlayerException 
   */
  public DBObject getByUid(String uid)
      throws GraphModelException;
  
  public DBObject getByName(String entityType, String entityName)
      throws GraphModelException;
  
  public boolean existsByUid(String uniqueId)
      throws GraphModelException;
  
  public boolean existsByName(String entityType, String entityName)
      throws GraphModelException;
  
  public DBObject deleteByUid(String uid)
      throws GraphModelException;
  
  
  
  public DBCursor iterateAll()
      throws GraphModelException;
  
  public List<String> listTypes()
      throws GraphModelException;
  
  public Iterable<DBObject> iterateByType(String typeName)
      throws GraphModelException;
  

  public Iterable<String> iterateIdsByType(String typeName, int offset, int limit)
      throws GraphModelException;
  
  public Iterable<String> iterateNamesByType(String typeName, int offset, int limit)
      throws GraphModelException;
  
  public long countByType(String typeName)
      throws GraphModelException;
  public long count()
      throws GraphModelException;
}
