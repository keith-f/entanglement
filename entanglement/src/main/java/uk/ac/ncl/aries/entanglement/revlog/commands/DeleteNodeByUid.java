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

package uk.ac.ncl.aries.entanglement.revlog.commands;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class DeleteNodeByUid
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(DeleteNodeByUid.class.getName());
  
  /*
   * Node ID
   */
  private String uid;
  

  public DeleteNodeByUid()
  {
  }

  @Override
  public String toString()
  {
    return "DeleteNodeByUid{" + "uniqueId=" + uid + '}';
  }

  public DeleteNodeByUid(String uniqueId)
  {
    this.uid = uniqueId;
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
