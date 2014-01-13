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
import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.GraphEntity;
import com.entanglementgraph.graph.data.Node;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewEdge<E extends NewEdge, F extends NewNode2, T extends NewNode2>
       implements Serializable, GraphEntity<E>
{
  protected EntityKeys<E> keys;

  protected EntityKeys<F> from;
  protected EntityKeys<T> to;

  public NewEdge()
  {
    keys = new EntityKeys<>();
  }

  public NewEdge(String type, EntityKeys<F> from, EntityKeys<T> to)
  {
    this.keys = new EntityKeys<>();
    this.keys.setType(type);
    this.from = from;
    this.to = to;
  }

  public NewEdge(EntityKeys<E> keys, EntityKeys<F> from, EntityKeys<T> to)
  {
    this.keys = keys;
    this.from = from;
    this.to = to;
  }

  public NewEdge(EntityKeys<E> keys, F from, T to)
  {
    this.keys = keys;
    this.from = from.keys;
    this.to = to.keys;
  }

  public NewEdge(EntityKeys<E> keys, String fromUid, String toUid)
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NewEdge edge = (NewEdge) o;

    if (!keys.equals(edge.keys)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return keys.hashCode();
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
