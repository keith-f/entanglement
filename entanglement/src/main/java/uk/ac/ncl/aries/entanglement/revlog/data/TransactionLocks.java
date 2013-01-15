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
 * File created: 07-Dec-2012, 12:42:36
 */

package uk.ac.ncl.aries.entanglement.revlog.data;

/**
 * NOTE: currently not used
 * 
 * 
 * @author Keith Flanagan
 */
public class TransactionLocks
{
  private String transactionUid;
  private String entityUid;

  public TransactionLocks()
  {
  }

  public TransactionLocks(String transactionUid, String entityUid)
  {
    this.transactionUid = transactionUid;
    this.entityUid = entityUid;
  }

  public String getTransactionUid()
  {
    return transactionUid;
  }

  public void setTransactionUid(String transactionUid)
  {
    this.transactionUid = transactionUid;
  }

  public String getEntityUid()
  {
    return entityUid;
  }

  public void setEntityUid(String entityUid)
  {
    this.entityUid = entityUid;
  }
}
