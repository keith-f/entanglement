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
 * File created: 15-Nov-2012, 15:30:12
 */

package uk.ac.ncl.aries.entanglement.player;

import com.mongodb.*;
import java.util.List;
import uk.ac.ncl.aries.entanglement.player.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public interface NodeDAO
    extends GraphEntityDAO
{
  /**
   * Attempts to add a Node instance to an existing graph.
   * @param node
   * @throws LogPlayerException if a node with the same unique ID already
   * exists in the data structure.
   * @deprecated 
   */
//  public void store(Node node)
//      throws LogPlayerException;
  
}
