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
 * File created: 27-Nov-2012, 11:37:11
 */

package uk.ac.ncl.aries.entanglement.player.spi;

import com.torrenttamer.mongodb.dbobject.DbObjectMarshaller;
import uk.ac.ncl.aries.entanglement.player.EdgeDAO;
import uk.ac.ncl.aries.entanglement.player.LogPlayerException;
import uk.ac.ncl.aries.entanglement.player.NodeDAO;
import uk.ac.ncl.aries.entanglement.revlog.data.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public interface LogItemPlayer
{
  public void setMarshaller(DbObjectMarshaller marshaller);
  
  public String getSupportedLogItemType();
  
  public void playItem(NodeDAO nodeDao, EdgeDAO edgeDao, RevisionItem item)
      throws LogPlayerException;
}
