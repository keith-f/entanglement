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
import com.entanglementgraph.graph.data.GraphEntity;
import com.entanglementgraph.graph.data.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.BaseJsonNode;
import org.ektorp.support.CouchDbDocument;

import java.util.*;

/**
 * @author Keith Flanagan
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "+jt")
public class NewNode extends CouchDbDocument implements GraphEntity { //extends Node<NewNode> implements Map<String, JsonNode> {
  protected EntityKeys keys;
//  @JsonProperty("+type")
  protected String type;
  protected Map<String, Object> data;
//  protected NodeData data;

  public NewNode() {
    data = new HashMap<>();
//    values.
  }

  @Override
  public String toString() {
    return "NewNode{" +
        "keys=" + keys +
        ", type='" + type + '\'' +
        ", data=" + data +
        '}';
  }

//  @JsonProperty("+type")
//  public String getType() {
//    return type;
//  }
//
//  @JsonProperty("+type")
//  public void setType(String type) {
//    this.type = type;
//  }

  @Override
  public EntityKeys getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys keys) {
    this.keys = keys;
  }

  public void setDataProperty(String name, Object value) {
    data.put(name, value);
  }

  public Object getDataProperty(String name) {
    return data.get(name);
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
