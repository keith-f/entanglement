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

import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * User: keith
 * Date: 19/11/13; 16:17
 *
 * Try the following with these:
 *
 *
 * http://localhost:8182/resource/abc/def?param=123
 *
 * http://localhost:8182/users/abc/def?param=123
 *
 *
 * @author Keith Flanagan
 */
public class TestServer { //extends ServerResource {

  public static void main(String[] args) throws Exception {
    // Create a new Restlet component and add a HTTP server connector to it
    Component component = new Component();
    component.getServers().add(Protocol.HTTP, 8182);

    // Then attach it to the local host
    component.getDefaultHost().attach("/resource", TestServerResource.class);
    component.getDefaultHost().attach("/users/{user}", new TestRestlet());

    // Now, let's start the component!
    // Note that the HTTP server connector is also automatically started.
    component.start();
  }
}

