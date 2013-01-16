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

package uk.ac.ncl.aries.entanglement.graph.data;

import java.io.Serializable;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge
       implements Serializable
{ 
  protected String uid;
  protected String type;
  protected String fromUid;
  protected String fromType;
  protected String toUid;
  protected String toType;
  
  /*
   * Optional - we can specify named nodes here as well IN ADDITION TO UIDs.
   */
  protected String fromName;
  protected String toName;
  
  public Edge()
  {
  }

  @Override
  public String toString() {
    return "Edge{" + "uid=" + uid + ", type=" + type + ", fromUid=" + fromUid 
            + ", toUid=" + toUid + ", fromName=" + fromName + ", toName=" + toName + '}';
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFromUid() {
    return fromUid;
  }

  public void setFromUid(String fromUid) {
    this.fromUid = fromUid;
  }

  public String getToUid() {
    return toUid;
  }

  public void setToUid(String toUid) {
    this.toUid = toUid;
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

  public String getFromType() {
    return fromType;
  }

  public void setFromType(String fromType) {
    this.fromType = fromType;
  }

  public String getToType() {
    return toType;
  }

  public void setToType(String toType) {
    this.toType = toType;
  }
  
}
