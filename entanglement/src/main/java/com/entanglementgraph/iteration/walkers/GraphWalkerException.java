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
package com.entanglementgraph.iteration.walkers;

/**
 * User: keith
 * Date: 19/08/13; 15:05
 *
 * @author Keith Flanagan
 */
public class GraphWalkerException extends Exception {
  public GraphWalkerException() {
  }

  public GraphWalkerException(String message) {
    super(message);
  }

  public GraphWalkerException(String message, Throwable cause) {
    super(message, cause);
  }

  public GraphWalkerException(Throwable cause) {
    super(cause);
  }

  public GraphWalkerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}