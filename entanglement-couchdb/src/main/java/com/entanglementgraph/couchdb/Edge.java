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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;


/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge<C extends Content, F extends Content, T extends Content> implements Serializable {
  protected EntityKeys<C> keys;
  protected EntityKeys<F> from;
  protected EntityKeys<T> to;
  protected C content;

  @JsonIgnore
  protected boolean loaded; // Indicates whether the content has been loaded from the database

  public Edge() {
    this.keys = new EntityKeys();
  }

  public Edge(EntityKeys keys) {
    this.keys = keys;
  }

  public Edge(String typeName) {
    this.keys = new EntityKeys();
    this.keys.setType(typeName);
  }

  public Edge(EntityKeys keys, C content) {
    this.keys = keys;
    this.content = content;
  }

  public Edge(String typeName, C content) {
    this.keys = new EntityKeys();
    this.keys.setType(typeName);
    this.content = content;
  }

  @Override
  public String toString() {
    return "Edge{" +
        "keys=" + keys +
        ", from=" + from +
        ", to=" + to +
        ", content=" + content +
        ", loaded=" + loaded +
        '}';
  }

  public EntityKeys<C> getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys<C> keys) {
    this.keys = keys;
  }

  public EntityKeys<F> getFrom() {
    return from;
  }

  public void setFrom(EntityKeys<F> from) {
    this.from = from;
  }

  public EntityKeys<T> getTo() {
    return to;
  }

  public void setTo(EntityKeys<T> to) {
    this.to = to;
  }

  public C getContent() {
    return content;
  }

  public void setContent(C content) {
    this.content = content;
  }

  public boolean isLoaded() {
    return loaded;
  }

  public void setLoaded(boolean loaded) {
    this.loaded = loaded;
  }
}
