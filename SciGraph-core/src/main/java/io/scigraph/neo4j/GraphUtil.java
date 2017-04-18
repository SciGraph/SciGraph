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
package io.scigraph.neo4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;

/***
 * Convenience utilities for dealing with Neo4j graphs.
 * 
 * <b>NOTE:</b> Clients are responsible for managing transactions around these methods.
 */
public class GraphUtil {

  /***
   * Add a property value to a container.
   * 
   * Abstracts dealing with underlying value arrays. The property may already have a value.
   * 
   * @param container the {@link PropertyContainer} in question
   * @param property the name of the property to append
   * @param value the value to append
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

  static Object getNewPropertyValue(Object originalValue, Object newValue) {
    Class<?> clazz = checkNotNull(newValue).getClass();
    boolean reduceToString = false;
    if (null != originalValue && originalValue.getClass().isArray()) {
      Class<?> originalClazz = Array.get(originalValue, 0).getClass();
      if (!originalClazz.equals(clazz)) {
        reduceToString = true;
        clazz = String.class;
      }
      newValue = reduceToString ? newValue.toString() : newValue;
      Object newArray = Array.newInstance(clazz, Array.getLength(originalValue) + 1);
      for (int i = 0; i < Array.getLength(originalValue); i++) {
        Object val = Array.get(originalValue, i);
        if (newValue.equals(val)) {
          return originalValue;
        }
        Array.set(newArray, i, reduceToString ? val.toString() : val);
      }
      Array.set(newArray, Array.getLength(originalValue), newValue);
      return newArray;
    } else if (null != originalValue) {
      if (!clazz.equals(originalValue.getClass())) {
        reduceToString = true;
        clazz = String.class;
      }
      originalValue = reduceToString ? originalValue.toString() : originalValue;
      newValue = reduceToString ? newValue.toString() : newValue;
      if (!originalValue.equals(newValue)) {
        Object newArray = Array.newInstance(clazz, 2);
        Array.set(newArray, 0, originalValue);
        Array.set(newArray, 1, newValue);
        return newArray;
      } else {
        return originalValue;
      }
    } else {
      return newValue;
    }
  }

  /***
   * Get a single valued property in a type-safe way.
   * 
   * @param container the {@link PropertyContainer} in question
   * @param property the name of the property
   * @param type the expected type of the property
   * @return an {@link Optional} property value
   * @throws ClassCastException if {@code type} does not match the actual type in the graph
   */
  static public <T> Optional<T> getProperty(PropertyContainer container, String property,
      Class<T> type) {
    Optional<T> value = Optional.<T>empty();
    if (container.hasProperty(property)) {
      value = Optional.<T> of(type.cast(container.getProperty(property)));
    }
    return value;
  }

  /***
   * Get multi-valued properties.
   * 
   * @param container the {@link PropertyContainer} in question
   * @param property the name of the property
   * @param type the expected type of the property
   * @return collection of property values (empty if the property does not exist). 
   * @throws ClassCastException if the {@code type} does not match the actual type in the graph
   */
  static public <T> Collection<T> getProperties(PropertyContainer container, String property,
      Class<T> type) {
    List<T> list = new ArrayList<>();
    if (container.hasProperty(property)) {
      return getPropertiesAsSet(container.getProperty(property), type);
    }
    return list;
  }

  static <T> Set<T> getPropertiesAsSet(Object value, Class<T> type) {
    Set<T> set = new HashSet<>();
    if (value.getClass().isArray()) {
      List<Object> objects = new ArrayList<>();
      for (int i = 0; i < Array.getLength(value); i++) {
        objects.add(Array.get(value, i));
      }
      for (Object o : objects) {
        set.add(type.cast(o));
      }
    } else {
      set.add(type.cast(value));
    }
    return set;
  }

  public static Iterable<Relationship> getRelationships(final Node a, final Node b,
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

  public static Iterable<Relationship> getRelationships(final Node a, final Node b,
      RelationshipType type) {
    return getRelationships(a, b, type, true);
  }

  /***
   * TODO: This and every spot that uses it is a bit of a hack
   * This should ideally be handled by the index.
   * @param value
   * @return
   */
  public static boolean ignoreProperty(Object value) {
    if (value instanceof String
        && (CharMatcher.WHITESPACE.matchesAllOf((String) value)
            || StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(((String) value).toLowerCase()))) {
      return true;
    } 
    return false;
  }

}
