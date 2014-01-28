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

/**
 * @author Keith Flanagan
 */
public class Pillow implements Content {
  private static final String TYPE_NAME = Pillow.class.getSimpleName();

  public static enum Softness {SOFT, MEDIUM, FIRM}

  private Softness softness;

//  @JsonCreator
//  public Pillow(@JsonProperty("softness") Softness s) {
//    getKeys().setType(TYPE_NAME);
//    softness = s;
//  }


  public Pillow() {
  }

  public Softness getSoftness() {
    return softness;
  }

  public void setSoftness(Softness softness) {
    this.softness = softness;
  }
}
