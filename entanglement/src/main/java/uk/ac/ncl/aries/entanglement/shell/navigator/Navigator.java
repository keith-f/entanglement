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

package uk.ac.ncl.aries.entanglement.shell.navigator;

import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import uk.ac.ncl.aries.entanglement.graph.EdgeDAO;
import uk.ac.ncl.aries.entanglement.graph.GraphModelException;
import uk.ac.ncl.aries.entanglement.graph.NodeDAO;
import uk.ac.ncl.aries.entanglement.graph.data.Edge;
import uk.ac.ncl.aries.entanglement.graph.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public class Navigator
{
  private final NodeDAO nodeDao;
  private final EdgeDAO edgeDao;
  
  private DBObject currentNode;
  private long numOutgoingEdges;
  private long numIncomingEdges;
  private SortedMap<String, Long> outgoingEdgeTypeToCount;
  private SortedMap<String, Long> incomingEdgeTypeToCount;
  
  private final Stack<Navigator> history;

  public Navigator(NodeDAO nodeDao, EdgeDAO edgeDao, String startNodeUid)
          throws GraphModelException
  {
    this.nodeDao = nodeDao;
    this.edgeDao = edgeDao;
    this.history = new Stack<>();
    
    configure(startNodeUid);
  }
  
  private Navigator(NodeDAO nodeDao, EdgeDAO edgeDao, Stack<Navigator> history, 
          String startNodeUid) throws GraphModelException
  {
    this.nodeDao = nodeDao;
    this.edgeDao = edgeDao;
    this.history = history;
    configure(startNodeUid);
  }
  
  private Navigator(NodeDAO nodeDao, EdgeDAO edgeDao, Stack<Navigator> history, 
          DBObject node) throws GraphModelException
  {
    this.nodeDao = nodeDao;
    this.edgeDao = edgeDao;
    this.history = history;
    configure(node);
  }
  
  private void configure(String nodeUid) throws GraphModelException
  {
    DBObject node = nodeDao.getByUid(nodeUid);
    configure(node);
  }
  
  private void configure(DBObject node) throws GraphModelException
  {
    setCurrentNode(node);
    edgeDao.countEdgesByTypeFromNode((String) node.get(NodeDAO.FIELD_UID));
    getOutgoingEdgeTypeToCount().putAll(
          edgeDao.countEdgesByTypeFromNode((String) node.get(NodeDAO.FIELD_UID)));
    getIncomingEdgeTypeToCount().putAll(
          edgeDao.countEdgesByTypeToNode((String) node.get(NodeDAO.FIELD_UID)));

    
    long incomingCount = 0;
    for (Long count : getIncomingEdgeTypeToCount().values()) {
      incomingCount = incomingCount + count;
    }
    setNumIncomingEdges(incomingCount);
    
    long outgoingCount = 0;
    for (Long count : getOutgoingEdgeTypeToCount().values()) {
      outgoingCount = outgoingCount + count;
    }
    setNumOutgoingEdges(outgoingCount);
    
    history.push(this);
  }
  
  
//  public Navigator startAt(String nodeType, String nodeName)
//          throws NavigatorException
//  {
//    try {
//      DBObject node = nodeDao.getByName(nodeType, nodeName);
//      
//      Navigator newNav = new Navigator(nodeDao, edgeDao, history, node);
//    return null;
//    } catch(Exception e) {
//      throw new NavigatorException("Failed to perform navigator operation", e);
//    }
//  }
  
  public Navigator gotoNodeById(String uid)
          throws NavigatorException
  {
    try {
      return new Navigator(nodeDao, edgeDao, history, uid);
    } catch(Exception e) {
      throw new NavigatorException("Failed to perform navigator operation", e);
    }
  }
  
  public Navigator gotoNodeByName(String nodeType, String nodeName)
          throws NavigatorException
  {
    try {
      DBObject node = nodeDao.getByName(nodeType, nodeName);
      return new Navigator(nodeDao, edgeDao, history, node);
    } catch(Exception e) {
      throw new NavigatorException("Failed to perform navigator operation", e);
    }
  }
  
//  public Navigator stepToFirstNodeOfType(String nodeType)
//          throws NavigatorException
//  {
//    return null;
//  }
//  
//  public Navigator stepViaEdgeType(Class<? extends Edge> edgeType)
//  {
//    return null;
//  }
//  
//  public Navigator stepViaEdgeTypeToNodeType(Class<? extends Edge> edgeType, Class<? extends Node> nodeType)
//  {
//    return null;
//  }
//  
//  public Navigator stepViaEdgeId(Class<? extends Edge> edgeType)
//  {
//    return null;
//  }
//  
//  public Node destination()
//  {
//    
//  }
  
//  public void test()
//  {
//    Navigator p = new Navigator();
//    p.startAt(null).stepToFirstNodeOfType("gene").stepViaEdgeTypeToNodeType(LocatedOn.class, Chromosome.class).destination();
//  }

  public DBObject getCurrentNode() {
    return currentNode;
  }

  public void setCurrentNode(DBObject currentNode) {
    this.currentNode = currentNode;
  }

  public long getNumOutgoingEdges() {
    return numOutgoingEdges;
  }

  public void setNumOutgoingEdges(long numOutgoingEdges) {
    this.numOutgoingEdges = numOutgoingEdges;
  }

  public long getNumIncomingEdges() {
    return numIncomingEdges;
  }

  public void setNumIncomingEdges(long numIncomingEdges) {
    this.numIncomingEdges = numIncomingEdges;
  }

  public SortedMap<String, Long> getOutgoingEdgeTypeToCount() {
    return outgoingEdgeTypeToCount;
  }

  public void setOutgoingEdgeTypeToCount(SortedMap<String, Long> outgoingEdgeTypeToCount) {
    this.outgoingEdgeTypeToCount = outgoingEdgeTypeToCount;
  }

  public SortedMap<String, Long> getIncomingEdgeTypeToCount() {
    return incomingEdgeTypeToCount;
  }

  public void setIncomingEdgeTypeToCount(SortedMap<String, Long> incomingEdgeTypeToCount) {
    this.incomingEdgeTypeToCount = incomingEdgeTypeToCount;
  }
}
