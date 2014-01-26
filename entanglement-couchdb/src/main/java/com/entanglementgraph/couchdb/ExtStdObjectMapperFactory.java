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

import com.entanglementgraph.couchdb.testdata.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.ektorp.CouchDbConnector;
import org.ektorp.impl.StdObjectMapperFactory;

import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class ExtStdObjectMapperFactory extends StdObjectMapperFactory {
  private static final Logger logger = Logger.getLogger(ExtStdObjectMapperFactory.class.getName());

  @Override
  public synchronized ObjectMapper createObjectMapper() {
    logger.info("CreateObjectMapper being called");
    return super.createObjectMapper();
  }

  @Override
  public ObjectMapper createObjectMapper(CouchDbConnector connector) {
    logger.info("CreateObjectMapper(CouchDbConnector ...) being called");
    ObjectMapper om = super.createObjectMapper(connector);

    logger.info("Adding serialisation customisations");
    // Exclude NULL and empty values
    om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

//    om.addMixInAnnotations(Rectangle.class, MixIn.class);
//    om.addMixInAnnotations(NewNode.class, ANode.class);
//    om.addMixInAnnotations(NewNode.class, BNode.class);

//    om.addMixInAnnotations(NewNode.class, ABNodeMixinTest.class);

//    om.getSerializationConfig().
//    om.registerSubtypes(new NamedType(ANode.class, "a-node3"));
//    om.registerSubtypes(new NamedType(BNode.class, "b-node3"));
//
    om.registerSubtypes(new NamedType(Sofa.class, "Sfa"));
    om.registerSubtypes(new NamedType(Pillow.class, "pillow"));
    om.registerSubtypes(new NamedType(HasPillow.class, "has-pillow"));
    om.registerSubtypes(new NamedType(MapContent.class, "map-content"));
//
//    om.registerSubtypes(new NamedType(NodeWithContent.class, "NodeWithContent"));
    om.registerSubtypes(new NamedType(GeneContent.class, "GC"));


    return om;
  }


}
