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

import com.mongodb.DBObject;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Keith Flanagan
 */
public class JungGraphFrame {
  private final JFrame frame;
  private final VisualizationViewer<DBObject, DBObject> visualiser;

  public JungGraphFrame(final VisualizationViewer<DBObject, DBObject> visualiser) {
    this.visualiser = visualiser;
    frame = new JFrame();
    Container content = frame.getContentPane();
    final GraphZoomScrollPane panel = new GraphZoomScrollPane(visualiser);
    content.add(panel);

    final ModalGraphMouse gm = new DefaultModalGraphMouse<Integer,Number>();
    visualiser.setGraphMouse(gm);

    final ScalingControl scaler = new CrossoverScalingControl();

    JButton plus = new JButton("+");
    plus.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scaler.scale(visualiser, 1.1f, visualiser.getCenter());
      }
    });
    JButton minus = new JButton("-");
    minus.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scaler.scale(visualiser, 1/1.1f, visualiser.getCenter());
      }
    });

    JPanel controls = new JPanel();
    controls.add(plus);
    controls.add(minus);
    controls.add(((DefaultModalGraphMouse<Integer,Number>) gm).getModeComboBox());
    content.add(controls, BorderLayout.SOUTH);

    frame.pack();
    frame.setVisible(true);
  }

  public JFrame getFrame() {
    return frame;
  }

  public VisualizationViewer<DBObject, DBObject> getVisualiser() {
    return visualiser;
  }
}
