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

package com.entanglementgraph.couchdb.revlog.commands;

import com.entanglementgraph.revlog.commands.GraphOperation;

import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class TransactionBegin
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(com.entanglementgraph.revlog.commands.TransactionBegin.class.getName());
  
  private String uid; 
  

  public TransactionBegin()
  {
  }

  public TransactionBegin(String transactionUid)
  {
    this.uid = transactionUid;
  }

  @Override
  public String toString()
  {
    return "TransactionBegin{" + "transactionUid=" + uid + '}';
  }

  public String getUid()
  {
    return uid;
  }

  public void setUid(String transactionUid)
  {
    this.uid = transactionUid;
  }
}
