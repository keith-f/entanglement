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
 * File created: 08-Nov-2012, 13:34:32
 */

package uk.ac.ncl.aries.entanglement.revlog.data;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevisionItemContainer
{ 
  private Date timestamp;
  private String graphUniqueId;
  private String graphBranchId;
 
  private String uniqueId;
  
  
  private String transactionUid;
  private int txnSubmitId;
  private boolean committed;
  private Date dateCommitted;
  
  List<RevisionItem> items;

  public RevisionItemContainer()
  {
    items = new LinkedList<>();
  }

  @Override
  public String toString() {
    return "RevisionItem{" + "timestamp=" + timestamp + ", graphUniqueId=" + graphUniqueId 
            + ", graphBranchId=" + graphBranchId + ", uniqueId=" + uniqueId 
            + ", transactionUid=" + transactionUid + ", txnSubmitId=" + txnSubmitId
            + ", items=" + items.size() + '}';
  }

  public String getGraphBranchId()
  {
    return graphBranchId;
  }

  public void setGraphBranchId(String graphBranchId)
  {
    this.graphBranchId = graphBranchId;
  }

  public String getGraphUniqueId()
  {
    return graphUniqueId;
  }

  public void setGraphUniqueId(String graphUniqueId)
  {
    this.graphUniqueId = graphUniqueId;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public int getTxnSubmitId() {
    return txnSubmitId;
  }

  public void setTxnSubmitId(int txnSubmitId) {
    this.txnSubmitId = txnSubmitId;
  }
  
  public List<RevisionItem> getItems() {
    return items;
  }

  public void setItems(List<RevisionItem> items) {
    this.items = items;
  }

  public Date getTimestamp()
  {
    return timestamp;
  }

  public void setTimestamp(Date timestamp)
  {
    this.timestamp = timestamp;
  }

  public String getTransactionUid()
  {
    return transactionUid;
  }

  public void setTransactionUid(String transactionUid)
  {
    this.transactionUid = transactionUid;
  }

  public boolean isCommitted() {
    return committed;
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  public Date getDateCommitted() {
    return dateCommitted;
  }

  public void setDateCommitted(Date dateCommitted) {
    this.dateCommitted = dateCommitted;
  }

}
