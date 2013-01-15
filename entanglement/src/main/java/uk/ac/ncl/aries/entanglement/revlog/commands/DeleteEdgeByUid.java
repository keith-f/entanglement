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

package uk.ac.ncl.aries.entanglement.revlog.commands;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class DeleteEdgeByUid
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(DeleteEdgeByUid.class.getName());
  
  /*
   * Edge ID
   */
  private String uid;
  
  public DeleteEdgeByUid()
  {
  }
  
  public DeleteEdgeByUid(String uniqueId)
  {
    this.uid = uniqueId;
  }

  @Override
  public String toString()
  {
    return "DeleteEdgeByUid{" + "uniqueId=" + uid + '}';
  }

  public String getUid()
  {
    return uid;
  }

  public void setUid(String uniqueId)
  {
    this.uid = uniqueId;
  }

}
