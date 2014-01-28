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
 * File created: 08-Nov-2012, 16:41:04
 */

package com.entanglementgraph.graph.commands;

/**
 *
 * @author Keith Flanagan
 */
public class GraphOperationEvent
{
  private GraphOperation operation;
  
  public GraphOperationEvent()
  {
  }

  public GraphOperation getOperation()
  {
    return operation;
  }

  public void setOperation(GraphOperation operation)
  {
    this.operation = operation;
  }
  
}
