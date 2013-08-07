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
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshaller;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;

/**
 * @author Keith Flanagan
 */
public interface CustomVertexRenderer {
  /**
   * Sets the display that this renderer will provide visualisations for. This is usually set by the visualiser
   * instance itself and can be used for situations where you need to render objects differently depending on
   * circumstances such as user object selection, etc.
   *
   * @param visualiser
   */
  public void setVisualiser(Visualiser visualiser);

  public Transformer<DBObject, Icon> getVertexIconTransformer();
//  public Transformer<DBObject, Shape> getVertexShapeTransformer();
  public Transformer<DBObject, String> getVertexLabelTransformer();
  public Transformer<DBObject, String> getTooltipTransformer();

  public void setMarshaller(DbObjectMarshaller marshaller);
}
