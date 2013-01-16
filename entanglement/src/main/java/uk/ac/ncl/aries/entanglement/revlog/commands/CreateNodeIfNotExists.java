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

package uk.ac.ncl.aries.entanglement.revlog.commands;

import com.mongodb.BasicDBObject;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class CreateNodeIfNotExists
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(CreateNodeIfNotExists.class.getName());
  
  //private Node2 node;
  private BasicDBObject node;
  
  public CreateNodeIfNotExists()
  {
  }

  public CreateNodeIfNotExists(BasicDBObject node)
  {
    this.node = node;
  }

  @Override
  public String toString() {
    return "CreateNodeIfNotExists{" + "node=" + node + '}';
  }

  public BasicDBObject getNode() {
    return node;
  }

  public void setNode(BasicDBObject node) {
    this.node = node;
  }

}
