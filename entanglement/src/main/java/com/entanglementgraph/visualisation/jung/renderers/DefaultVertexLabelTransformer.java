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

import com.mongodb.DBObject;
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
  private final VisualizationViewer<V, E> vv;
//  private final V v;

  public DefaultVertexLabelTransformer(VisualizationViewer<V, E> vv) {
    this.vv = vv;
//    this.v = v;
  }

  @Override
  public String transform(V v) {
    return (String) ((DBObject) v.get("keys")).get("type");
  }

}
