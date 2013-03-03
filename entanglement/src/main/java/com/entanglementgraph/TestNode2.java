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

package com.entanglementgraph;

import java.util.HashMap;
import java.util.Map;
import com.entanglementgraph.graph.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public class TestNode2
    extends Node
{
  private String string1;
  private String string2;
  private double doubleNumber;
  private float anotherNumber;
  private Map<String, Integer> mapOfThings;
  
  public TestNode2()
  {
    mapOfThings = new HashMap<>();
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

  public double getDoubleNumber() {
    return doubleNumber;
  }

  public void setDoubleNumber(double doubleNumber) {
    this.doubleNumber = doubleNumber;
  }

  public float getAnotherNumber() {
    return anotherNumber;
  }

  public void setAnotherNumber(float anotherNumber) {
    this.anotherNumber = anotherNumber;
  }

  public Map<String, Integer> getMapOfThings() {
    return mapOfThings;
  }

  public void setMapOfThings(Map<String, Integer> mapOfThings) {
    this.mapOfThings = mapOfThings;
  }
}
