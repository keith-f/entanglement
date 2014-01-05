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

package com.entanglementgraph.couchdb.revlog.data;

import com.entanglementgraph.revlog.commands.GraphOperation;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * A lightweight object that contains simply a graph operation type and the
 * serialized operation itself. Instances of this class are contained in a 
 * RevisionItemContainer.
 * 
 * @author Keith Flanagan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevisionItem
{ 
  /**
   * Fully qualified Java classname of the GraphOperation, <code>op</code>
   */
  private String type;
  /**
   * The actual graph operation bean
   */
  private GraphOperation op;

  public RevisionItem()
  {
  }

  @Override
  public String toString() {
    return "RevisionItem{" 
            + ", opCls=" + type + ", op=" + op + '}';
  }

  public String getType() {
    return type;
  }

  public void setType(String opType) {
    this.type = opType;
  }

  public GraphOperation getOp() {
    return op;
  }

  public void setOp(GraphOperation operation) {
    this.op = operation;
  }
}
