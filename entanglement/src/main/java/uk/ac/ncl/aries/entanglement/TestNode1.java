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

import java.util.ArrayList;
import java.util.List;
import uk.ac.ncl.aries.entanglement.player.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public class TestNode1
    extends Node
{
  private String string1;
  private String string2;
  private int number;
  private List<String> listOfThings;
  
  public TestNode1()
  {
    listOfThings = new ArrayList<>();
  }

  public String getString1() {
    return string1;
  }

  public void setString1(String string1) {
    this.string1 = string1;
  }

  public String getString2() {
    return string2;
  }

  public void setString2(String string2) {
    this.string2 = string2;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public List<String> getListOfThings() {
    return listOfThings;
  }

  public void setListOfThings(List<String> listOfThings) {
    this.listOfThings = listOfThings;
  }
}
