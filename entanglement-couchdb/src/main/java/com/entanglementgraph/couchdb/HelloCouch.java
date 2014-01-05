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

package com.entanglementgraph.couchdb;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.net.MalformedURLException;

/**
 * @author Keith Flanagan
 */
public class HelloCouch {
  public static void main(String[] args) throws Exception {
    HttpClient httpClient = new StdHttpClient.Builder()
        .url("http://localhost:5984")
//        .username("admin")
//        .password("secret")
        .build();

    CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
// if the second parameter is true, the database will be created if it doesn't exists
    CouchDbConnector db = dbInstance.createConnector("my_first_database", true);

    System.out.println("Connected");

    db.get
  }
}
