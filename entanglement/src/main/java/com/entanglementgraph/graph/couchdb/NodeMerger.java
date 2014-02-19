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
import com.entanglementgraph.graph.Node;
import com.entanglementgraph.graph.commands.MergePolicy;
import com.entanglementgraph.graph.mongodb.player.LogPlayerException;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * A convenience class for merging two Node content beans based on a specified  <code>MergePolicy</code>,
 * which determines which data fields are ignored, overwritten, or combined.
 *
 * Elements of the object's identifier (UIDs and names of EntityKeys) are always merged.
 *
 * Note that the current implementation doesn't do anything clever with collections. They are treated in the
 * same way as primitive types.
 *
 * @author Keith Flanagan
 */
public class NodeMerger<C extends Content> {

  public NodeMerger() {
  }

  public void merge(MergePolicy mergePolicy, Node<C> existingNode, Node<C> newNode)
      throws GraphModelException {
    try {
      //Node<C> outputNode = new Node<>();
      //Node<C> outputNode = existingNode;
      EntityKeys<C> mergedKeys = mergeKeys(existingNode.getKeys(), newNode.getKeys());
      existingNode.setKeys(mergedKeys);

      switch(mergePolicy) {
        case NONE:
          //return existingNode;
          break;
        case ERR:
          throw new LogPlayerException("Attempt to merge nodes with one or more keyset items in common with" +
              "an existing node: "+existingNode.getKeys());
        case APPEND_NEW__LEAVE_EXISTING:
          doAppendNewLeaveExisting(existingNode, newNode);
//          return outputNode;
          break;
        case APPEND_NEW__OVERWRITE_EXSITING:
          doAppendNewOverwriteExisting(existingNode, newNode);
//          return outputNode;
          break;
        case OVERWRITE_ALL:
          doOverwriteAll(existingNode, newNode);
//          return outputNode;
          break;
        default:
          throw new LogPlayerException("Unsupported merge policy type: "+mergePolicy);
      }
    } catch (Exception e) {
      throw new GraphModelException("Failed to perform merge between nodes: "+existingNode+" and: "+newNode, e);
    }
  }



  /**
   * This method adds new properties to an existing node. Where there are
   * properties on the existing node with the same name as the ones specified
   * in the update command, we leave the existing values as they are.
   */
  private void doAppendNewLeaveExisting(
      Node<C> existingNode, Node<C> newNode)
      throws GraphModelException
  {
    try {
      if (existingNode.getContent() == null) {
        existingNode.setContent(newNode.getContent());
        return;
      }
      if (newNode.getContent() == null) {
        return;
      }

      BeanInfo info = Introspector.getBeanInfo(existingNode.getContent().getClass());
      // Loop over all properties on the existing content bean
      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
        Method getter = pd.getReadMethod();
        Method setter = pd.getWriteMethod();
        Object existingValue = getter.invoke(existingNode.getContent());
        Object newValue = getter.invoke(newNode.getContent());

        // If the value of the existing node is non-null, ignore.
        // Otherwise, set the value from the 'new' object
        if (existingValue == null) {
          setter.invoke(existingNode.getContent(), newValue);
        }
      }
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform 'append new, leave existing' operation on existing node: "+existingNode, e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties.
   */
  private void doAppendNewOverwriteExisting(Node<C> existingNode, Node<C> newNode)
      throws GraphModelException
  {
    try {
      if (existingNode.getContent() == null) {
        existingNode.setContent(newNode.getContent());
        return;
      }
      if (newNode.getContent() == null) {
        return;
      }

      BeanInfo info = Introspector.getBeanInfo(existingNode.getContent().getClass());
      // Loop over all properties on the new content bean
      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
        Method getter = pd.getReadMethod();
        Method setter = pd.getWriteMethod();
        Object existingValue = getter.invoke(existingNode.getContent());
        Object newValue = getter.invoke(newNode.getContent());

        // If the value of the new node is non-null, set it regardless of the existing value.
        if (newValue != null) {
          setter.invoke(existingNode.getContent(), newValue);
        }
      }
    }
    catch(Exception e) {
      throw new GraphModelException(
          "Failed to perform 'append new, overwrite existing' operation on existing node: "+existingNode, e);
    }
  }

  /**
   * This method adds new properties to an existing node and overwrites the
   * values of existing properties.
   * Immutable properties (UID, type and name) are, of course, ignored.
   */
  private void doOverwriteAll(Node<C> existingNode, Node<C> newNode)
      throws GraphModelException
  {
    try {
      existingNode.setContent(newNode.getContent());
//      if (existingNode.getContent() == null) {
//        existingNode.setContent(newNode.getContent());
//        return;
//      }
//      if (newNode.getContent() == null) {
//        return;
//      }
//
//      BeanInfo info = Introspector.getBeanInfo(existingNode.getContent().getClass());
//      // Loop over all properties on the new content bean
//      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
//        Method getter = pd.getReadMethod();
//        Method setter = pd.getWriteMethod();
//        Object existingValue = getter.invoke(existingNode.getContent());
//        Object newValue = getter.invoke(newNode.getContent());
//
//        // Overwrite everything
//        setter.invoke(existingNode.getContent(), newValue);
//      }
    }
    catch(Exception e) {
      throw new GraphModelException("Failed to perform 'overwrite' operation on existing node: "+existingNode, e);
    }
  }

  /**
   * Given two EntityKeys objects, returns a NEW EntityKeys instance containing the combined sets of UIDs and names.
   * An exception is thrown if the two provided objects have differing 'type' fields, or if the 'type' fields are NULL.
   *
   * @param existingKeys
   * @param newKeys
   * @return
   * @throws GraphModelException
   */
  private <C extends Content> EntityKeys<C> mergeKeys(EntityKeys<C> existingKeys, EntityKeys<C> newKeys)
      throws GraphModelException {

    if (existingKeys.getType() == null || newKeys.getType() == null) {
      throw new GraphModelException("Attempt to merge nodes with no 'type' field set.");
    }

    if (!existingKeys.getType().equals(newKeys.getType())) {
      throw new GraphModelException("Attempt to merge nodes with different types: "
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
