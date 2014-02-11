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

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.util.GraphConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.CouchDbConnector;

/**
 * A CouchDB GraphConnection.
 *
 * @author Keith Flanagan
 */
public class CouchGraphConnection extends GraphConnection {
  private CouchDbConnector db;

  private ObjectMapper om;



  public CouchGraphConnection() {
  }

  public CouchDbConnector getDb() {
    return db;
  }

  public void setDb(CouchDbConnector db) {
    this.db = db;
  }

  public ObjectMapper getOm() {
    return om;
  }

  public void setOm(ObjectMapper om) {
    this.om = om;
  }
}
