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
public class Edge<F extends Node, T extends Node>
       implements Serializable
{
  protected EntityKeys keys;



//  protected String uid;
//  protected String type;
//  protected Set<String> names;

//  protected String fromUid;
//  protected String fromType;
//  protected String toUid;
//  protected String toType;

  protected EntityKeys from;
  protected EntityKeys to;
  
  /**
   * Indicates whether or not this edge might be a hanging edge.
   * If true, indicates that this edge is <i>possibly</i> hanging (i.e., one or
   * both of the associated nodes does not exist).
   * If false, indicates that this edge is <i>definitely not</i> hanging; both
   * associated nodes exist.
   */
  protected boolean hanging;
  
  public Edge()
  {
    keys = new EntityKeys();
  }

  public Edge(String type, EntityKeys from, EntityKeys to)
  {
    this.keys = new EntityKeys();
    this.keys.setType(type);
    this.from = from;
    this.to = to;
  }

  public Edge(EntityKeys keys, EntityKeys from, EntityKeys to)
  {
    this.keys = keys;
    this.from = from;
    this.to = to;
  }

  public Edge(EntityKeys keys, F from, T to)
  {
    this.keys = keys;
    this.from = from.keys;
    this.to = to.keys;
  }

  public Edge(EntityKeys keys, String fromUid, String toUid)
  {
    this.keys = keys;
    this.from = new EntityKeys(fromUid);
    this.to = new EntityKeys(toUid);
  }
          
//  public Edge(F from, T to)
//  {
//    names = new HashSet<>();
//    this.type = getClass().getSimpleName();
//    this.from = new EntityRef(from.getUid(), from.getType(), from.getNames());
//    this.to = new EntityRef(to.getUid(), to.getType(), to.getNames());
//  }

  @Override
  public String toString() {
    return "Edge{" +
        "keys=" + keys +
        ", from=" + from +
        ", to=" + to +
        ", hanging=" + hanging +
        '}';
  }

  public EntityKeys getKeys() {
    return keys;
  }

  public void setKeys(EntityKeys keys) {
    this.keys = keys;
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

  public boolean isHanging() {
    return hanging;
  }

  public void setHanging(boolean hanging) {
    this.hanging = hanging;
  }
}
