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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.ektorp.CouchDbConnector;
import org.ektorp.impl.StdObjectMapperFactory;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class ExtStdObjectMapperFactory extends StdObjectMapperFactory {
  private static final Logger logger = Logger.getLogger(ExtStdObjectMapperFactory.class.getName());

  private final Map<Class, String> classJsonMappings;

  private ObjectMapper objectMapper;

  public ObjectMapper getLastCreatedObjectMapper() {
    return objectMapper;
  }

  public ExtStdObjectMapperFactory(Map<Class, String> classJsonMappings) {
    this.classJsonMappings = classJsonMappings;
  }

  @Override
  public synchronized ObjectMapper createObjectMapper() {
    logger.info("CreateObjectMapper being called");
    return super.createObjectMapper();
  }

  @Override
  public ObjectMapper createObjectMapper(CouchDbConnector connector) {
    logger.info("CreateObjectMapper(CouchDbConnector ...) being called");
    objectMapper = super.createObjectMapper(connector);

    logger.info("Adding serialisation customisations");
    // Exclude NULL and empty values
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

//    objectMapper.registerSubtypes(new NamedType(Sofa.class, "Sfa"));
//    objectMapper.registerSubtypes(new NamedType(Pillow.class, "pillow"));
//    objectMapper.registerSubtypes(new NamedType(HasPillow.class, "has-pillow"));
//    objectMapper.registerSubtypes(new NamedType(MapContent.class, "map-content"));
////
////    om.registerSubtypes(new NamedType(NodeWithContent.class, "NodeWithContent"));
//    objectMapper.registerSubtypes(new NamedType(GeneContent.class, "GC"));

    for (Map.Entry<Class, String> mapping : classJsonMappings.entrySet()) {
      logger.info("Adding mapping "+mapping.getKey()+" --> "+mapping.getValue());
      objectMapper.registerSubtypes(new NamedType(mapping.getKey(), mapping.getValue()));
    }


    return objectMapper;
  }


}
