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

package com.entanglementgraph.irc;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 14/05/2013
 * Time: 12:47
 * To change this template use File | Settings | File Templates.
 */
public class EntanglementBotException extends Exception {
  public EntanglementBotException() {
  }

  public EntanglementBotException(String message) {
    super(message);
  }

  public EntanglementBotException(String message, Throwable cause) {
    super(message, cause);
  }

  public EntanglementBotException(Throwable cause) {
    super(cause);
  }

  public EntanglementBotException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
