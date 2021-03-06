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
 * File created: 28-Aug-2012, 15:44:46
 */

package uk.ac.ncl.aries.entanglement.revlog.commands;

import com.mongodb.BasicDBObject;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class CreateEdgeIfNotExists
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(CreateEdgeIfNotExists.class.getName());
  
  
  private BasicDBObject edge;
  
 

  public CreateEdgeIfNotExists()
  {
  }
  
  public CreateEdgeIfNotExists(BasicDBObject edge)
  {
    this.edge = edge;
  }
  
  public CreateEdgeIfNotExists(DbObjectMarshaller marshaller,
          String edgeUid, String fromNodeUid, String toNodeUid,
          String fromNodeName, String toNodeName)
  {
    
  }
  
  
  @Override
  public String toString() {
    return "CreateEdgeIfNotExists{" + "edge=" + edge + '}';
  }

  public BasicDBObject getEdge() {
    return edge;
  }

  public void setEdge(BasicDBObject edge) {
    this.edge = edge;
  }

}
