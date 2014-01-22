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

import java.io.File;

/**
 * @author Keith Flanagan
 */
public class OutputFileUtil {

  public static File createFile(File directory, String baseName, String extension, int xDim, int yDim, long animationSeconds) {
    File outputFile = new File(directory, baseName+"-"+xDim+"x"+yDim+"-"+animationSeconds+"s"+extension);
    return outputFile;
  }

  /**
   * Convenience function to remove characters from a string that are not usually permitted in filenames
   * @param rawString
   * @return
   */
  public static String prepareStringForUseAsFilename(String rawString) {
    return rawString.replace(' ', '_')
        .replace(':', '_')
        .replace('`', '_')
        .replace('"', '_')
        .replace('\"', '_');
  }
}