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

package com.entanglementgraph.couchdb.revlog.data;

import java.util.Date;

/**
 * NOTE: currently not used
 * 
 * 
 * There is exactly one instance of this class for each transaction.
 * 
 * @author Keith Flanagan
 */
public class TransactionItem
{
  private String txnId;
  private String firstRevItemInTxn;
  private String lastInsertedRevItem;
  
  private Date began;
  private Date ended;
  private Date lastActivity;
  
  private boolean committed;
  private boolean rolledBack;
  
  

  public TransactionItem() {
  }

  public String getTxnId() {
    return txnId;
  }

  public void setTxnId(String txnId) {
    this.txnId = txnId;
  }

  public boolean isCommitted() {
    return committed;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  public String getLastInsertedRevItem() {
    return lastInsertedRevItem;
  }

  public void setLastInsertedRevItem(String lastInsertedRevItem) {
    this.lastInsertedRevItem = lastInsertedRevItem;
  }

  public Date getBegan() {
    return began;
  }

  public void setBegan(Date began) {
    this.began = began;
  }

  public Date getEnded() {
    return ended;
  }

  public void setEnded(Date ended) {
    this.ended = ended;
  }

  public boolean isRolledBack() {
    return rolledBack;
  }

  public void setRolledBack(boolean rolledBack) {
    this.rolledBack = rolledBack;
  }

  public String getFirstRevItemInTxn() {
    return firstRevItemInTxn;
  }

  public void setFirstRevItemInTxn(String firstRevItemInTxn) {
    this.firstRevItemInTxn = firstRevItemInTxn;
  }

  public Date getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(Date lastActivity) {
    this.lastActivity = lastActivity;
  }
  
}
