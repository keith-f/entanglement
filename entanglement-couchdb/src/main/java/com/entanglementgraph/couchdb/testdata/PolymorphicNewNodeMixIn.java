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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Keith Flanagan
 */
abstract public class PolymorphicNewNodeMixIn {
//  ANodeMixIn(@JsonProperty("width") int w, @JsonProperty("height") int h) { }
  // note: could alternatively annotate fields "w" and "h" as well -- if so, would need to @JsonIgnore getters
//  @JsonProperty("width") abstract int getW(); // rename property
//  @JsonProperty("height") abstract int getH(); // rename property
//  @JsonIgnore abstract int getSize(); // we don't need it!

}
