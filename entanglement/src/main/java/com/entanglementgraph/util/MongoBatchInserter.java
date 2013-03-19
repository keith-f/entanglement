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

package com.entanglementgraph.util;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.data.EntityKeys;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: keith
 * Date: 19/03/2013
 * Time: 08:36
 * To change this template use File | Settings | File Templates.
 */
public class MongoBatchInserter
    implements KeysetListener {

  private static final Logger logger = Logger.getLogger(MongoBatchInserter.class.getName());
  private static final int DEFAULT_BATCH_SIZE = 5000;

  private static class TypeNamePair {
    String type;
    String name;

    private TypeNamePair(String type, String name) {
      this.type = type;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TypeNamePair that = (TypeNamePair) o;

      if (name != null ? !name.equals(that.name) : that.name != null) return false;
      if (type != null ? !type.equals(that.type) : that.type != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = type != null ? type.hashCode() : 0;
      result = 31 * result + (name != null ? name.hashCode() : 0);
      return result;
    }
  }

  private Set<String> uidsInUse;
  private Set<TypeNamePair> namesInUse;

  private final DBCollection col;
  private final List<DBObject> batch;

  private int batchSize;

  public MongoBatchInserter(DBCollection col) {
    this.col = col;
    this.batch = new LinkedList<>();
    uidsInUse = new HashSet<>();
    namesInUse = new HashSet<>();
    this.batchSize = DEFAULT_BATCH_SIZE;
  }

  public void notifyOperationInvolvingKeyset(EntityKeys keyset) {

  }

  public void addItemToBatch(EntityKeys keyset, DBObject obj) throws GraphModelException {
    batch.add(obj);
    addKeys(keyset);
    if (batch.size() > batchSize) {
      //flushNodes();
      flush();
    }
  }

  private void addKeys(EntityKeys keyset) {
    uidsInUse.addAll(keyset.getUids());
    if (!keyset.getNames().isEmpty()) {
      for (String name : keyset.getNames()) {
        namesInUse.add(new TypeNamePair(keyset.getType(), name));
      }
    }
  }

  public void flush() throws GraphModelException {
    logger.info("\n\n**************************\n" +
        "Flushing batch of "+batch.size()+" to "+col.getName()+
        "\n**********************\n");
    col.insert(batch);
    batch.clear();
    uidsInUse.clear();
    namesInUse.clear();
  }
}
