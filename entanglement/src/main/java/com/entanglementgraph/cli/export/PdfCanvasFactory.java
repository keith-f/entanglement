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

package com.entanglementgraph.cli.export;

import com.itextpdf.text.pdf.PdfContentByte;
import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.util.mxCellRenderer;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 25/06/2013
 * Time: 13:57
 * To change this template use File | Settings | File Templates.
 */
public class PdfCanvasFactory extends mxCellRenderer.CanvasFactory {
  private final PdfContentByte cb;

  public PdfCanvasFactory(PdfContentByte cb) {
    this.cb = cb;
  }

  public mxICanvas createCanvas(int width, int height) {
    Graphics2D g2 = cb.createGraphics(width, height);
    return new mxGraphics2DCanvas(g2);
  }

}
