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

package uk.ac.ncl.aries.entanglement.revlog.commands;

import java.util.logging.Logger;

/**
 * Allows a one-off update to an existing edge's <code>from*</code> and/or 
 * <code>to*</code> node fields in order to make an existing 'hanging' edge 
 * non-hanging, or less hanging than it was.
 * 
 * This command no effect if:
 * <ul>
 * <li>The specified edge is not hanging</li>
 * </ul>
 * 
 * @author Keith Flanagan
 */
public class FixHangingEdge
    extends GraphOperation
{
  private static final Logger logger = 
      Logger.getLogger(FixHangingEdge.class.getName());
  
  private String edgeUid;
  /**
   * Set this true if you want the associated command player to throw an
   * exception if the specified edge doesn't exist. Default behaviour is to
   * ignore missing edges.
   */
  private boolean errOnMissingEdge;
  
  protected String fromUid;
  protected String fromType;
  protected String fromName;
  
  protected String toUid;
  protected String toType;
  protected String toName;
  
  public FixHangingEdge()
  {
  }

  public String getEdgeUid() {
    return edgeUid;
  }

  public void setEdgeUid(String edgeUid) {
    this.edgeUid = edgeUid;
  }

  public String getFromUid() {
    return fromUid;
  }

  public void setFromUid(String fromUid) {
    this.fromUid = fromUid;
  }

  public String getFromType() {
    return fromType;
  }

  public void setFromType(String fromType) {
    this.fromType = fromType;
  }

  public String getToUid() {
    return toUid;
  }

  public void setToUid(String toUid) {
    this.toUid = toUid;
  }

  public String getToType() {
    return toType;
  }

  public void setToType(String toType) {
    this.toType = toType;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public String getToName() {
    return toName;
  }

  public void setToName(String toName) {
    this.toName = toName;
  }

  public boolean isErrOnMissingEdge() {
    return errOnMissingEdge;
  }

  public void setErrOnMissingEdge(boolean errOnMissingEdge) {
    this.errOnMissingEdge = errOnMissingEdge;
  }
  
}
