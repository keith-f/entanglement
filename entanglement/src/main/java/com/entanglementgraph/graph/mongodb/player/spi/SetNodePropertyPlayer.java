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
 * File created: 27-Nov-2012, 11:56:00
 */

package com.entanglementgraph.graph.mongodb.player.spi;

import com.entanglementgraph.graph.mongodb.player.LogPlayerException;
import com.entanglementgraph.graph.commands.SetNodeProperty;
import com.entanglementgraph.graph.RevisionItem;

/**
 *
 * @author Keith Flanagan
 */
public class SetNodePropertyPlayer
    extends AbstractLogItemPlayer
{
  @Override
  public String getSupportedLogItemType()
  {
    return SetNodeProperty.class.getSimpleName();
  }

  @Override
  public void playItem(RevisionItem item)
      throws LogPlayerException
  {
    try {
      SetNodeProperty op = (SetNodeProperty) item.getOp();

      nodeDao.setPropertyByUid(op.getnUid(), op.getpName(), op.getpVal());
    } catch (Exception e) {
      throw new LogPlayerException("Failed to play command", e);
    }
  }

}
