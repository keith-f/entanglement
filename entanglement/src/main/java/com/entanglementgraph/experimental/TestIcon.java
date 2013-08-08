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
package com.entanglementgraph.experimental;

import edu.uci.ics.jung.visualization.VisualizationViewer;

import javax.swing.*;
import java.awt.*;

/**
 * User: keith
 * Date: 01/08/13; 16:38
 *
 * @author Keith Flanagan
 */
public class TestIcon<V, E> implements Icon {
  private final VisualizationViewer<V, E> vv;
  private final V v;

  public TestIcon(VisualizationViewer<V, E> vv, V v) {
    this.vv = vv;
    this.v = v;
  }

  public int getIconHeight() {
    return 20;
  }

  public int getIconWidth() {
    return 20;
  }

  public void paintIcon(Component c, Graphics g,
                        int x, int y) {
    if (vv.getPickedVertexState().isPicked(v)) {
      g.setColor(Color.yellow);
    } else {
      g.setColor(Color.red);
    }
    g.fillOval(x, y, 20, 20);
    if (vv.getPickedVertexState().isPicked(v)) {
      g.setColor(Color.black);
    } else {
      g.setColor(Color.white);
    }
    g.drawString("" + v, x + 6, y + 15);

  }
}
