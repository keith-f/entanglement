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
 */

package com.entanglementgraph.graph.commands;

import com.entanglementgraph.graph.EntityKeys;

/**
 *
 * @author Keith Flanagan
 * @deprecated Use a EdgeModification instead
 */
public class DeleteEdge
    extends GraphOperation
{
  private EntityKeys edgeKeyset;
  
  public DeleteEdge()
  {
  }
  
  public DeleteEdge(EntityKeys edgeKeyset)
  {
    this.edgeKeyset = edgeKeyset;
  }

  @Override
  public String toString()
  {
    return "DeleteEdge" + "edgeKeyset=" + edgeKeyset + '}';
  }

  public EntityKeys getEdgeKeyset() {
    return edgeKeyset;
  }

  public void setEdgeKeyset(EntityKeys edgeKeyset) {
    this.edgeKeyset = edgeKeyset;
  }
}
