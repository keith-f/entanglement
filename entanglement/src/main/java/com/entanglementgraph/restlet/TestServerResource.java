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

package com.entanglementgraph.restlet;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * @author Keith Flanagan
 */
public class TestServerResource extends ServerResource {

  @Get
  public String toString() {
    // Print the requested URI path
    return "Resource URI  : " + getReference() + '\n' + "Root URI      : "
        + getRootRef() + '\n' + "Routed part   : "
        + getReference().getBaseRef() + '\n' + "Remaining part: "
        + getReference().getRemainingPart();
  }
}
