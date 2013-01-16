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
 * File created: 27-Nov-2012, 17:07:37
 */

package uk.ac.ncl.aries.entanglement.shell.gdfexport;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import uk.ac.ncl.aries.entanglement.graph.data.Edge;
import uk.ac.ncl.aries.entanglement.graph.data.Node;

/**
 *
 * @author Keith Flanagan
 */
public class GdfWriter
    extends BufferedWriter
{
  private final BufferedWriter delegate;
   
  public GdfWriter(BufferedWriter delegate)
  {
    super(delegate);
    this.delegate = delegate;
  }

  public void writeNodeDef() throws IOException
  {
    StringBuilder sb = new StringBuilder();
    sb.append("nodedef>");
    sb.append("name VARCHAR,");
    sb.append("type VARCHAR,");
//    sb.append("incoming_edges INTEGER,");
    sb.append("color VARCHAR");
    
    delegate.write(sb.toString());
    delegate.newLine();
  }
  
  public void writeNode(Node node, Color c) throws IOException
  {
    StringBuilder sb = new StringBuilder();
    sb.append(node.getUid());
    sb.append(",").append(node.getType());
//    sb.append(",").append(node.getIncomingEdgeIds().size());
    
    //Color
    sb.append(",'").append(c.getRed()).append(",").append(c.getGreen()).append(",").append(c.getBlue()).append("'");
    
    delegate.write(sb.toString());
    delegate.newLine();
  }
  
  public void writeEdgeDef() throws IOException
  {
    StringBuilder sb = new StringBuilder();
    sb.append("edgedef>");
    sb.append("node1 VARCHAR,");
    sb.append("node2 VARCHAR,");
    sb.append("edge_guid VARCHAR,");
    sb.append("type VARCHAR");
    
    delegate.write(sb.toString());
    delegate.newLine();
  }
  
  public void writeEdge(Edge edge) throws IOException
  {
    StringBuilder sb = new StringBuilder();
    sb.append(edge.getFromUid());
    sb.append(",").append(edge.getToUid());
    sb.append(",").append(edge.getUid());
    sb.append(",").append(edge.getType());
    
    delegate.write(sb.toString());
    delegate.newLine();
  }

  @Override
  public void write(char[] chars, int i, int i1)
      throws IOException
  {
    delegate.write(chars, i, i1);
  }

  @Override
  public void flush()
      throws IOException
  {
    delegate.flush();
  }

  @Override
  public void close()
      throws IOException
  {
    delegate.close();
  }

}
