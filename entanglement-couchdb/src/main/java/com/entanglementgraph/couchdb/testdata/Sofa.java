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

package com.entanglementgraph.couchdb.testdata;

import com.entanglementgraph.graph.data.Node;
import org.codehaus.jackson.annotate.*;

/**
 * @author Keith Flanagan
 */
public class Sofa extends NewNode2<Sofa> {
  private static final String TYPE_NAME = Sofa.class.getSimpleName();

//  @JsonProperty("_id")
//  private String uid;
//  @JsonProperty("_rev")
//  private String rev;

  private String color;

  public Sofa() {
    getKeys().setType(TYPE_NAME);
  }

//  public String getUid() {
//    return uid;
//  }
//
//  public void setUid(String uid) {
//    this.uid = uid;
//  }
//
//  public String getRev() {
//    return rev;
//  }
//
//  public void setRev(String rev) {
//    this.rev = rev;
//  }

  public void setColor(String s) {
    color = s;
  }

  public String getColor() {
    return color;
  }
}
