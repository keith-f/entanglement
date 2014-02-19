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

package com.entanglementgraph.graph.couchdb;

import com.entanglementgraph.graph.Content;
import com.entanglementgraph.graph.EntityKeys;
import com.entanglementgraph.graph.GraphModelException;
import com.entanglementgraph.graph.Edge;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.mongodb.player.LogPlayerException;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * A convenience class for merging two Edge content beans based on a specified  <code>MergePolicy</code>,
 * which determines which data fields are ignored, overwritten, or combined.
 *
 * Elements of the object's identifier (UIDs and names of EntityKeys) are always merged.
 *
 * Note that the current implementation doesn't do anything clever with collections. They are treated in the
 * same way as primitive types.
 *
 * @author Keith Flanagan
 */
public class EdgeMerger<C extends Content, T extends Content, F extends Content> {

  public EdgeMerger() {
  }

  public void merge(MergePolicy mergePolicy, Edge<C, T, F> existingEdge, Edge<C, T, F> newEdge)
      throws GraphModelException {
    try {
      //Edge<C, T, F> outputEdge = new Edge<>();

      existingEdge.setKeys(mergeKeys(existingEdge.getKeys(), newEdge.getKeys()));
      existingEdge.setFrom(mergeKeys(existingEdge.getFrom(), newEdge.getFrom()));
      existingEdge.setTo(mergeKeys(existingEdge.getTo(), newEdge.getTo()));

      switch(mergePolicy) {
        case NONE:
          break;
        case ERR:
          throw new LogPlayerException("Attempt to merge nodes with one or more keyset items in common with" +
              "an existing node: "+existingEdge.getKeys());
        case APPEND_NEW__LEAVE_EXISTING:
          doAppendNewLeaveExisting(existingEdge, newEdge);
          break;
        case APPEND_NEW__OVERWRITE_EXSITING:
          doAppendNewOverwriteExisting(existingEdge, newEdge);
          break;
        case OVERWRITE_ALL:
          doOverwriteAll(existingEdge, newEdge);
          break;
        default:
          throw new LogPlayerException("Unsupported merge policy type: "+mergePolicy);
      }
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform merge between nodes: "+existingEdge.getKeys()+" and: "+newEdge.getKeys(), e);
    }
  }



  /**
   * This method adds new properties to an existing node. Where there are
   * properties on the existing node with the same name as the ones specified
   * in the update command, we leave the existing values as they are.
   */
  private void doAppendNewLeaveExisting(Edge<C, T, F> existingEdge, Edge<C, T, F> newEdge)
      throws GraphModelException
  {
    try {
      if (existingEdge.getContent() == null) {
        existingEdge.setContent(newEdge.getContent());
        return;
      }
      if (newEdge.getContent() == null) {
        return;
      }

      BeanInfo info = Introspector.getBeanInfo(existingEdge.getContent().getClass());
      // Loop over all properties on the existing content bean
      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
        Method getter = pd.getReadMethod();
        Method setter = pd.getWriteMethod();
        Object existingValue = getter.invoke(existingEdge.getContent());
        Object newValue = getter.invoke(newEdge.getContent());

        // If the value of the existing edge is non-null, ignore.
        // Otherwise, set the value from the 'new' object
        if (existingValue == null) {
          setter.invoke(existingEdge.getContent(), newValue);
        }
      }
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform 'append new, leave existing' operation on existing node: "+existingEdge.getKeys(), e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties.
   */
  private void doAppendNewOverwriteExisting(Edge<C, T, F> existingEdge, Edge<C, T, F> newEdge)
      throws GraphModelException
  {
    try {
      if (existingEdge.getContent() == null) {
        existingEdge.setContent(newEdge.getContent());
        return;
      }
      if (newEdge.getContent() == null) {
        return;
      }

      BeanInfo info = Introspector.getBeanInfo(existingEdge.getContent().getClass());
      // Loop over all properties on the new content bean
      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
        Method getter = pd.getReadMethod();
        Method setter = pd.getWriteMethod();
        Object existingValue = getter.invoke(existingEdge.getContent());
        Object newValue = getter.invoke(newEdge.getContent());

        // If the value of the new edge is non-null, set it regardless of the existing value.
        if (newValue != null) {
          setter.invoke(existingEdge.getContent(), newValue);
        }
      }
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform 'append new, overwrite existing' operation on existing node: "+existingEdge.getKeys(), e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doOverwriteAll(Edge<C, T, F> existingEdge, Edge<C, T, F> newEdge)
      throws GraphModelException
  {
    try {
      existingEdge.setContent(newEdge.getContent());
//      BeanInfo info = Introspector.getBeanInfo(outputEdge.getContent().getClass());
//      // Loop over all properties on the new content bean
//      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
//        Method getter = pd.getReadMethod();
//        Method setter = pd.getWriteMethod();
//        Object value = getter.invoke(newEdge);
//
//        // If the value of the new node is non-null, copy the new value to the target object
//        if (value != null) {
//          setter.invoke(outputEdge, getter.invoke(newEdge));
//        }
//        // Otherwise, there's nothing to do. Leave it NULL.
//      }
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'overwrite' operation on existing edge: "+existingEdge.getKeys(), e);
    }
  }

  /**
   * Given two EntityKeys objects, returns a NEW EntityKeys instance containing the combined sets of UIDs and names.
   * An exception is thrown if the two provided objects have differing 'type' fields, or if the 'type' fields are NULL.
   *
   * @param existingKeys
   * @param newKeys
   * @return
   * @throws com.entanglementgraph.graph.GraphModelException
   */
  private <C extends Content> EntityKeys<C> mergeKeys(EntityKeys<C> existingKeys, EntityKeys<C> newKeys)
      throws GraphModelException {

    if (existingKeys.getType() == null || newKeys.getType() == null) {
      throw new GraphModelException("Attempt to merge edges with no 'type' field set.");
    }

    if (!existingKeys.getType().equals(newKeys.getType())) {
      throw new GraphModelException("Attempt to merge edges with different types: "
          + existingKeys.getType() + " != " + newKeys.getType());
    }

    EntityKeys merged = new EntityKeys();
    merged.setType(existingKeys.getType());

    merged.addUids(existingKeys.getUids());
    merged.addUids(newKeys.getUids());
    merged.addNames(existingKeys.getNames());
    merged.addNames(newKeys.getNames());

    return merged;
  }

}
