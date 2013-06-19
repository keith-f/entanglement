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

package com.entanglementgraph.util.gephi;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.OutputStream;
import org.gephi.io.exporter.spi.ByteExporter;
import org.gephi.io.exporter.spi.VectorExporter;
import org.gephi.preview.*;
import org.gephi.preview.PreviewControllerImpl;
import org.gephi.preview.api.PDFTarget;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.gephi.project.api.Workspace;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * This class is essentially a clone of <code>org.gephi.io.exporter.preview.PDFExporter</code> (version 0.8.2).
 * Static references to singleton objects have been replaced.
 *
 * See:
 * https://github.com/gephi/gephi/blob/0.8.2/modules/PreviewExport/src/main/java/org/gephi/io/exporter/preview/PDFExporter.java
 *
 * FIXME we will need to remove this file before release (Gephi seems to be GPL, and we want Apache)
 */
public class PDFExporter  implements ByteExporter, VectorExporter, LongTask {

  private ProgressTicket progress;
  private Workspace workspace;
  private OutputStream stream;
  private boolean cancel = false;
  private PDFTarget target;
  //Parameters
  private float marginTop = 18f;
  private float marginBottom = 18f;
  private float marginLeft = 18f;
  private float marginRight = 18f;
  private boolean landscape = false;
  private Rectangle pageSize = PageSize.A4;

  public boolean execute() {
    Progress.start(progress);

    // ****** Replaced original code (commented) ******
//    PreviewController controller = Lookup.getDefault().lookup(PreviewController.class);
    PreviewController controller = new PreviewControllerImpl();
    // ^^^^^^ Replaced original code (commented above) ^^^^^^
    controller.getModel(workspace).getProperties().putValue(PreviewProperty.VISIBILITY_RATIO, 1.0);
    controller.refreshPreview(workspace);
    PreviewProperties props = controller.getModel(workspace).getProperties();

    Rectangle size = new Rectangle(pageSize);
    if (landscape) {
      size = new Rectangle(pageSize.rotate());
    }
    Color col = props.getColorValue(PreviewProperty.BACKGROUND_COLOR);
    size.setBackgroundColor(new BaseColor(col.getRed(), col.getGreen(), col.getBlue()));

    Document document = new Document(size);
    PdfWriter pdfWriter = null;
    try {
      pdfWriter = PdfWriter.getInstance(document, stream);
      pdfWriter.setPdfVersion(PdfWriter.PDF_VERSION_1_5);
      pdfWriter.setFullCompression();

    } catch (DocumentException ex) {
      Exceptions.printStackTrace(ex);
    }
    document.open();
    PdfContentByte cb = pdfWriter.getDirectContent();
    cb.saveState();

    props.putValue(PDFTarget.LANDSCAPE, landscape);
    props.putValue(PDFTarget.PAGESIZE, size);
    props.putValue(PDFTarget.MARGIN_TOP, new Float((float) marginTop));
    props.putValue(PDFTarget.MARGIN_LEFT, new Float((float) marginLeft));
    props.putValue(PDFTarget.MARGIN_BOTTOM, new Float((float) marginBottom));
    props.putValue(PDFTarget.MARGIN_RIGHT, new Float((float) marginRight));
    props.putValue(PDFTarget.PDF_CONTENT_BYTE, cb);
    target = (PDFTarget) controller.getRenderTarget(RenderTarget.PDF_TARGET, workspace);
    if (target instanceof LongTask) {
      ((LongTask) target).setProgressTicket(progress);
    }

    try {
      controller.render(target, workspace);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    cb.restoreState();
    document.close();

    Progress.finish(progress);

    props.putValue(PDFTarget.PDF_CONTENT_BYTE, null);
    props.putValue(PDFTarget.PAGESIZE, null);

    return !cancel;
  }

  public boolean isLandscape() {
    return landscape;
  }

  public float getMarginBottom() {
    return marginBottom;
  }

  public float getMarginLeft() {
    return marginLeft;
  }

  public float getMarginRight() {
    return marginRight;
  }

  public float getMarginTop() {
    return marginTop;
  }

  public Rectangle getPageSize() {
    return pageSize;
  }

  public void setMarginBottom(float marginBottom) {
    this.marginBottom = marginBottom;
  }

  public void setMarginLeft(float marginLeft) {
    this.marginLeft = marginLeft;
  }

  public void setMarginRight(float marginRight) {
    this.marginRight = marginRight;
  }

  public void setMarginTop(float marginTop) {
    this.marginTop = marginTop;
  }

  public void setPageSize(Rectangle pageSize) {
    this.pageSize = pageSize;
  }

  public void setOutputStream(OutputStream stream) {
    this.stream = stream;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setLandscape(boolean landscape) {
    this.landscape = landscape;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public boolean cancel() {
    this.cancel = true;
    if (target instanceof LongTask) {
      ((LongTask) target).cancel();
    }
    return true;
  }

  public void setProgressTicket(ProgressTicket progressTicket) {
    this.progress = progressTicket;
  }
}
