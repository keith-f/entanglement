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
  
  /*
   * Parent graph info
   */
//  private String graphUniqueId;
//  private String graphBranchId;
  
  /*
   * Node ID
   */
  private String uid;
  
  /*
   * Node info
   */
  private String name; //An optional but unique string for named concepts
  private String type;
  private String dsName;
  private String etName;  
  

  public CreateNode()
  {
  }

  public CreateNode(String typeUid, String uniqueId, 
      String dataSourceUid, String evidenceTypeUid)
  {
    this.uid = uniqueId;
    this.type = typeUid;
    this.dsName = dataSourceUid;
    this.etName = evidenceTypeUid;
  }
  
  
  @Override
  public String toString()
  {
    return "CreateNode{" + ", uniqueId=" + uid 
        + ", wellKnownName=" + name + ", typeUid=" + type 
        + ", dataSourceUid=" + dsName + ", evidenceTypeUid=" + etName + '}';
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDsName() {
    return dsName;
  }

  public void setDsName(String dsName) {
    this.dsName = dsName;
  }

  public String getEtName() {
    return etName;
  }

  public void setEtName(String etName) {
    this.etName = etName;
  }

}
