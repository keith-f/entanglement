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
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class CreateEdgeFromObjBetweenNamedNodes
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(CreateEdgeFromObjBetweenNamedNodes.class.getName());
  
  
  private BasicDBObject edge;
  
  
  /*
   * IDs of connected nodes
   */
  private String fromName;
  private String toName;

  public CreateEdgeFromObjBetweenNamedNodes()
  {
  }
  
  public CreateEdgeFromObjBetweenNamedNodes(BasicDBObject edge,
          String fromNodeName, String toNodeName)
  {
    this.edge = edge;
    this.fromName = fromNodeName;
    this.toName = toNodeName;
  }

  @Override
  public String toString() {
    return "CreateEdge2BetweenNamedNodes{" + "edge=" + edge 
            + ", fromName=" + fromName + ", toName=" + toName + '}';
  }

  public BasicDBObject getEdge() {
    return edge;
  }

  public void setEdge(BasicDBObject edge) {
    this.edge = edge;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public String getToName() {
    return toName;
  }

  public void setToName(String toName) {
    this.toName = toName;
  }

}
