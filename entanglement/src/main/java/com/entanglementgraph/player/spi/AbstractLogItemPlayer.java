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

package com.entanglementgraph.player.spi;

import com.entanglementgraph.graph.EdgeDAO;
import com.entanglementgraph.graph.NodeDAO;
import com.entanglementgraph.player.LogPlayerException;
import com.entanglementgraph.revlog.data.RevisionItem;
import com.entanglementgraph.util.GraphConnection;
import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;

import java.util.List;

/**
 *
 * @author Keith Flanagan
 */
abstract public class AbstractLogItemPlayer
    implements LogItemPlayer
{
  protected GraphConnection graphConnection;
  protected NodeDAO nodeDao;
  protected EdgeDAO edgeDao;
  protected DbObjectMarshaller marshaller;
  
  @Override
  public void setGraphConnection(GraphConnection graphConnection)
  {
    this.graphConnection = graphConnection;
    nodeDao = graphConnection.getNodeDao();
    edgeDao = graphConnection.getEdgeDao();
    marshaller = graphConnection.getMarshaller();
  }

  /**
   * Default implementation simply calls <code>playItem</code> for each <code>RevisionItem</code>.
   * @param items
   * @throws LogPlayerException
   */
//  public void playBatch(List<RevisionItem> items)
//      throws LogPlayerException
//  {
//    for (RevisionItem item : items) {
//      playItem(item);
//    }
//  }
}
