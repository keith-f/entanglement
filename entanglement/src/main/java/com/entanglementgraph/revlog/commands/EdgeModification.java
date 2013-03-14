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

package com.entanglementgraph.revlog.commands;

import com.mongodb.BasicDBObject;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class EdgeModification
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(EdgeModification.class.getName());

  private MergePolicy mergePol;
  private BasicDBObject edge;
  
  private boolean allowHanging;
 

  public EdgeModification()
  {
  }
  
  public EdgeModification(BasicDBObject edge)
  {
    this.mergePol = MergePolicy.NONE;
    this.edge = edge;
    this.allowHanging = false;
  }
  
  public EdgeModification(MergePolicy mergePol, BasicDBObject edge)
  {
    this.mergePol = mergePol;
    this.edge = edge;
    this.allowHanging = false;
  }
  
  
  
  @Override
  public String toString() {
    return "EdgeModification{" + "edge=" + edge + '}';
  }

  public BasicDBObject getEdge() {
    return edge;
  }

  public void setEdge(BasicDBObject edge) {
    this.edge = edge;
  }

  public MergePolicy getMergePol() {
    return mergePol;
  }

  public void setMergePol(MergePolicy mergePol) {
    this.mergePol = mergePol;
  }

  public boolean isAllowHanging() {
    return allowHanging;
  }

  public void setAllowHanging(boolean allowHanging) {
    this.allowHanging = allowHanging;
  }

}
