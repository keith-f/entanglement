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

package com.entanglementgraph.revlog.commands;

import com.entanglementgraph.graph.data.Node;
import com.entanglementgraph.util.GraphConnection;
import com.mongodb.BasicDBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class can be used in cases where we just want to create a node entity
 * in the graph, without a corresponding Java bean.
 * 
 * @author Keith Flanagan
 */
public class NodeModification
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(NodeModification.class.getName());

  public static NodeModification create(GraphConnection graphConn, MergePolicy mergePol, Node node)
      throws DbObjectMarshallerException {
    BasicDBObject nodeSer = graphConn.getMarshaller().serialize(node);
    NodeModification op = new NodeModification(mergePol, nodeSer);
    return op;
  }

  public static List<NodeModification> create(GraphConnection graphConn,
                                              MergePolicy mergePol, Collection<Node> nodes)
      throws DbObjectMarshallerException {
    List<NodeModification> ops = new ArrayList<>(nodes.size());
    for (Node node : nodes) {
      BasicDBObject nodeSer = graphConn.getMarshaller().serialize(node);
      NodeModification op = new NodeModification(mergePol, nodeSer);
      ops.add(op);
    }
    return ops;
  }

  private MergePolicy mergePol;
  
  private BasicDBObject node;
  
  public NodeModification()
  {
  }

  public NodeModification(MergePolicy mergePol, BasicDBObject node)
  {
    this.mergePol = mergePol;
    this.node = node;
  }

  @Override
  public String toString() {
    return "NodeModification{" + "node=" + node + '}';
  }

  public BasicDBObject getNode() {
    return node;
  }

  public void setNode(BasicDBObject node) {
    this.node = node;
  }

  public MergePolicy getMergePol() {
    return mergePol;
  }

  public void setMergePol(MergePolicy mergePol) {
    this.mergePol = mergePol;
  } 
}
