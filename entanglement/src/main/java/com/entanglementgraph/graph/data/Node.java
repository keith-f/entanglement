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
import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node
       implements Serializable
{ 
  protected String uid;
  protected Set<String> names;
  protected String type;
  
  public Node()
  {
    names = new HashSet<>();
    this.type = getClass().getSimpleName();
  }

  @Override
  public String toString() {
    return "Node{" + "uid=" + uid + ", names=" + names + ", type=" + type + '}';
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

  public Set<String> getNames() {
    return names;
  }

  public void setNames(Set<String> names) {
    this.names = names;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

}
