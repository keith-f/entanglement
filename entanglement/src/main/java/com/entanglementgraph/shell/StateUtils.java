/*
 * Copyright 2012 Keith Flanagan
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

package com.entanglementgraph.shell;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Keith Flanagan
 */
public class StateUtils
{
  private static final Logger logger = 
          Logger.getLogger(StateUtils.class.getName());
  private static final String DEFAULT_PATH_PREFIX = ".entanglementgraph";
  private static final String DEFAULT_FILENAME = "entanglementgraph-settings.xml";
  
  private File stateFile;
  
  public StateUtils()
  {
    stateFile = new File(getDefaultFilename());
    logger.info("Default configuration file: "+stateFile);
  }
  
  public String getDefaultFilename()
  {
    Map<String, String> environment = System.getenv();
    StringBuilder filename = new StringBuilder();
    filename.append(DEFAULT_PATH_PREFIX);
    filename.append(File.separator);
    filename.append(DEFAULT_FILENAME);
    if (environment.containsKey("HOME")) {
      filename.insert(0, File.separator);
      filename.insert(0, environment.get("HOME"));
    }
    return filename.toString();
  }
  
  public ShellState loadStateIfExists() throws IOException
  {
    if (stateFile.exists()) {
      logger.info("Loading previous settings ...");
      ShellState existingState = loadState();
      
      //Even though we've loaded a previous state, ensure that all 'special' keys exist.
      Map<String, String> defaults = EntanglementStatePropertyNames.getDefaultPropertySettings();
      for (String defaultKey : defaults.keySet()) {
        if (!existingState.getProperties().containsKey(defaultKey)) {
          existingState.getProperties().put(defaultKey, defaults.get(defaultKey));
        }
      }
      return existingState;
    }
    ShellState newState = new ShellState(
            EntanglementStatePropertyNames.getDefaultPropertySettings());
    return newState;
  }
  
  
  public ShellState loadState() throws IOException
  {
    XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(stateFile)));
    ShellState state = (ShellState) d.readObject();
    d.close();
    return state;
  }
  
  public void saveState(ShellState state) throws IOException
  {
    if (!stateFile.getParentFile().exists()) {
      stateFile.getParentFile().mkdirs();
    }
    XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(stateFile)));
    e.writeObject(state);
    e.flush();
    e.close();
  }
}
