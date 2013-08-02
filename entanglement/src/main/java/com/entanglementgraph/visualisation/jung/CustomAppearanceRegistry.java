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

package com.entanglementgraph.visualisation.jung;

import com.entanglementgraph.graph.AbstractGraphEntityDAO;
import com.mongodb.DBObject;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Keith Flanagan
 */
public class CustomAppearanceRegistry implements CustomVertexAppearance {
  /**
   * A set of node types to custom visualisation providers.
   */
  private final Map<String, Class<? extends CustomVertexAppearance>> entanglementTypeToAppearance;

  /**
   * A map of individual node values to renderer instances. Each graph data value that requires a custom renderer
   * gets its own instance of a renderer, allowing the renderer to maintain state from one frame to the next.
   * This map stores graph entity -> renderer instance mappings
   */
  private final Map<DBObject, CustomVertexAppearance> valueToRenderer;

  public CustomAppearanceRegistry() {
    entanglementTypeToAppearance = new HashMap<>();
    valueToRenderer = new HashMap<>();
  }

  @Override
  public Transformer<DBObject, Icon> getIconTransformer() {
    return new Transformer<DBObject, Icon>() {
      @Override
      public Icon transform(DBObject data) {
        CustomVertexAppearance customRenderer = findRendererForValue(data);
        return customRenderer == null
            ? null
            : customRenderer.getIconTransformer().transform(data);
      }
    };
  }


  @Override
  public Transformer<DBObject, Shape> getShapeTransformer() {
    return new Transformer<DBObject, Shape>() {
      @Override
      public Shape transform(DBObject data) {
        CustomVertexAppearance customRenderer = findRendererForValue(data);
        return customRenderer == null
            ? null
            : customRenderer.getShapeTransformer().transform(data);
      }
    };
  }

  @Override
  public Transformer<DBObject, String> getVertexLabelTransformer() {
    return new Transformer<DBObject, String>() {
      @Override
      public String transform(DBObject data) {
        CustomVertexAppearance customRenderer = findRendererForValue(data);
        return customRenderer == null
            ? null
            : customRenderer.getVertexLabelTransformer().transform(data);
      }
    };
  }

  @Override
  public Transformer<DBObject, String> getTooltipTransformer() {
    return new Transformer<DBObject, String>() {
      @Override
      public String transform(DBObject data) {
        CustomVertexAppearance customRenderer = findRendererForValue(data);
        return customRenderer == null
            ? null
            : customRenderer.getTooltipTransformer().transform(data);
      }
    };
  }

  /**
   * Given a DBObject (representing an Entanglement node), find a renderer type and instantiate it (if there's
   * a match).
   *
   * @param data
   * @return
   */
  private CustomVertexAppearance findRendererForValue(DBObject data) {
    CustomVertexAppearance renderer = valueToRenderer.get(data);
    if (renderer == null) {
      renderer = createRendererForValue(data);
      valueToRenderer.put(data, renderer);
    }
    return renderer;
  }


  private CustomVertexAppearance createRendererForValue(DBObject data) {
    String nodeType =  (String) ((DBObject) data.get(AbstractGraphEntityDAO.FIELD_KEYS)).get("type");
    Class<? extends CustomVertexAppearance> rendererType = entanglementTypeToAppearance.get(nodeType);

    try {
      CustomVertexAppearance renderer =  rendererType.newInstance();
      return renderer;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create renderer instance of type: "+rendererType+" for data item: "+data, e);
    }
  }

}
