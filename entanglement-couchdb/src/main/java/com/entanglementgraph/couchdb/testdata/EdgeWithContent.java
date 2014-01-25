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

import com.entanglementgraph.graph.data.EntityKeys;

/**
 * @author Keith Flanagan
 */
//@JsonTypeInfo(
//    use = JsonTypeInfo.Id.NAME,
//    include = JsonTypeInfo.As.PROPERTY,
//    property = "+jt")
public class EdgeWithContent<C extends NodeContent> {
  protected EntityKeys keys;
  protected EntityKeys from;
  protected EntityKeys to;
  protected C content;

  public EdgeWithContent() {
  }

  public EdgeWithContent(EntityKeys keys, C content) {
    this.keys = keys;
    this.content = content;
  }

  public EdgeWithContent(EntityKeys keys) {
    this.keys = keys;
  }

  @Override
  public String toString() {
    return "NodeWithContent{" +
        "keys=" + keys +
        ", content=" + content +
        '}';
  }

  public EntityKeys getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys keys) {
    this.keys = keys;
  }

  public C getContent() {
    return content;
  }

  public void setContent(C content) {
    this.content = content;
  }

  public EntityKeys getFrom() {
    return from;
  }

  public void setFrom(EntityKeys from) {
    this.from = from;
  }

  public EntityKeys getTo() {
    return to;
  }

  public void setTo(EntityKeys to) {
    this.to = to;
  }
}

