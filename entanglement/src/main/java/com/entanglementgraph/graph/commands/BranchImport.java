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

package com.entanglementgraph.graph.commands;

/**
 * Imports all revisions from a specified graph/branch and merges them into the 
 * current graph/branch. This command is useful for collating different branches
 * of the same graph or different graphs. Examples of where this is useful:
 * <ul>
 * <li>Some processes populate a set of nodes and edges from primary data. This
 * primary data might time-consuming to parse. Therefore, you may wish to use
 * the parsed data as a starting point for one or more subsequent graphs that
 * perform additional analyses.</li>
 * <li>In a system with multiple processes that execute in parallel, each 
 * process could output its results into a separate graph. This approach
 * requires fewer resource locks and is therefore more scalable. Also, if a 
 * process fails, it is possible to simply throw away the result graph for that
 * process an re-execute it. Finally, the graphs from the successful processes
 * can be merged together.</li>
 * </ul>
 * 
 * @author Keith Flanagan
 */
public class BranchImport
    extends GraphOperation
{
  
  private String fromGraphUid;
  
  public BranchImport() {
  }

  public BranchImport(String fromGraphUid) {
    this.fromGraphUid = fromGraphUid;
  }

  public String getFromGraphUid() {
    return fromGraphUid;
  }

  public void setFromGraphUid(String fromGraphUid) {
    this.fromGraphUid = fromGraphUid;
  }

}
