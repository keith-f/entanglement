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

package uk.ac.ncl.aries.entanglement;

import uk.ac.ncl.aries.entanglement.player.data.Edge;

/**
 *
 * @author Keith Flanagan
 */
public class TestEdge1
    extends Edge
{
  private String someProperty;
  
  public TestEdge1()
  {
    this.type = TestEdge1.class.getSimpleName();
  }

  public String getSomeProperty() {
    return someProperty;
  }

  public void setSomeProperty(String someProperty) {
    this.someProperty = someProperty;
  }
}
