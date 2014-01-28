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

package com.entanglementgraph.graph.commands;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class SetNodeProperty
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(SetNodeProperty.class.getName());
  
  /*
   * Node ID
   */
  private String nUid;
  
  /*
   * Property info
   */
  private String pName;
  private Object pVal;
  

  public SetNodeProperty()
  {
  }

  public SetNodeProperty(
      String nodeUniqueId, String propertyName, Object propertyValue)
  {
    this.nUid = nodeUniqueId;
    this.pName = propertyName;
    this.pVal = propertyValue;
  }

  @Override
  public String toString() {
    return "SetNodeProperty{" + "nodeUniqueId=" + nUid
            + ", propertyName=" + pName + ", propertyValue="
            + pVal + '}';
  }

  public String getnUid() {
    return nUid;
  }

  public void setnUid(String nUid) {
    this.nUid = nUid;
  }

  public String getpName() {
    return pName;
  }

  public void setpName(String pName) {
    this.pName = pName;
  }

  public Object getpVal() {
    return pVal;
  }

  public void setpVal(Object pVal) {
    this.pVal = pVal;
  }

}
