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

package com.entanglementgraph.graph.data;

import java.io.Serializable;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge<E extends Edge, F extends Node, T extends Node>
       implements Serializable, GraphEntity
{
  protected EntityKeys<E> keys;

  protected EntityKeys<F> from;
  protected EntityKeys<T> to;
  
  public Edge()
  {
    keys = new EntityKeys<>();
  }

  public Edge(String type, EntityKeys<F> from, EntityKeys<T> to)
  {
    this.keys = new EntityKeys<>();
    this.keys.setType(type);
    this.from = from;
    this.to = to;
  }

  public Edge(EntityKeys<E> keys, EntityKeys<F> from, EntityKeys<T> to)
  {
    this.keys = keys;
    this.from = from;
    this.to = to;
  }

  public Edge(EntityKeys<E> keys, F from, T to)
  {
    this.keys = keys;
    this.from = from.keys;
    this.to = to.keys;
  }

  public Edge(EntityKeys<E> keys, String fromUid, String toUid)
  {
    this.keys = keys;
    this.from = new EntityKeys<>(fromUid);
    this.to = new EntityKeys<>(toUid);
  }

  @Override
  public String toString() {
    return "Edge{" +
        "keys=" + keys +
        ", from=" + from +
        ", to=" + to +
        '}';
  }

  public EntityKeys<E> getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys<E> keys) {
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
}
