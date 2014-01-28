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

package com.entanglementgraph.graph.mongodb;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 24/02/2013
 * Time: 13:12
 * To change this template use File | Settings | File Templates.
 */
public class GraphConnectionFactoryException extends Exception {
  public GraphConnectionFactoryException() {
  }

  public GraphConnectionFactoryException(String message) {
    super(message);
  }

  public GraphConnectionFactoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public GraphConnectionFactoryException(Throwable cause) {
    super(cause);
  }
}
