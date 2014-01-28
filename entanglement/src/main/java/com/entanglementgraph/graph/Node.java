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

package com.entanglementgraph.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;


/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node<C extends Content> implements Serializable {
  protected EntityKeys<C> keys;
  protected C content;

  @JsonIgnore
  protected boolean virtual; // Indicates whether this is a virtual node (i.e., non-existent in the DB)
  @JsonIgnore
  protected boolean loaded; // Indicates whether the content has been loaded from the database

  public Node() {
    this.keys = new EntityKeys();
  }

  public Node(EntityKeys keys) {
    this.keys = keys;
  }

  public Node(String typeName) {
    this.keys = new EntityKeys();
    this.keys.setType(typeName);
  }

  public Node(EntityKeys keys, C content) {
    this.keys = keys;
    this.content = content;
  }

  public Node(String typeName, C content) {
    this.keys = new EntityKeys();
    this.keys.setType(typeName);
    this.content = content;
  }


  @Override
  public String toString() {
    return "Node{" +
        "keys=" + keys +
        ", content=" + content +
        ", virtual=" + virtual +
        ", loaded=" + loaded +
        '}';
  }

  public EntityKeys<C> getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys<C> keys) {
    this.keys = keys;
  }

  public C getContent() {
    return content;
  }

  public void setContent(C content) {
    this.content = content;
  }

  public boolean isVirtual() {
    return virtual;
  }

  public void setVirtual(boolean virtual) {
    this.virtual = virtual;
  }

  public boolean isLoaded() {
    return loaded;
  }

  public void setLoaded(boolean loaded) {
    this.loaded = loaded;
  }
}
