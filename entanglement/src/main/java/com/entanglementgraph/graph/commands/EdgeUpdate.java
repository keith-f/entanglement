/*
 * Copyright 2012 Keith Flanagan
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
 * File created: 28-Aug-2012, 15:44:46
 */

package com.entanglementgraph.graph.commands;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.Edge;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Keith Flanagan
 */
@JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
public class EdgeUpdate
    extends GraphOperation
{
  private MergePolicy mergePol;
  private Edge edge;

  public EdgeUpdate() {
  }

  public EdgeUpdate(MergePolicy mergePol, Edge edge)  {
    this.mergePol = mergePol;
    this.edge = edge;
  }

  @Override
  public String toString() {
    return "EdgeModification{" + "edge=" + edge + '}';
  }

  public Edge getEdge() {
    return edge;
  }

  public void setEdge(Edge edge) {
    this.edge = edge;
  }

  public MergePolicy getMergePol() {
    return mergePol;
  }

  public void setMergePol(MergePolicy mergePol) {
    this.mergePol = mergePol;
  }


}
