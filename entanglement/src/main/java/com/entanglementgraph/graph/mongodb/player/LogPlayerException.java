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
 * File created: 15-Nov-2012, 17:12:00
 */

package com.entanglementgraph.graph.mongodb.player;

/**
 *
 * @author Keith Flanagan
 */
public class LogPlayerException
    extends Exception
{
  public LogPlayerException(Throwable cause)
  {
    super(cause);
  }

  public LogPlayerException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public LogPlayerException(String message)
  {
    super(message);
  }

  public LogPlayerException()
  {
  }

}
