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

import com.entanglementgraph.couchdb.Content;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Keith Flanagan
 */
//@JsonTypeInfo(
//    use = JsonTypeInfo.Id.NAME,
//    include = JsonTypeInfo.As.PROPERTY,
//    property = "+jt")
public class GeneContent implements Content {
  private String geneName;
  private String embl;

  public GeneContent() {
  }

  public GeneContent(String geneName, String embl) {
    this.geneName = geneName;
    this.embl = embl;
  }

  public String getGeneName() {
    return geneName;
  }

  public void setGeneName(String geneName) {
    this.geneName = geneName;
  }

  public String getEmbl() {
    return embl;
  }

  public void setEmbl(String embl) {
    this.embl = embl;
  }
}
