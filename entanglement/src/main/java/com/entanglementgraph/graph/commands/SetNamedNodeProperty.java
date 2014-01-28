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
public class SetNamedNodeProperty
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(SetNamedNodeProperty.class.getName());
  
  /*
   * Node identification by type/name pair
   */
  private String nType;
  private String nName;
  
  /*
   * Property info
   */
  private String pName;
  private Object pVal;
  

  public SetNamedNodeProperty()
  {
  }

  public SetNamedNodeProperty(String nodeType, String nodeWellKnownName, 
          String propertyName, Object propertyValue)
  {
    this.nType = nodeType;
    this.nName = nodeWellKnownName;
    this.pName = propertyName;
    this.pVal = propertyValue;
  }

  @Override
  public String toString()
  {
    return "SetNamedNodeProperty{" + "nodeType=" + nType 
        + "nodeWellKnownName=" + nName 
        + ", propertyName=" + pName + ", propertyValue=" + pVal + '}';
  }

  public String getnName() {
    return nName;
  }

  public void setnName(String nName) {
    this.nName = nName;
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

  public String getnType() {
    return nType;
  }

  public void setnType(String nType) {
    this.nType = nType;
  }


}
