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

import com.entanglementgraph.couchdb.Edge;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
@JsonSerialize(include= JsonSerialize.Inclusion.NON_EMPTY)
public class EdgeModification
    extends GraphOperation
{
  private static final Logger logger = Logger.getLogger(EdgeModification.class.getName());

//  public static EdgeModification create(GraphConnection graphConn, MergePolicy mergePol, Edge edge)
//      throws DbObjectMarshallerException {
////    BasicDBObject edgeSer = graphConn.getMarshaller().serialize(edge);
//    String edgeSer = graphConn.getMarshaller().serializeToString(edge);
//    EdgeModification op = new EdgeModification(mergePol, edgeSer);
//    return op;
//  }
//
//  public static List<EdgeModification> create(GraphConnection graphConn, MergePolicy mergePol, Collection<Edge> edges)
//      throws DbObjectMarshallerException {
//    List<EdgeModification> ops = new ArrayList<>(edges.size());
//    for (Edge edge : edges) {
////      BasicDBObject edgeSer = graphConn.getMarshaller().serialize(edge);
//      String edgeSer = graphConn.getMarshaller().serializeToString(edge);
//      EdgeModification op = new EdgeModification(mergePol, edgeSer);
//      ops.add(op);
//    }
//    return ops;
//  }
//
//  public static EdgeModification create(DbObjectMarshaller m, MergePolicy mergePol, Edge edge)
//      throws DbObjectMarshallerException {
//    String edgeSer = m.serializeToString(edge);
//    EdgeModification op = new EdgeModification(mergePol, edgeSer);
//    return op;
//  }
//
//  public static List<EdgeModification> create(DbObjectMarshaller m, MergePolicy mergePol, Collection<Edge> edges)
//      throws DbObjectMarshallerException {
//    List<EdgeModification> ops = new ArrayList<>(edges.size());
//    for (Edge node : edges) {
////      BasicDBObject nodeSer = graphConn.getMarshaller().serialize(node);
//      String edgeSer = m.serializeToString(node);
//      EdgeModification op = new EdgeModification(mergePol, edgeSer);
//      ops.add(op);
//    }
//    return ops;
//  }

  private MergePolicy mergePol;
//  private BasicDBObject edge;
  private Edge edge;

 

  public EdgeModification()
  {
  }

  public EdgeModification(String edgeAsJson)
  {
    this.mergePol = MergePolicy.NONE;
    this.edge = edge;
    if (edgeAsJson == null) {
      throw new RuntimeException("The specified edge was NULL!");
    }
  }

//  public EdgeModification(BasicDBObject edge)
//  {
//    this.mergePol = MergePolicy.NONE;
//    this.edge = edge;
//    if (edge == null) {
//      throw new RuntimeException("The specified edge was NULL!");
//    }
//  }
  
  public EdgeModification(MergePolicy mergePol, Edge edge)
  {
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
