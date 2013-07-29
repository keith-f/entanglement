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
package com.entanglementgraph.visualisation.jgraphx;

import com.entanglementgraph.irc.EntanglementRuntime;
import com.entanglementgraph.util.GraphConnection;
import com.entanglementgraph.visualisation.jgraphx.renderers.CustomCellRenderer;
import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxImageCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * An extension of the <code>mxGraph</code> class that can be used to implement custom cell renderers.
 * One use for this is for displaying charts within certain types of node.
 *
 * This class is based on the JGraphX "custom canvas" example.
 *
 * @author Keith Flanagan
 */
public class EntanglementMxGraph extends mxGraph {
  /**
   * The following are set by the EntanglementMxGraph constructor and are passed on to new instances of
   * custom renderers. This map can be used to pass on runtime configuration information, such as variables,
   * display properties, graph connections, file paths, or anything else that a custom renderer might need to
   * complete its task,
   */
  private final Map<String, Object> rendererProperties;


  /**
   * A map of bean type to cell renderer type
   */
  private final Map<Class, Class<? extends CustomCellRenderer>> beanTypeToCustomRendererType;

//  /**
//   * A map of bean instance to cell renderer type - useful for overriding the bean class mappings for particular
//   * bean instances.
//   */
//  private final Map<Class, CustomCellRenderer> beanInstanceToCustomRendererType;

  /**
   * A map of bean instance to cell renderer instance. This is used to store renderer instances for all on-screen
   * objects. Storing renderer instances here saves creating them for each frame to be rendered and is extremely
   * useful where renderers are heavyweight objects - such as JFreeChart instances.
   */
  private final Map<Object, CustomCellRenderer> beanInstanceToCustomRenderer;

  public EntanglementMxGraph() {
    beanTypeToCustomRendererType = new HashMap<>();
    beanInstanceToCustomRenderer = new HashMap<>();
    rendererProperties = new HashMap<>();
  }

  public EntanglementMxGraph(Map<String, Object> rendererProperties) {
    beanTypeToCustomRendererType = new HashMap<>();
    beanInstanceToCustomRenderer = new HashMap<>();
    this.rendererProperties = rendererProperties;
  }

  @Override
  public void drawState(mxICanvas canvas, mxCellState state, boolean drawLabel)
  {
    final Object cellObj = state.getCell(); // (most likely of type: com.mxgraph.model.mxCell)

//    Class cellValueType = null;  // The type of the data bean associated with the graph cell to be rendered
    Object cellValue = null;     // The data bean associated with the graph cell to be rendered
    if (cellObj instanceof mxCell) {
      mxCell cell = (mxCell) cellObj;
      cellValue = cell.getValue();
    }


    String label = null; // A basic label to be used when no custom renderer is specified.
    JComponent centralContent = null;
    CustomCellRenderer customCellRenderer = getRendererForCellValue(cellValue);
    if (customCellRenderer != null) {
      label = (drawLabel) ? state.getLabel() : "";
      centralContent = customCellRenderer.renderCellContent(cellValue);
    }
//    System.out.println("Cell type: "+cellValueType+", customCellRenderer: "+customCellRenderer);

    // Indirection for wrapped swing canvas inside image canvas (used for creating
    // the preview image when cells are dragged)
    if (customCellRenderer != null && getModel().isVertex(cellValue)
        && canvas instanceof mxImageCanvas
        && ((mxImageCanvas) canvas).getGraphicsCanvas() instanceof EntanglementPassThroughCanvas)
    {
      ((EntanglementPassThroughCanvas) ((mxImageCanvas) canvas).getGraphicsCanvas()).drawVertex(state, cellValue, customCellRenderer);
    }
    // Redirection of drawing vertices in SwingCanvas
    else if (customCellRenderer != null
        && getModel().isVertex(state.getCell())
        && canvas instanceof EntanglementPassThroughCanvas)
    {
      ((EntanglementPassThroughCanvas) canvas).drawVertex(state, cellValue, customCellRenderer);
    }
    else
    {
      // Use the default superclass renderer
      super.drawState(canvas, state, drawLabel);
    }
  }

  /**
   * Given a data bean (<code>cellValue</code>) associated with a graph cell, returns an existing renderer instance,
   * or creates a new one if possible.
   *
   * @param cellValue the data bean associated with the graph cell currently being rendered.
   * @return a CustomCellRenderer for the <code>cellValue</code>, assuming that a) cellValue != null; b) a suitable
   * renderer could be found.
   */
  private CustomCellRenderer getRendererForCellValue(Object cellValue)  {
    if (cellValue == null) return null;

    /*
     * First, look for an existing renderer instance for this particular bean
     */
    if (beanInstanceToCustomRenderer.containsKey(cellValue)) {
      return beanInstanceToCustomRenderer.get(cellValue);
    }

    /*
     * No existing renderer exists for this particular bean. Can we create a new renderer based on the bean's type?
     */
    Class beanType = cellValue.getClass();
    if (beanTypeToCustomRendererType.containsKey(beanType)) {
      Class<? extends CustomCellRenderer> rendererType = beanTypeToCustomRendererType.get(beanType);
      try {
        CustomCellRenderer renderer = rendererType.newInstance();
        renderer.setRendererProperties(rendererProperties);

        //Cache this renderer for reuse with this bean later.
        beanInstanceToCustomRenderer.put(cellValue, renderer);
        return renderer;
      } catch (Exception e) {
        throw new RuntimeException("Failed to instantiate a custom renderer.", e);
      }
    }

    /*
     * No custom renderer could be found for this bean/type
     */
    return null;
  }

  public void addCustomRendererForBeanType(Class<?> beanType, Class<? extends CustomCellRenderer> rendererType) {
    beanTypeToCustomRendererType.put(beanType, rendererType);
  }
}
