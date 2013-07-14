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
 */

package com.entanglementgraph.shell;

import com.scalesinformatics.util.UidGenerator;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Keith Flanagan
 */
public class EntanglementStatePropertyNames
{
  /*
   * Special state property names
   */
  public static String PROP_HOSTNAME = "mongo_hostname";
  public static String PROP_DB_NAME = "database_name";
  public static String PROP_GRAPH_NAME = "graph_name";
  public static String PROP_GRAPH_BRANCH_NAME = "graph_branch";
  public static String PROP_TXN_UID = "transaction_uid";
  public static String PROP_TXN_SUBMIT_ID = "transaction_submit_id";
  
  public static String PROP_DEFAULT_DATASOURCE_NAME = "default_datasource";
  public static String PROP_DEFAULT_EVIDENCETYPE_NAME = "default_evidencetype";

  
  
  
  public static Map<String, String> getDefaultPropertySettings()
  {
    Map<String, String> defaults = new HashMap<>();
    defaults.put(PROP_HOSTNAME, "localhost");
    defaults.put(PROP_DB_NAME, "testing");
    defaults.put(PROP_GRAPH_NAME, "test_graph");
    defaults.put(PROP_GRAPH_BRANCH_NAME, "trunk");
    defaults.put(PROP_TXN_UID, UidGenerator.generateUid());
    defaults.put(PROP_TXN_SUBMIT_ID, "0");
    
    defaults.put(PROP_DEFAULT_DATASOURCE_NAME, "default");
    defaults.put(PROP_DEFAULT_EVIDENCETYPE_NAME, "default");

    
    return defaults;
  }
  
}
