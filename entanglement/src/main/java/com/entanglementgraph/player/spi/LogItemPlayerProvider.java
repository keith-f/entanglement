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
 * File created: 27-Nov-2012, 11:42:11
 */

package com.entanglementgraph.player.spi;

import com.entanglementgraph.util.GraphConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class LogItemPlayerProvider
{
  private static final Logger logger = 
          Logger.getLogger(LogItemPlayerProvider.class.getName());
  
  private final GraphConnection graphConn;


  private final ServiceLoader<LogItemPlayer> logItemPlayerLoader;
  private final Map<String, LogItemPlayer> typeToProvider;

  public LogItemPlayerProvider(GraphConnection graphConn)
  {
    this.graphConn = graphConn;
    logger.info("Using classloader: "+graphConn.getClassLoader());
    logItemPlayerLoader = ServiceLoader.load(LogItemPlayer.class, graphConn.getClassLoader());
    typeToProvider = new HashMap<>();
  }

//  public LogItemPlayerProvider(ClassLoader cl, DbObjectMarshaller marshaller)
//  {
//    this.marshaller = marshaller;
//    logger.info("Using classloader: "+cl);
//    logItemPlayerLoader = ServiceLoader.load(LogItemPlayer.class, cl);
//    typeToProvider = new HashMap<>();
//  }

  public LogItemPlayer getPlayerFor(String itemType)
      throws LogItemPlayerProviderException
  {
    if (typeToProvider.containsKey(itemType)) {
      return typeToProvider.get(itemType);
    }
    
    for (LogItemPlayer impl : logItemPlayerLoader) {
      if (impl.getSupportedLogItemType().equals(itemType)) {
        impl.setGraphConnection(graphConn);
        typeToProvider.put(itemType, impl);
        return impl;
      }
    }
    
    throw new LogItemPlayerProviderException(
        "No item player implementation could be found for log item type: "+itemType);
  }
}
