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

import com.entanglementgraph.graph.data.Edge;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Keith Flanagan
 */
public class HasPillow extends NewEdge<HasPillow, NewNode2, Pillow> {
  private static final String TYPE_NAME = HasPillow.class.getSimpleName();

//  @JsonProperty("_id")
//  private String uid; //Mapped to CouchDB's '_id' field.
//
//  @JsonProperty("_rev")
//  private String rev; //Used internally by CouchDB

  public HasPillow() {
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
}
