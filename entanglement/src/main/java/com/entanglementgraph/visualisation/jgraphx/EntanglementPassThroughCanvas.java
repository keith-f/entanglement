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

import com.entanglementgraph.visualisation.jgraphx.renderers.CustomCellRenderer;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.view.mxCellState;

import javax.swing.*;
import javax.swing.border.BevelBorder;

/**
 * A custom <code>mxInteractiveCanvas</code> used for drawing custom JGraphX images.
 * Note that there is only ONE instance of this class per <code>GraphComponent</code>. This is awkward, so we have
 * a method here that takes an implementation of <code>CustomCellRenderer</code> in order to actually perform the
 * rendering. All rendering operations are simply delegated onot the particular custom cell renderer that the caller
 * provides.
 *
 * @author Keith Flanagan
 */
public class EntanglementPassThroughCanvas extends mxInteractiveCanvas {

//  protected final CellRendererPane rendererPane = new CellRendererPane();

  protected final mxGraphComponent graphComponent;

  public EntanglementPassThroughCanvas(mxGraphComponent graphComponent)
  {
    this.graphComponent = graphComponent;
  }

  public <T> void drawVertex(mxCellState state, T dataBean, CustomCellRenderer<T> renderer)
  {
    renderer.renderCellContentToCanvas(state, dataBean, graphComponent, this);

//    JLabel backgroundLabel = new JLabel(labelText);
//    backgroundLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
//    backgroundLabel.setHorizontalAlignment(JLabel.CENTER);
//    backgroundLabel.setVerticalAlignment(JLabel.TOP);
//    backgroundLabel.setBackground(graphComponent.getBackground().darker());
//    backgroundLabel.setOpaque(true);
//
////    System.out.println("StateX: "+state.getX()+", StateY: "+state.getY()+"; translation: "+translate.getX()+", "+translate.getY()
////        + "; dimensions: "+state.getWidth()+", "+state.getHeight());
//
////    System.out.println("Components: "+centralContent.getComponents().length);
//
//    rendererPane.paintComponent(g, backgroundLabel, graphComponent,
//        (int) state.getX() + translate.x, (int) state.getY() + translate.y,
//        (int) state.getWidth(), (int) state.getHeight(), true);
//
//    rendererPane.paintComponent(g, centralContent, graphComponent,
//        (int) state.getX() + translate.x, (int) (state.getY() + translate.y + backgroundLabel.getPreferredSize().getHeight()),
//        (int) state.getWidth(), (int) (state.getHeight() - backgroundLabel.getPreferredSize().getHeight()), true);
  }

//  public CellRendererPane getRendererPane() {
//    return rendererPane;
//  }
}
