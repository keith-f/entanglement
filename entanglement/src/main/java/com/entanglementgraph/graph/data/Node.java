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
public class Node<N extends Node>
      implements Serializable, GraphEntity<N>
{ 
//  protected String uid;
//  protected Set<String> names;
//  protected String type;
  protected EntityKeys<N> keys;
  
  public Node()
  {
    keys = new EntityKeys<>();
//    names = new HashSet<>();
//    this.type = getClass().getSimpleName();
  }

  @Override
  public String toString() {
    return "Node{" +
        "keys=" + keys +
        '}';
  }

  public EntityKeys<N> getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys<N> keys) {
    this.keys = keys;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Node node = (Node) o;

    if (!keys.equals(node.keys)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return keys.hashCode();
  }
}
