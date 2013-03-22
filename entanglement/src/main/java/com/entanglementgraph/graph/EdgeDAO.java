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
 * File created: 15-Nov-2012, 16:43:38
 */

package com.entanglementgraph.graph;

import com.mongodb.*;

import java.util.Map;

/**
 *
 * @author Keith Flanagan
 */
public interface EdgeDAO
    extends GraphEntityDAO
{


  public static final String FIELD_FROM_KEYS = "from";
  public static final String FIELD_FROM_KEYS_TYPE = FIELD_FROM_KEYS+".type";
  public static final String FIELD_FROM_KEYS_UIDS = FIELD_FROM_KEYS+".uids";
  public static final String FIELD_FROM_KEYS_NAMES = FIELD_FROM_KEYS+".names";

  public static final String FIELD_TO_KEYS = "to";
  public static final String FIELD_TO_KEYS_TYPE = FIELD_TO_KEYS+".type";
  public static final String FIELD_TO_KEYS_UIDS = FIELD_TO_KEYS+".uids";
  public static final String FIELD_TO_KEYS_NAMES = FIELD_TO_KEYS+".names";



  /**
   * Given a 'from' node and a 'to' node, returns an iterator over all the 
   * edges between these nodes.
   * @param fromNodeUid the ID of the 'from' node.
   * @param toNodeUid the ID of the 'to' node.
   * @return an iterable list of edge instances.
   * @throws GraphModelException 
   */
  public Iterable<DBObject> iterateEdgesBetweenNodes(
          String fromNodeUid, String toNodeUid)
          throws GraphModelException;
  
  /**
   * Given a 'from' node and a 'to' node, returns an iterator over all the 
   * edges of type <code>edgeType<code> between those nodes.
   * @param edgeType the type of edge to return
   * @param fromNodeUid the UID of the 'from' node.
   * @param toNodeUid the UID of the 'to' node.
   * @return an iterable set of edge instances with the specifed type that
   * link the two specified nodes.
   * @throws GraphModelException 
   */
  public Iterable<DBObject> iterateEdgesBetweenNodes(
          String edgeType, String fromNodeUid, String toNodeUid)
          throws GraphModelException;
  
  /**
   * Given a node , returns an Iterable over all the outgoing edges of that node.
   * @param fromNodeUid the node whose outgoing edges are to be iterated
   * @return an Iterable of edges.
   * @throws GraphModelException 
   */
  public Iterable<DBObject> iterateEdgesFromNode(String fromNodeUid)
          throws GraphModelException;
  
  /**
   * Given a node, returns an Iterable over all the incoming edges to that node.
   * @param toNodeUid the node whose incoming edges are to be iterated.
   * @return an Iterable of edges.
   * @throws GraphModelException 
   */
  public Iterable<DBObject> iterateEdgesToNode(String toNodeUid)
          throws GraphModelException;
  
  /**
   * Returns true if there exists at least one edge between the specified node, 
   * and any node of type <code>toNodeType</code>.
   * @param fromNodeUid the node ID from which an edge should start
   * @param toNodeType the type of node that we're interested in as a destination
   * @return true if there is an edge between <code>fromNodeUid</code> and any
   * other node of type <code>toNodeType</code>.
   * @throws GraphModelException 
   */
  public boolean existsEdgeToNodeOfType(String fromNodeUid, String toNodeType)
          throws GraphModelException;
  

  
  
  public Long countEdgesFromNode(String fromNodeUid)
          throws GraphModelException;
  
  public Long countEdgesOfTypeFromNode(String edgeType, String fromNodeUid)
          throws GraphModelException;
  
  public Long countEdgesToNode(String toNodeUid)
          throws GraphModelException;
  
  public Long countEdgesOfTypeToNode(String edgeType, String toNodeUid)
          throws GraphModelException;
  
  public Long countEdgesOfTypeBetweenNodes(
          String edgeType, String fromNodeUid, String toNodeUid)
          throws GraphModelException;
  
  
  
  /**
   * Given a node UID, returns a count of each distinct outgoing edge type for 
   * that node.
   * 
   * @param fromNodeUid the node whose outgoing edges are to be counted.
   * @return a Map of 'edge type name' to a count of the number of outgoing edges
   * that the specified node has of that type. Only edge type counts where the
   * count is greater than 0 are returned.
   * 
   * @throws GraphModelException 
   */
  public Map<String, Long> countEdgesByTypeFromNode(String fromNodeUid)
          throws GraphModelException;
  
  /**
   * Given a node UID, returns a count of each distinct incoming edge type for 
   * that node.
   * 
   * @param toNodeUid the node whose incoming edges are to be counted.
   * @return a Map of 'edge type name' to a count of the number of incoming edges
   * that the specified node has of that type. Only edge type counts where the
   * count is greater than 0 are returned.
   * 
   * @throws GraphModelException 
   */
  public Map<String, Long> countEdgesByTypeToNode(String toNodeUid)
          throws GraphModelException;

}