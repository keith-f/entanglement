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
 * File created: 15-Nov-2012, 16:00:01
 */

package com.entanglementgraph.player;

/**
 *
 * @author Keith Flanagan
 */
public class GraphCheckoutNamingScheme
{
  private final String graphName;
  private final String graphBranchName;
  private final String revCollectionName;
  private final String nodeCollectionName;
  private final String edgeCollectionName;
  
  public GraphCheckoutNamingScheme(String graphName, String graphBranchName)
  {
    this.graphName = graphName;
    this.graphBranchName = graphBranchName;

    this.revCollectionName = graphName +"_revisions";
    this.nodeCollectionName = graphName +"_" + graphBranchName+"_nodes";
    this.edgeCollectionName = graphName +"_" + graphBranchName+"_edges";
  }

  public String getNodeCollectionName() {
    return nodeCollectionName;
  }

  public String getEdgeCollectionName() {
    return edgeCollectionName;
  }

  public String getGraphBranchName()
  {
    return graphBranchName;
  }

  public String getGraphName()
  {
    return graphName;
  }

  public String getRevCollectionName() {
    return revCollectionName;
  }
}
