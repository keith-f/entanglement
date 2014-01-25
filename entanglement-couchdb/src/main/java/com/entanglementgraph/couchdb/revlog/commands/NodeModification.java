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

package com.entanglementgraph.couchdb.revlog.commands;

import com.entanglementgraph.couchdb.testdata.NodeWithContent;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.logging.Logger;

/**
 * This class can be used in cases where we just want to create a node entity
 * in the graph, without a corresponding Java bean.
 * 
 * @author Keith Flanagan
 */
@JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
public class NodeModification
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(NodeModification.class.getName());

//  public static NodeModification create(DbObjectMarshaller m, MergePolicy mergePol, Node node)
//      throws DbObjectMarshallerException {
//    ObjectMapper jsonMapper = new ObjectMapper();
////    String nodeSer = m.serializeToString(node);
////    NodeModification op = new NodeModification(mergePol, nodeSer);
//      NodeModification op = new NodeModification(mergePol, node);
//    return op;
//  }
//
//  public static List<NodeModification> create(DbObjectMarshaller m, MergePolicy mergePol, Collection<Node> nodes)
//      throws DbObjectMarshallerException {
//    List<NodeModification> ops = new ArrayList<>(nodes.size());
//    for (Node node : nodes) {
////      BasicDBObject nodeSer = graphConn.getMarshaller().serialize(node);
////      String nodeSer = m.serializeToString(node);
////      NodeModification op = new NodeModification(mergePol, nodeSer);
//      NodeModification op = new NodeModification(mergePol, node);
//      ops.add(op);
//    }
//    return ops;
//  }


  private MergePolicy mergePol;

//  private Node node;
//  private NewNode2 node;
  private NodeWithContent node;
  
  public NodeModification()
  {
  }

  public NodeModification(MergePolicy mergePol, NodeWithContent node)
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

  public NodeWithContent getNode() {
    return node;
  }

  public void setNode(NodeWithContent node) {
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
