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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * @author Keith Flanagan
 */
public interface EdgeDAO<C extends Content, F extends Content, T extends Content> {

  public <C extends Content, F extends Content, T extends Content> EdgeDAO<C, F, T>
      forContent(Class<C> contentType, Class<F> fromType, Class<T> toType);

  /**
   * Given a partial keyset (a keyset suspected of containing less then the complete number of UIDs or names for a
   * given node), queries the database and returns a fully populated keyset.
   * @param partial
   * @return
   */
  public EntityKeys<C> populateFullKeyset(EntityKeys<C> partial)
      throws GraphModelException;

  public Edge<C, F, T> getByKey(EntityKeys<C> keyset)
      throws GraphModelException;

  public boolean existsByKey(EntityKeys<C> keyset)
      throws GraphModelException;

  //TODO implement these later
  public Iterable<Edge<C, F, T>> iterateAll()
      throws GraphModelException;
  public Iterable<Edge<C, F, T>> iterateByType(String typeName)
      throws GraphModelException;
//  public long countAll()
//      throws GraphModelException;
//
//  public <C extends Content> Iterable<Node<C>> iterateByType(String typeName)
//      throws GraphModelException;
//  public long countByType(String typeName)
//      throws GraphModelException;
//  public List<String> listTypes()
//      throws GraphModelException;






  /**
   * Given a 'from' node and a 'to' node, returns an iterator over all the
   * edges between these nodes, regardless of which of these nodes they originate at.
   *
   * @param fromNode the ID of the 'from' node.
   * @param to       the ID of the 'to' node.
   * @return an iterable list of edge instances.
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesBetweenNodes(
      EntityKeys<F> fromNode, EntityKeys<T> to)
      throws GraphModelException;

  /**
   * Given a 'from' node and a 'to' node, returns an iterator over all the
   * edges of type <code>edgeType<code> between those nodes, regardless of
   * which of these nodes they originate at.
   *
   * @param edgeType the type of edge to return
   * @param from     the UID of the 'from' node.
   * @param to       the UID of the 'to' node.
   * @return an iterable set of edge instances with the specified type that
   *         link the two specified nodes.
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesBetweenNodes(
      String edgeType, EntityKeys<F> from, EntityKeys<T> to)
      throws GraphModelException;

  /**
   * Given a node , returns an Iterable over all the outgoing edges of that node.
   *
   * @param fromFullNodeKeyset the node whose outgoing edges are to be iterated
   * @return an Iterable of edges.
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesFromNode(EntityKeys<F> fromFullNodeKeyset)
      throws GraphModelException;

  /**
   * Given a node, returns an Iterable over all the outgoing edges of that node, of the specified <code>edgeType</code>.
   *
   * @param edgeType the edge type to return.
   * @param fromFullNodeKeyset the node whose outgoing edges are to be iterated
   * @return an Iterable of edges.
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesFromNode(String edgeType, EntityKeys<F> fromFullNodeKeyset)
      throws GraphModelException;

//  public Iterable<JsonNode> iterateEdgesFromNode(String edgeType, EntityKeys<? extends Node> from,
//                                       Integer offset, Integer limit, DBObject customQuery, DBObject sort)
//      throws GraphModelException;

  /**
   * Iterates all edges from a specified node that link to a destination node of a specified type.
   * @param from the source node
   * @param toNodeType the type of destination node
   * @return
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesFromNodeToNodeOfType(EntityKeys<? extends Node> from, String toNodeType)
      throws GraphModelException;

  /**
   * Iterates all edges <b>to</b> a specified node that originate from a remote (source) node of a specified type.
   * @param to the destination node
   * @param fromNodeType the type of source node
   * @return
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesToNodeFromNodeOfType(EntityKeys<? extends Node> to, String fromNodeType)
      throws GraphModelException;

  /**
   * Given a node, returns an Iterable over all the incoming edges to that node.
   *
   * @param to the node whose incoming edges are to be iterated.
   * @return an Iterable of edges.
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesToNode(EntityKeys to)
      throws GraphModelException;

  /**
   * Given a node, returns an Iterable over all the incoming edges to that node, of the specified <code>edgeType</code>.
   * @param edgeType
   * @param to
   * @return
   * @throws GraphModelException
   */
  public Iterable<Edge<C, F, T>> iterateEdgesToNode(String edgeType, EntityKeys to)
      throws GraphModelException;

//  public Iterable<JsonNode> iterateEdgesToNode(String edgeType, EntityKeys<? extends Node> to,
//                                     Integer offset, Integer limit, DBObject customQuery, DBObject sort)
//      throws GraphModelException;

  /**
   * Returns true if there exists at least one edge between the specified node,
   * and any node of type <code>toNodeType</code>.
   *
   * @param from       the node ID from which an edge should start
   * @param toNodeType the type of node that we're interested in as a destination
   * @return true if there is an edge between <code>fromNodeUid</code> and any
   *         other node of type <code>toNodeType</code>.
   * @throws GraphModelException
   */
  public boolean existsEdgeToNodeOfType(EntityKeys from, String toNodeType)
      throws GraphModelException;


  public Long countEdgesFromNode(EntityKeys from)
      throws GraphModelException;

  public Long countEdgesOfTypeFromNode(String edgeType, EntityKeys from)
      throws GraphModelException;

  public Long countEdgesToNode(EntityKeys to)
      throws GraphModelException;

  public Long countEdgesOfTypeToNode(String edgeType, EntityKeys to)
      throws GraphModelException;

  /**
   * Counts edges of a given type between the specified nodes, regardless of which node the edge starts at.
   * @param edgeType
   * @param from
   * @param to
   * @return
   * @throws GraphModelException
   */
  public Long countEdgesOfTypeBetweenNodes(
      String edgeType, EntityKeys from, EntityKeys to)
      throws GraphModelException;


  /**
   * Given a node UID, returns a count of each distinct outgoing edge type for
   * that node.
   *
   * @param from the node whose outgoing edges are to be counted.
   * @return a Map of 'edge type name' to a count of the number of outgoing edges
   *         that the specified node has of that type. Only edge type counts where the
   *         count is greater than 0 are returned.
   * @throws GraphModelException
   */
  public Map<String, Long> countEdgesByTypeFromNode(EntityKeys from)
      throws GraphModelException;

  /**
   * Given a node UID, returns a count of each distinct incoming edge type for
   * that node.
   *
   * @param to the node whose incoming edges are to be counted.
   * @return a Map of 'edge type name' to a count of the number of incoming edges
   *         that the specified node has of that type. Only edge type counts where the
   *         count is greater than 0 are returned.
   * @throws GraphModelException
   */
  public Map<String, Long> countEdgesByTypeToNode(EntityKeys to)
      throws GraphModelException;

}
