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

import com.entanglementgraph.irc.commands.cursor.IrcEntanglementFormat;
import com.entanglementgraph.visualisation.jung.imageexport.ImageUtil;
import com.entanglementgraph.visualisation.jung.imageexport.JungToBufferedImage;
import com.entanglementgraph.visualisation.jung.MongoToJungGraphExporter;
import com.entanglementgraph.visualisation.jung.imageexport.OutputFileUtil;
import com.mongodb.DBObject;
import com.scalesinformatics.uibot.BotLogger;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.util.Animator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Keith Flanagan
 */
public class JungGraphFrame {
  private final JFrame frame;
  private final BotLogger logger;
  private final IrcEntanglementFormat entFormat = new IrcEntanglementFormat();
  private final TrackingVisualisation trackingVis;
  private final VisualizationViewer<DBObject, DBObject> visualiser;

  public JungGraphFrame(final BotLogger logger, final TrackingVisualisation trackingVis,
                        final VisualizationViewer<DBObject, DBObject> visualiser) {
    this.logger = logger;
    this.trackingVis = trackingVis;
    this.visualiser = visualiser;
    frame = new JFrame();
    Container content = frame.getContentPane();
    final GraphZoomScrollPane panel = new GraphZoomScrollPane(visualiser);
    content.add(panel);

    final ModalGraphMouse gm = new DefaultModalGraphMouse<Integer,Number>();
    visualiser.setGraphMouse(gm);

    final ScalingControl scaler = new CrossoverScalingControl();

    JButton animationStart = new JButton("Start animator");
    animationStart.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Animator animator = trackingVis.getAnimator();
        if (animator != null) {
          animator.start();
        }
      }
    });
    JButton animationStop = new JButton("Stop animator");
    animationStop.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Animator animator = trackingVis.getAnimator();
        if (animator != null) {
          animator.stop();
        }
      }
    });
    final JTextField animationTimeField = new JTextField("15");
    animationTimeField.setToolTipText("Image export layout animation time.");
    JButton saveToGraphMl = new JButton("GraphML");
    saveToGraphMl.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        MongoToJungGraphExporter exporter = new MongoToJungGraphExporter(visualiser.getGraphLayout().getGraph());
        try {
          exporter.writeToGraphMLFile(new File("graph.xml"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    JButton saveToSvg = new JButton("SVG");
    saveToSvg.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        MongoToJungGraphExporter exporter = new MongoToJungGraphExporter(visualiser.getGraphLayout().getGraph());
        try {
          exporter.writeToSvgFile(new File("graph.svg"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });
    JButton saveToPng = new JButton("PNG");
    saveToPng.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          int width = (int) visualiser.getGraphLayout().getSize().getWidth();
          int height = (int) visualiser.getGraphLayout().getSize().getHeight();
          long animationSeconds = Long.parseLong(animationTimeField.getText());
          JungToBufferedImage graphToImage = new JungToBufferedImage(logger,
              width, height, animationSeconds,
              trackingVis.getCustomVertexRenderer(), visualiser.getGraphLayout().getGraph());
          logger.println("Exporting image...");
          BufferedImage image = graphToImage.createImage();
//          trackingVis.getCustomVertexRenderer().getDisplayNameFactories().createNameForEntity()
          String fileBaseName = "image-export-"+System.currentTimeMillis();
          File file = OutputFileUtil.createFile(new File("."), fileBaseName, ".png", width, height, animationSeconds);
          ImageUtil.writePng(image, file);
          logger.infoln("Written: %s", entFormat.format(file).toString());
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    });

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
    controls.add(animationStart);
    controls.add(animationStop);
    controls.add(saveToGraphMl);
    controls.add(saveToSvg);
    controls.add(animationTimeField);
    controls.add(saveToPng);
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
