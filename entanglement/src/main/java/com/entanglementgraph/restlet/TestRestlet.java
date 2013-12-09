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

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * @author Keith Flanagan
 */
public class TestRestlet extends Restlet {

  @Override
  public void handle(Request request, Response response) {
    // Print the requested URI path
    String message = "Account of user \""
        + request.getAttributes().get("user") + "\"";

    message = message + "\n\n";

    message = message + "Resource URI  : " + request.getResourceRef() + '\n' + "Root URI      : "
        + request.getRootRef() + '\n' + "Routed part   : "
        + request.getResourceRef().getBaseRef() + '\n' + "Remaining part: "
        + request.getResourceRef().getRemainingPart();


    response.setEntity(message, MediaType.TEXT_PLAIN);

  }
}
