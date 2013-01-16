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
 * This class can be used in cases where we just want to create a node entity
 * in the graph, without a corresponding Java bean.
 * 
 * @author Keith Flanagan
 */
public class CreateNode
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(CreateNode.class.getName());
  
  private BasicDBObject node;
  
  public CreateNode()
  {
  }

  public CreateNode(BasicDBObject node)
  {
    this.node = node;
  }

  @Override
  public String toString() {
    return "CreateNode{" + "node=" + node + '}';
  }

  public BasicDBObject getNode() {
    return node;
  }

  public void setNode(BasicDBObject node) {
    this.node = node;
  }

}
