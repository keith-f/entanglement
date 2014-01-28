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

package com.entanglementgraph.graph.mongodb.jiti;

import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.EntityKeys;
import com.scalesinformatics.mongodb.dbobject.DbObjectMarshallerException;

import java.util.Set;

import static com.entanglementgraph.graph.mongodb.MongoUtils.parseKeyset;

/**
 * A convenience class for merging two BasicDBObject instances that represent graph nodes. This functionality is
 * required in several places so has been extracted from its original location within NodeModificationPlayer.
 *
 * Nodes may be merged according to a <code>MergePolicy</code>, which determines which data fields are ignored,
 * overwritten, or combined. Elements of the object's identifier (UIDs and names of EntityKeys) are always merged.
 *
 * @author Keith Flanagan
 */
public class EntityKeysMerger {

  public EntityKeysMerger() {
  }

  public EntityKeys merge(Set<EntityKeys> keysets) throws GraphModelException, DbObjectMarshallerException {
    EntityKeys existing = null;
    for (EntityKeys keyset : keysets) {
      if (existing == null) {
        existing = keyset;
      } else {
        existing = merge(existing, keyset);
      }
    }
    return existing;
  }

  public EntityKeys merge(EntityKeys existingKeyset, EntityKeys newKeyset)
      throws DbObjectMarshallerException, GraphModelException {

    //If entity types mismatch, then we have a problem.
    if (newKeyset.getType() != null && existingKeyset.getType() != null &&
        !newKeyset.getType().equals(existingKeyset.getType())) {
      throw new GraphModelException("Attempt to merge existing keyset with keyset from current request failed. " +
          "The type fields are non-null and mismatch. Existing keyset: "+existingKeyset
          +". Request keyset: "+newKeyset);
    }

    EntityKeys merged = new EntityKeys();
    // Set entity type. Here, existing 'type' gets priority, followed by new type (just to be sure...)
    if (existingKeyset.getType() != null) {
      merged.setType(existingKeyset.getType());
    } else if (newKeyset.getType() != null) {
      merged.setType(newKeyset.getType());
    }

    // Merge UIDs
    merged.addUids(existingKeyset.getUids());
    merged.addUids(newKeyset.getUids());

    // Merge names
    merged.addNames(existingKeyset.getNames());
    merged.addNames(newKeyset.getNames());

    return merged;
  }

}
