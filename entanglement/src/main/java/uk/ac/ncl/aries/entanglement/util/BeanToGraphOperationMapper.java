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

package uk.ac.ncl.aries.entanglement.util;

import com.torrenttamer.util.UidGenerator;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.ncl.aries.entanglement.revlog.commands.CreateNodeIfNotExistsByName;
import uk.ac.ncl.aries.entanglement.revlog.commands.GraphOperation;
import uk.ac.ncl.aries.entanglement.revlog.commands.SetNamedNodeProperty;

/**
 * A set of utilities for storing Java data beans as graph entities. The classes
 * contained here generate one or more graph revisions for Java for each bean
 * instance.
 * 
 * @author Keith Flanagan
 */
public class BeanToGraphOperationMapper
{
  private static final Logger logger =
          Logger.getLogger(BeanToGraphOperationMapper.class.getName());

  /**
   * Creates a new node (if one of that name doesn't already exist), and adds
   * a property to it. This is done as a single revision log item.
   * 
   * @param nodeName
   * @param nodeType
   * @param annName
   * @param annotation
   * @return
   * @throws BeanMapperException 
   */
//  public static CreateNodeWithPropertyIfNotExistsByName addNamedNodeWithAnnotationBean(
//          String nodeName, String nodeType, String annName, Object annotation)
//          throws BeanMapperException
//  {
//    String uniqueId = UidGenerator.generateUid();
////    String nodeType = bean.getClass().getName();
//    String dataSourceUid = null;
//    String evidenceTypeUid = null;
//    CreateNodeWithPropertyIfNotExistsByName createNodeWithProp = 
//            new CreateNodeWithPropertyIfNotExistsByName(nodeType, uniqueId, 
//            nodeName, dataSourceUid, evidenceTypeUid, annName, annotation.getClass().getName(), annotation);
//    return createNodeWithProp;
//  }
//  
  /**
   * Enables a graph node to be created, and a property added to that node
   * as two different graph operations.
   * 
   * @param nodeName
   * @param nodeType
   * @param annName
   * @param annotation
   * @return
   * @throws BeanMapperException 
   */
  public static List<GraphOperation> addBeanAsAnnotationToNamedNode(
          String nodeName, String nodeType, String annName, Object annotation)
          throws BeanMapperException
  {
    List<GraphOperation> ops = new LinkedList<>();
    
    String uniqueId = UidGenerator.generateUid();
//    String nodeType = bean.getClass().getName();
    String dataSourceUid = null;
    String evidenceTypeUid = null;
    CreateNodeIfNotExistsByName createNode = new CreateNodeIfNotExistsByName(
            nodeType, uniqueId, nodeName, dataSourceUid, evidenceTypeUid);
    ops.add(createNode);
    
    SetNamedNodeProperty setNodeProp = new SetNamedNodeProperty(
            nodeName, annName, annotation);
        ops.add(setNodeProp);
    
    return ops;
  }
  
  /**
   * Generates a number of GraphOperations, one for each property found on the
   * specified bean. 
   * 
   * @param beanName
   * @param bean
   * @return
   * @throws BeanMapperException 
   */
  public static List<GraphOperation> createCommandsForEachBeanProperty(
          String beanName, Object bean)
          throws BeanMapperException
  {
    List<GraphOperation> ops = new LinkedList<>();
    String uniqueId = UidGenerator.generateUid();
    String nodeType = bean.getClass().getName();
    String dataSourceUid = null;
    String evidenceTypeUid = null;
    CreateNodeIfNotExistsByName createNode = new CreateNodeIfNotExistsByName(
            nodeType, uniqueId, beanName, dataSourceUid, evidenceTypeUid);
    ops.add(createNode);
    
    Map<String, Object> beanProps = beanToPropertySet(bean);
    for (String propName : beanProps.keySet()) {
      Object propValue = beanProps.get(propName);
      if (propValue != null) {
        String propType = propValue.getClass().getName();
        SetNamedNodeProperty setNodeProp = new SetNamedNodeProperty(
                beanName, propName, propValue);
        ops.add(setNodeProp);
      }
    }
    
    return ops;
  }
  
  
  private static Map<String, Object> beanToPropertySet(Object bean) 
          throws BeanMapperException
  {
    try {
      Map<String, Object> propertyToValue = new HashMap<>();

      Class beanType = bean.getClass();
  //    System.out.println("Class: "+beanType.getName());
        BeanInfo info = Introspector.getBeanInfo(beanType);

      for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
        //Ignore certain common properties.
        switch (pd.getName()) {
          case "class" : 
            continue;
        }
        Method getter = pd.getReadMethod();
        Class returnType = getter.getReturnType();
        Object val = getter.invoke(bean);

        propertyToValue.put(pd.getName(), val);
//        AttributeName attType = 
//            MetaGraphUtils.loadAttributeType(config, pd.getName());
//        annotations.add(new PropertyAnnotation(attType.getId(), val));
      }

      return propertyToValue;
    }
    catch(Exception e) {
      throw new BeanMapperException("Failed to convert a data bean type: "
          + bean.getClass().getName() + " to a Map of property name -> value", e);
    }
  }
}
