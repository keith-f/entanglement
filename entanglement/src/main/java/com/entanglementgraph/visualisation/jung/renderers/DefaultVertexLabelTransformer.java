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
package com.entanglementgraph.visualisation.jung.renderers;

import com.entanglementgraph.graph.data.EntityKeys;
import com.entanglementgraph.util.MongoUtils;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.apache.commons.collections15.Transformer;

import java.util.logging.Logger;

/**
 * User: keith
 * Date: 07/08/13; 14:48
 *
 * @author Keith Flanagan
 */
public class DefaultVertexLabelTransformer<V extends DBObject, E> implements Transformer<V, String> {
  private static final Logger logger = Logger.getLogger(DefaultVertexLabelTransformer.class.getName());

  protected DbObjectMarshaller marshaller;
  private final EntityDisplayNameRegistry displayNameFactories;
  private final VisualizationViewer<V, E> vv;
  private boolean enabled;

  public DefaultVertexLabelTransformer(VisualizationViewer<V, E> vv, DbObjectMarshaller marshaller,
                                       EntityDisplayNameRegistry displayNameFactories) {
    this.vv = vv;
    this.marshaller = marshaller;
    this.displayNameFactories = displayNameFactories;
    this.enabled = true;
  }

  @Override
  public String transform(V vertexData) {
    if (!enabled) {
      return "";
    }
    try {
      //This is potentially expensive when rendering lots of nodes, but icons should be cached and reused for most frames
      EntityKeys<?> keys = MongoUtils.parseKeyset(marshaller, vertexData);
      return displayNameFactories.createNameForEntity(keys);
    } catch (DbObjectMarshallerException e) {
      e.printStackTrace();
      return "Rendering Error!";
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
