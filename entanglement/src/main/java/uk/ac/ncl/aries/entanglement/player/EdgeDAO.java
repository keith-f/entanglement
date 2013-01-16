/*
 * Copyright 2012 Keith Flanagan
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
 * File created: 15-Nov-2012, 16:43:38
 */

package uk.ac.ncl.aries.entanglement.player;

import com.mongodb.*;
import java.util.List;
import uk.ac.ncl.aries.entanglement.player.data.Edge;

/**
 *
 * @author Keith Flanagan
 */
public interface EdgeDAO
    extends GraphEntityDAO
{
  public static final String FIELD_FROM_NODE_UID = "fromUid";
  public static final String FIELD_TO_NODE_UID = "toUid";
  
  public static final String FIELD_FROM_NODE_TYPE = "fromType";
  public static final String FIELD_TO_NODE_TYPE = "toType";
  
  public static final String FIELD_FROM_NODE_NAME = "fromName";
  public static final String FIELD_TO_NODE_NAME = "toName";
  
  public DBCollection getNodeCol();
  
  
  
  public Iterable<DBObject> iterateEdgesBetweenNodes(
          String fromNodeUid, String toNodeUid)
          throws LogPlayerException;
  
  
  public Iterable<DBObject> iterateEdgesFromNode(String fromNodeUid)
          throws LogPlayerException;
  
  public Iterable<DBObject> iterateEdgesToNode(String fromNodeUid)
          throws LogPlayerException;
  
  

}
