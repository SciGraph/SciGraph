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
package edu.sdsc.scigraph.neo4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/***
 * Convenience utilities for dealing with Neo4j graphs.
 * 
 * <b>Note:</b> Clients are responsible for managing transactions around these methods.
 */
public class GraphUtil {

  /***
   * Add a property value to a container.
   * 
   * Abstracts dealing with underlying value arrays for the client. The property may already have a
   * value. <br />
   * <b>Note:</b> duplicate values are ignored.
   * 
   * @param container
   *          The PropertyContainer in question
   * @param property
   *          The name of the property
   * @param value
   *          The value to append
   */
  public static void addProperty(PropertyContainer container, String property, Object value) {
    if (container.hasProperty(property)) {
      Object origValue = container.getProperty(property);
      Object newValue = getNewPropertyValue(origValue, value);
      container.setProperty(property, newValue);
    } else {
      container.setProperty(property, value);
    }
  }
  
  public static Object getNewPropertyValue(Object originalValue, Object newValue) {
    Class<?> clazz = checkNotNull(newValue).getClass();
    if (null != originalValue && originalValue.getClass().isArray()) {
      Object newArray = Array.newInstance(clazz, Array.getLength(originalValue) + 1);
      for (int i = 0; i < Array.getLength(originalValue); i++) {
        Array.set(newArray, i, Array.get(originalValue, i));
      }
      Array.set(newArray, Array.getLength(originalValue), newValue);
      return newArray;
    } else if (null != originalValue) {
      Object newArray = Array.newInstance(clazz, 2);
      Array.set(newArray, 0, originalValue);
      Array.set(newArray, 1, newValue);
      return newArray;
    } else {
      return newValue;
    }
  }

  /***
   * Get a single valued property in a type safe way.
   * 
   * @param container
   *          The PropertyContainer in question
   * @param property
   *          The name of the property
   * @param type
   *          The type of the property
   * @return An optional property value
   * @throws ClassCastException
   *           if the property types don't match
   */
  static public <T> Optional<T> getProperty(PropertyContainer container, String property,
      Class<T> type) {
    Optional<T> value = Optional.<T> absent();
    if (container.hasProperty(property)) {
      value = Optional.<T> of(type.cast(container.getProperty(property)));
    }
    return value;
  }

  /***
   * Get multi-valued properties.
   * 
   * @param container
   *          The PropertyContainer in question
   * @param property
   *          The name of the property
   * @param type
   *          The type of the property
   * @return An list of property values
   * @throws ClassCastException
   *           if the property types don't match
   */
  static public <T> List<T> getProperties(PropertyContainer container, String property,
      Class<T> type) {
    List<T> list = new ArrayList<>();
    if (container.hasProperty(property)) {
      return getPropertiesAsList(container.getProperty(property), type);
    }
    return list;
  }
  
  static public <T> List<T> getPropertiesAsList(Object value, Class<T> type) {
    List<T> list = new ArrayList<>();
    if (value.getClass().isArray()) {
      List<Object> objects = new ArrayList<>();
      for (int i = 0; i < Array.getLength(value); i++) {
        objects.add(Array.get(value, i));
      }
      for (Object o : objects) {
        list.add(type.cast(o));
      }
    } else {
      list.add(type.cast(value));
    }
    return list;
  }

  static public Iterable<Relationship> getRelationships(final Node a, final Node b,
      RelationshipType type, final boolean directed) {
    checkNotNull(a);
    checkNotNull(b);
    checkNotNull(type);

    return filter(a.getRelationships(type), new Predicate<Relationship>() {
      @Override
      public boolean apply(Relationship relationship) {
        return directed ? relationship.getEndNode().equals(b)
            || relationship.getStartNode().equals(b) : relationship.getEndNode().equals(b);
      }
    });
  }

  static public Iterable<Relationship> getRelationships(final Node a, final Node b,
      RelationshipType type) {
    return getRelationships(a, b, type, true);
  }

  static String getLastPathFragment(URI uri) {
    return uri.getPath().replaceFirst(".*/([^/?]+).*", "$1");
  }

  public static String getFragment(URI uri) {
    if (null != uri.getFragment()) {
      return uri.getFragment();
    } else if (uri.toString().startsWith("mailto:")) {
      return uri.toString().substring("mailto:".length());
    } else {
      return getLastPathFragment(uri);
    }
  }

}
