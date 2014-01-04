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

import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import org.restlet.Component;
import org.restlet.data.Protocol;

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
public class EntanglementRestServer {

  private final EntanglementRuntime runtime;
  private final Component component;

  public EntanglementRestServer(int port, EntanglementRuntime runtime, GraphConnection conn) {
    this.runtime = runtime;

    // Create a new Restlet component and add a HTTP server connector to it
    component = new Component();
    component.getServers().add(Protocol.HTTP, port);

    NodeUidRestlet nodeUidRestlet = new NodeUidRestlet(runtime, conn);
    NodeTypeNameRestlet nodeTypeNameRestlet = new NodeTypeNameRestlet(runtime, conn);

    EdgeUidRestlet edgeUidRestlet = new EdgeUidRestlet(runtime, conn);
    EdgeTypeNameRestlet edgeTypeNameRestlet = new EdgeTypeNameRestlet(runtime, conn);

    // Then attach it to the local host
    component.getDefaultHost().attach("/test/{user}", new TestRestlet());
    component.getDefaultHost().attach("/test-resource", TestServerResource.class);
    component.getDefaultHost().attach("/nodes/uid/{node}", nodeUidRestlet);
    component.getDefaultHost().attach("/nodes/type-name/{type}/{name}", nodeTypeNameRestlet);
    component.getDefaultHost().attach("/edges/uid/{node}", edgeUidRestlet);
    component.getDefaultHost().attach("/edges/type-name/{type}/{name}", edgeTypeNameRestlet);
  }

  public EntanglementRuntime getRuntime() {
    return runtime;
  }

  public Component getComponent() {
    return component;
  }

  //  private static HazelcastInstance createHazelcastInstance(String hazelcastClusterName, String[] bindAddresses)
//      throws UnknownHostException {
//    DefaultHazelcastConfig hzConfig = new DefaultHazelcastConfig(hazelcastClusterName, hazelcastClusterName);
//    if (bindAddresses.length == 0) {
//      String hostname = InetAddress.getLocalHost().getHostAddress();
//      hzConfig.specifyNetworkInterfaces(hostname);
//    } else {
//      hzConfig.specifyNetworkInterfaces(bindAddresses);
//    }
//    HazelcastInstance hzInstance = Hazelcast.newHazelcastInstance(hzConfig);
//    return hzInstance;
//  }

//  private static void startServer(EntanglementRestServer server) throws Exception {
//    // Now, let's start the component!
//    // Note that the HTTP server connector is also automatically started.
//    server.component.start();
//  }

}

