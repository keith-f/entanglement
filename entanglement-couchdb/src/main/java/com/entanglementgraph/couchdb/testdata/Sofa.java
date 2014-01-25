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

package com.entanglementgraph.couchdb.testdata;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.graph.data.Node;
import org.codehaus.jackson.annotate.*;

/**
 * @author Keith Flanagan
 */
//public class Sofa extends NewNode2<Sofa> implements NodeContent {
public class Sofa implements NodeContent {

  private String color;
  private int numSeats;

  public Sofa() {
  }


  public void setColor(String s) {
    color = s;
  }

  public String getColor() {
    return color;
  }

  public int getNumSeats() {
    return numSeats;
  }

  public void setNumSeats(int numSeats) {
    this.numSeats = numSeats;
  }
}
