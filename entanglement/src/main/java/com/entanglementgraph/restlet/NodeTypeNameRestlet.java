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

import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;

/**
 * A Restlet implementation that returns a raw JSON document for the specified node type/name.
 *
 * @author Keith Flanagan
 */
public class NodeTypeNameRestlet extends Restlet {

  private final ObjectMapper mapper = new ObjectMapper();
  private final EntanglementRuntime runtime;
  private final GraphConnection conn;

  public NodeTypeNameRestlet(EntanglementRuntime runtime, GraphConnection conn) {
    this.runtime = runtime;
    this.conn = conn;
  }

  @Override
  public void handle(Request request, Response response) {
    try {
      String type = (String) request.getAttributes().get("type");

      EntityKeys queryKeys = new EntityKeys();
      queryKeys.setType(type);
      Node node = conn.getNodeDao().getByKey(queryKeys);
      String message = mapper.writeValueAsString(node);

      response.setEntity(message, MediaType.TEXT_PLAIN);
    } catch (Exception e) {
      throw new RuntimeException("Request failed", e);
    }

  }
}

