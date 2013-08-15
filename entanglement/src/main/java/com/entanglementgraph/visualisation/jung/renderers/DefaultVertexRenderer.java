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

import com.entanglementgraph.visualisation.jung.Visualiser;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * User: keith
 * Date: 15/08/13; 13:50
 *
 * @author Keith Flanagan
 */
public class DefaultVertexRenderer implements CustomVertexRenderer {
  private static final Logger logger = Logger.getLogger(DefaultVertexRenderer.class.getName());

  protected DbObjectMarshaller marshaller;
  protected Visualiser visualiser;
  protected EntityDisplayNameRegistry displayNameFactories;
  private DefaultVertexLabelTransformer defaultVertexLabelTransformer;

  @Override
  public void setVisualiser(Visualiser visualiser) {
    this.visualiser = visualiser;
    defaultVertexLabelTransformer = new DefaultVertexLabelTransformer(visualiser.getVv(), marshaller, displayNameFactories);
  }

  @Override
  public Transformer<DBObject, Icon> getVertexIconTransformer() {
    return new Transformer<DBObject, Icon>() {
      @Override
      public Icon transform(DBObject data) {
        return new DefaultNodeIcon<>(visualiser.getVv(), data);
      }
    };
  }

  @Override
  public Transformer<DBObject, String> getVertexLabelTransformer() {
    return defaultVertexLabelTransformer;
  }

  @Override
  public Transformer<DBObject, String> getTooltipTransformer() {
    return defaultVertexLabelTransformer;
  }

  @Override
  public void setMarshaller(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }

  @Override
  public void setDisplayNameFactories(EntityDisplayNameRegistry displayNameFactories) {
    this.displayNameFactories = displayNameFactories;
  }
}
