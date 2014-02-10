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
import com.entanglementgraph.graph.Node;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.logging.Logger;

/**
 * This class can be used in cases where we just want to create a node entity
 * in the graph, without a corresponding Java bean.
 * 
 * @author Keith Flanagan
 */
@JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
public class NodeModification<C extends Content>
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(NodeModification.class.getName());


  private MergePolicy mergePol;

  private Node<C> node;
  
  public NodeModification()
  {
  }

  public NodeModification(MergePolicy mergePol, Node node)
  {
    this.mergePol = mergePol;
    this.node = node;
    if (node == null) {
      throw new RuntimeException("The specified node was NULL!");
    }
  }

  @Override
  public String toString() {
    return "NodeModification{" + "node=" + node + '}';
  }

  public Node<C> getNode() {
    return node;
  }

  public void setNode(Node<C> node) {
    this.node = node;
    if (node == null) {
      throw new RuntimeException("The specified node was NULL!");
    }
  }

  public MergePolicy getMergePol() {
    return mergePol;
  }

  public void setMergePol(MergePolicy mergePol) {
    this.mergePol = mergePol;
  } 
}
