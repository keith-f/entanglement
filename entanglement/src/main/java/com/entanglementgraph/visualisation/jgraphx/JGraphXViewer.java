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

import com.mxgraph.io.mxCodec;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import org.w3c.dom.Document;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 25/06/2013
 * Time: 11:17
 * To change this template use File | Settings | File Templates.
 */
public class JGraphXViewer extends JFrame {

  private final mxGraph graph;

  public JGraphXViewer()
  {
    super("Graph viewer");

    graph = new mxGraph();
    Object parent = graph.getDefaultParent();



    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    getContentPane().add(graphComponent);
  }

  private void loadFile(File file) throws IOException {
    Document document = mxXmlUtils.parseXml(
        mxUtils.readFile(file.getAbsolutePath()));

    mxCodec codec = new mxCodec(document);
    codec.decode(document.getDocumentElement(), graph.getModel());
  }

  public static void main(String[] args) throws IOException {
    JGraphXViewer frame = new JGraphXViewer();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(400, 320);
    frame.setVisible(true);

    File file = new File(args[0]);

    //fc.getSelectedFile().getAbsolutePath()
    frame.loadFile(file);
  }
}
