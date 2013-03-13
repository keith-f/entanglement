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
 * File created: 28-Aug-2012, 15:44:01
 */

package com.entanglementgraph.revlog.commands;

//import net.sourceforge.ondex.core.ONDEXGraph;

import com.entanglementgraph.util.GraphConnection;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


/**
 *
 * @author Keith Flanagan
 */
//@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
abstract public class GraphOperation
    implements Serializable
{
  private Set<String> pTags;
  private Set<String> pStrings;
  
  public GraphOperation()
  {
    pTags = new HashSet<>();
    pStrings = new HashSet<>();
  }
 
  public Set<String> getPStrings()
  {
    return pStrings;
  }

  public void setPStrings(Set<String> provenanceStrings)
  {
    this.pStrings = provenanceStrings;
  }

  public Set<String> getPTags()
  {
    return pTags;
  }

  public void setPTags(Set<String> provenanceTags)
  {
    this.pTags = provenanceTags;
  }

}
