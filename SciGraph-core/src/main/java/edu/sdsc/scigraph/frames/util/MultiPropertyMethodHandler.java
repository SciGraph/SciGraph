/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sdsc.scigraph.frames.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import com.tinkerpop.blueprints.Element;

import static edu.sdsc.scigraph.frames.util.ClassUtilities2.*;

import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.MethodHandler;

/***
 * Adds multi-valued property support to Frames (which is missing out of the box).
 * 
 * <p>You can install the handler like:
 * <pre>{@code
 *   FramedGraphFactory factory = new FramedGraphFactory(new AbstractModule() { 
 *     &#064;Override
 *     protected void doConfigure(FramedGraphConfiguration config) {
 *       config.addMethodHandler(new MultiPropertyMethodHandler());
 *     }
 *   });
 *   }</pre>
 * Or you can use {@link FramedMultivalueGraphFactory} to obtain an instance of a FramedGraph with support 
 * already installed.</p>
 * <p> Multi-valued support provides the following:
 * <ul>
 * <li><i>hasX</i> method will return true if the property has been set on the node</li>
 * <li><i>setX</i> methods with a primitive type argument will replace the current value with the primitive value</li>
 * <li><i>setX</i> methods with an {@link Iterable} type argument will replace the current value with the values of the Iterable</li>
 * <li><i>addX</i> methods with a primitive type argument append the primitive to the current value</li>
 * <li><i>addX</i> methods with an {@link Iterable} type argument will append the current value with the values of the Iterable</li>
 * <li><i>getX</i> methods with {@link Iterable} return types:
 *   <ul>
 *     <li>Return empty Iterables when no value is present</li>
 *     <li>Return single valued Iterables when a single value has been added</li>
 *     <li>Return multi-valued Iterables when multiple values have been added</li>
 *   </ul>
 * </li>
 * <li><i>getX</i> methods with primitive return types:
 *   <ul>
 *     <li>Return <i>null</i> when no value is present</li>
 *     <li>Return the primitive value</li>
 *     <li>Throw an {@link IllegalStateException} when multiple values are present</li>
 *   </ul>
 * </li>
 * </ul>
 * </p>
 * <p>Further:
 * <ul>
 * <li>This method handler supports any primitive type supported by the underlying graph</li>
 * <li>Properties are returned in the order they are added</li>
 * <li>Be sure that handler methods for each property are using the same primitive type:
 *   <ul>
 *     <li>Incompatible types will fail fast when getting a single primitive type</li>
 *     <li>Incompatible types will fail later - after the client references elements of the collection (due to type erasure)</li>
 *   </ul>
 * </li> 
 * </ul>
 * </p>
 */
@NotThreadSafe
public class MultiPropertyMethodHandler implements MethodHandler<Property> {

  @Override
  public Class<Property> getAnnotationType() {
    return Property.class;
  }

  @Override
  public Object processElement(Object frame, Method method, Object[] arguments,
      Property annotation, FramedGraph<?> framedGraph, Element node) {
    String property = annotation.value();

    if (isGetMethod(method)) {
      return processGet(node, property, method);
    } else if (isAddMethod(method)) {
      processAdd(node, property, arguments[0], method);
    } else if (isSetMethod(method)){
      node.removeProperty(property);
      processAdd(node, property, arguments[0], method);
    } else if (isRemoveMethod(method)) {
      node.removeProperty(property);
    } else if (isHasMethod(method)) {
      return (null != node.getProperty(property));
    }
    return null;
  }

  public void processAdd(Element node, String property, Object value, Method method) {
    checkNotNull(value, "Property values should not be null. Consider using a removeX method instead.");
    if (null != node.getProperty(property)) {
      Object orig = (Object)node.getProperty(property);
      if (isIterable(node.getProperty(property))) {
        List<Object> origValue = node.getProperty(property);
        origValue.add(value);
      } else {
        Set<Object> list = new LinkedHashSet<>();
        list.add(orig);
        list.add(value);
        node.setProperty(property, list);
      }
    } else {
      if (isIterable(value)) {
        for (Object o: (Iterable<?>)value) {
          processAdd(node, property, o, method);
        }
      } else {
        node.setProperty(property, value);
      }
    }
  }

  public Object processGet(Element node, String property, Method method) {
    if (returnsIterable(method)) {
      if (null == node.getProperty(property)) {
        return new ArrayList<>();
      } else if (isIterable(node.getProperty(property))) {
        return (List<?>)node.getProperty(property);
      } else {
        return newArrayList(node.getProperty(property));
      }
    } else {
      if (node.getProperty(property) instanceof Iterable) {
        throw new IllegalStateException("Can't call " + method.getName() + " when its property is multivalued");
      } else {
        return node.getProperty(property);
      }
    }
  }

}
