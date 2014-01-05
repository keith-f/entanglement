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

import java.util.logging.Logger;

/**
 *
 * @deprecated Not currently in use
 * @author Keith Flanagan
 */
public class CreateGraph
//    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(com.entanglementgraph.revlog.commands.CreateGraph.class.getName());
  
  private String graphDescr;


  public CreateGraph()
  {
  }
  
  public CreateGraph(String graphUniqueId, String graphBranchId, String graphDescr)
  {
    this.graphDescr = graphDescr;
  }
  
}
