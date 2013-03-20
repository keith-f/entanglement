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

package com.entanglementgraph.graph;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class NodeDAONodePerDocImpl
    extends AbstractGraphEntityDAO
    implements NodeDAO
{
  private static final Logger logger =
      Logger.getLogger(NodeDAO.class.getName());

  
  public NodeDAONodePerDocImpl(ClassLoader classLoader, Mongo m, DB db, DBCollection col)
  {
    super(classLoader, m, db, col);
  }


}
