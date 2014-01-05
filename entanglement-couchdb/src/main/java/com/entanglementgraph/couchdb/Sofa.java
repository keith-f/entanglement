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

package com.entanglementgraph.couchdb;

import org.codehaus.jackson.annotate.*;

/**
 * @author Keith Flanagan
 */
public class Sofa {
  @JsonProperty("_id")
  private String id;
  @JsonProperty("_rev")
  private String revision;
  private String color;


  public String getId() {
    return id;
  }

  public void setId(String s) {
    id = s;
  }


  public String getRevision() {
    return revision;
  }

  public void setRevision(String s) {
    revision = s;
  }

  public void setColor(String s) {
    color = s;
  }

  public String getColor() {
    return color;
  }
}
