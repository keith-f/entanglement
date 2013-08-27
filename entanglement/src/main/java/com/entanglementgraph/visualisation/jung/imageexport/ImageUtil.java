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

package com.entanglementgraph.visualisation.jung.imageexport;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Keith Flanagan
 */
public class ImageUtil {
  private static final Logger logger = Logger.getLogger(ImageUtil.class.getName());

  public static void writePng(BufferedImage image, File outputFile) throws IOException {
    logger.info("Writing file: " + outputFile.getAbsolutePath());
    ImageIO.write(image, "png", outputFile);
  }

  public static void writeJpeg(BufferedImage image, File outputFile) throws IOException {
    logger.info("Writing file: " + outputFile.getAbsolutePath());
    ImageIO.write(image, "jpeg", outputFile);
  }

  public static void writeBmp(BufferedImage image, File outputFile) throws IOException {
    logger.info("Writing file: " + outputFile.getAbsolutePath());
    ImageIO.write(image, "bmp", outputFile);
  }
}
