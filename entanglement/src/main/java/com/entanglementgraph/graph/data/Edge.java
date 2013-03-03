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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Edge<F extends Node, T extends Node>
       implements Serializable
{ 
  protected String uid;
  protected String type;
  protected Set<String> names;

  protected String fromUid;
  protected String fromType;
  protected String toUid;
  protected String toType;
  
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
    names = new HashSet<>();
    this.type = getClass().getSimpleName();
  }
  
  public Edge(String fromUid, String toUid)
  {
    names = new HashSet<>();
    this.type = getClass().getSimpleName();
    this.fromUid = fromUid;
    this.toUid = toUid;
  }
          
  public Edge(F from, T to) 
  {
    names = new HashSet<>();
    this.type = getClass().getSimpleName();
    this.fromUid = from.getUid();
    this.fromType = from.getType();
    this.toUid = to.getUid();
    this.toType = to.getType();
  }
  

  @Override
  public String toString() {
    return "Edge{" + "uid=" + uid + ", type=" + type + ", names=" + names
        + ", fromUid=" + fromUid + ", toUid=" + toUid +  + '}';
  }

  public void addName(String name)
  {
    names.add(name);
  }

  public void addNames(Collection<String> newNames)
  {
    names.addAll(newNames);
  }

  public void addNames(String... newNames)
  {
    names.addAll(Arrays.asList(newNames));
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFromUid() {
    return fromUid;
  }

  public void setFromUid(String fromUid) {
    this.fromUid = fromUid;
  }

  public String getToUid() {
    return toUid;
  }

  public void setToUid(String toUid) {
    this.toUid = toUid;
  }

  public String getFromType() {
    return fromType;
  }

  public void setFromType(String fromType) {
    this.fromType = fromType;
  }

  public String getToType() {
    return toType;
  }

  public void setToType(String toType) {
    this.toType = toType;
  }

  public boolean isHanging() {
    return hanging;
  }

  public void setHanging(boolean hanging) {
    this.hanging = hanging;
  }

  public Set<String> getNames() {
    return names;
  }

  public void setNames(Set<String> names) {
    this.names = names;
  }
}
