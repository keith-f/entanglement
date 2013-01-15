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
 */

package uk.ac.ncl.aries.entanglement.player;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

/**
 *
 * @author Keith Flanagan
 */
public class PlayerDAOFactory
{
  public static NodeDAO createDefaultNodeDAO(Mongo m, DB db, DBCollection nodeCol, DBCollection edgeCol)
  {
    NodeDAO nodeDao = new NodeDAONodePerDocImpl(m, db, nodeCol);
    return nodeDao;
  }
  
  public static EdgeDAO createDefaultEdgeDAO(Mongo m, DB db, DBCollection nodeCol, DBCollection edgeCol)
  {
    return createSeparateDocImplEdgeDAO(m, db, nodeCol, edgeCol);
  }
  
//  public static EdgeDAO createAttachementImplEdgeDAO(Mongo m, DB db, DBCollection edgeCol)
//  {
//    EdgeDAO edgeDao = new EdgeDAOAttachToNodeDocImpl(m, db, edgeCol);
//    return edgeDao;
//  }
  
  
  public static EdgeDAO createSeparateDocImplEdgeDAO(Mongo m, DB db, DBCollection nodeCol, DBCollection edgeCol)
  {
    EdgeDAO edgeDao = new EdgeDAOSeparateDocImpl(m, db, nodeCol, edgeCol);
    return edgeDao;
  }
}
