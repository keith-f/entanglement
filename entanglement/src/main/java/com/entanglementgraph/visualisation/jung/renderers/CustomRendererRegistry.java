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

import com.entanglementgraph.graph.AbstractGraphEntityDAO;
import com.entanglementgraph.visualisation.jung.Visualiser;
import com.entanglementgraph.visualisation.text.EntityDisplayNameRegistry;
import com.mongodb.DBObject;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
public class CustomRendererRegistry implements CustomVertexRenderer {
  private DbObjectMarshaller marshaller;
  private EntityDisplayNameRegistry displayNameFactories;
  private BasicVisualizationServer<DBObject, DBObject> visualiser;

  /**
   * A set of node types to custom visualisation providers.
   */
  private final Map<String, Class<? extends CustomVertexRenderer>> entanglementTypeToAppearance;

  /**
   * A map of individual node values to renderer instances. Each graph data value that requires a custom renderer
   * gets its own instance of a renderer, allowing the renderer to maintain state from one frame to the next.
   * This map stores graph entity -> renderer instance mappings
   */
  private final Map<DBObject, CustomVertexRenderer> valueToRenderer;


  public CustomRendererRegistry(DbObjectMarshaller marshaller, EntityDisplayNameRegistry displayNameFactories) {
    this.marshaller = marshaller;
    this.displayNameFactories = displayNameFactories;
    entanglementTypeToAppearance = new HashMap<>();
    valueToRenderer = new HashMap<>();
  }

  @Override
  public void setMarshaller(DbObjectMarshaller marshaller) {
    this.marshaller = marshaller;
  }

  @Override
  public void setDisplayNameFactories(EntityDisplayNameRegistry displayNameFactories) {
    this.displayNameFactories = displayNameFactories;
  }

  public void addTypeToRendererMapping(String nodeType, Class<? extends CustomVertexRenderer> renderer) {
    entanglementTypeToAppearance.put(nodeType, renderer);
  }

  @Override
  public void setVisualiser(BasicVisualizationServer<DBObject, DBObject> visualiser) {
    this.visualiser = visualiser;
  }

  @Override
  public Transformer<DBObject, Icon> getVertexIconTransformer() {
    return new Transformer<DBObject, Icon>() {
      @Override
      public Icon transform(DBObject data) {
        return findRendererForValue(data).getVertexIconTransformer().transform(data);
      }
    };
  }

  @Override
  public Transformer<DBObject, String> getVertexLabelTransformer() {
    return new Transformer<DBObject, String>() {
      @Override
      public String transform(DBObject data) {
        return findRendererForValue(data).getVertexLabelTransformer().transform(data);
      }
    };
  }

  @Override
  public Transformer<DBObject, String> getTooltipTransformer() {
    return new Transformer<DBObject, String>() {
      @Override
      public String transform(DBObject data) {
        return findRendererForValue(data).getTooltipTransformer().transform(data);
      }
    };
  }

  /**
   * Given a DBObject (representing an Entanglement node), return an existing renderer, or instantiate a new one
   * if we haven't seen this data item before.
   *
   * @param data
   * @return a renderer instance for use with the <code>data</code> item, or NULL if no custom renderer could be found.
   */
  private CustomVertexRenderer findRendererForValue(DBObject data) {
    CustomVertexRenderer renderer = valueToRenderer.get(data);
    if (renderer == null) {
      renderer = createRendererForValue(data);
      valueToRenderer.put(data, renderer);
    }
    return renderer;
  }


  private CustomVertexRenderer createRendererForValue(DBObject data) {
    String nodeType =  (String) ((DBObject) data.get(AbstractGraphEntityDAO.FIELD_KEYS)).get("type");
    Class<? extends CustomVertexRenderer> rendererType = entanglementTypeToAppearance.get(nodeType);
    if (rendererType == null) {
      //No custom renderer could be found for this data value. Use the default renderer.
      rendererType = DefaultVertexRenderer.class;
    }
    try {
      CustomVertexRenderer renderer =  rendererType.newInstance();
      renderer.setMarshaller(marshaller);
      renderer.setDisplayNameFactories(displayNameFactories);
      renderer.setVisualiser(visualiser);
      return renderer;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create renderer instance of type: "+rendererType+" for data item: "+data, e);
    }
  }

}
