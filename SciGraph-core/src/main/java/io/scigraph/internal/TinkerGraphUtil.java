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
package io.scigraph.internal;

import static com.google.common.collect.Sets.newHashSet;
import io.scigraph.frames.CommonProperties;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;

import scala.collection.convert.Wrappers.SeqWrapper;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/***
 * Utilities for building TinkerGraphs from Neo4j objects
 */
public final class TinkerGraphUtil {

  static final Logger logger = Logger.getLogger(TinkerGraphUtil.class.getName());

  static final ImmutableSet<String> PROTECTED_PROPERTY_KEYS = ImmutableSet.of(CommonProperties.IRI, CommonProperties.CURIE);

  static void copyProperties(PropertyContainer container, Element element) {
    for (String key: container.getPropertyKeys()) {
      Object property = container.getProperty(key);
      if (property.getClass().isArray()) {
        List<Object> propertyList = new ArrayList<>();
        for (int i = 0; i < Array.getLength(property); i++) {
          propertyList.add(Array.get(property, i));
        }
        property = propertyList;
      }
      element.setProperty(key, property);
    }
  }

  static Vertex addNode(Graph graph, Node node) {
    Vertex vertex = graph.getVertex(node.getId());
    if (null == vertex) {
      vertex = graph.addVertex(node.getId());
      copyProperties(node, vertex);
      Set<String> labels = new HashSet<>();
      for (Label label: node.getLabels()) {
        labels.add(label.name());
      }
      vertex.setProperty("types", labels);
    }
    return vertex;
  }

  static Edge addEdge(Graph graph, Relationship relationship) {
    Edge edge = graph.getEdge(relationship.getId());
    if (null == edge) {
      Vertex outVertex = addNode(graph, relationship.getStartNode());
      Vertex inVertex = addNode(graph, relationship.getEndNode());
      String label = relationship.getType().name();
      // TODO #152 add CurieUtil to resolve IRI to Curie
      edge = graph.addEdge(relationship.getId(), outVertex, inVertex, label);
      copyProperties(relationship, edge);
    }
    return edge;
  }

  public static Element addElement(Graph graph, PropertyContainer container) {
    if (container instanceof Node) {
      return addNode(graph, (Node) container);
    } else {
      return addEdge(graph, (Relationship) container);
    }
  }

  public static void addPath(Graph graph, Iterable<PropertyContainer> path) {
    for (PropertyContainer container: path) {
      addElement(graph, container);
    }
  }

  static void copyProperties(Element source, Element target) {
    for (String key: source.getPropertyKeys()) {
      Object property = source.getProperty(key);
      if (property.getClass().isArray()) {
        List<Object> propertyList = new ArrayList<>();
        for (int i = 0; i < Array.getLength(property); i++) {
          propertyList.add(Array.get(property, i));
        }
        property = propertyList;
      }
      target.setProperty(key, property);
    }
  }

  static Vertex addNode(Graph graph, Vertex node) {
    Vertex vertex = graph.getVertex(node.getId());
    if (null == vertex) {
      vertex = graph.addVertex(node.getId());
      copyProperties(node, vertex);
    }
    return vertex;
  }

  static Edge addEdge(Graph graph, Edge edge) {
    Edge newEdge = graph.getEdge(edge.getId());
    if (null == newEdge) {
      Vertex outVertex = addNode(graph, edge.getVertex(Direction.OUT));
      Vertex inVertex = addNode(graph, edge.getVertex(Direction.IN));
      String label = edge.getLabel();
      newEdge = graph.addEdge(edge.getId(), outVertex, inVertex, label);
      copyProperties(edge, edge);
    }
    return newEdge;
  }


  public static Element addElement(Graph graph, Element element) {
    if (element instanceof Vertex) {
      return addNode(graph, (Vertex) element);
    } else {
      return addEdge(graph, (Edge) element);
    }
  }

  static void addGraph(Graph graph, Graph addition) {
    for (Vertex vertex: addition.getVertices()) {
      addElement(graph, vertex);
    }
    for (Edge edge: addition.getEdges()) {
      addElement(graph, edge);
    }
  }

  public static Graph combineGraphs(Graph graph1, Graph graph2) {
    Graph graph = new TinkerGraph();
    addGraph(graph, graph1);
    addGraph(graph, graph2);
    return graph;
  }

  public static TinkerGraph resultToGraph(Result result) {
    TinkerGraph graph = new TinkerGraph();
    while (result.hasNext()) {
      Map<String, Object> map = result.next();
      for (Object value: map.values()) {
        if (null == value) {
          continue;
        } else if (value instanceof PropertyContainer) {
          addElement(graph, (PropertyContainer)value);
        } else if (value instanceof Path) {
          for (PropertyContainer container: (Path)value) {
            addElement(graph, container);
          }
        } else if (value instanceof SeqWrapper) {
          for (Object thing: (SeqWrapper<?>)value) {
            if (thing instanceof PropertyContainer) {
              addElement(graph, (PropertyContainer) thing);
            }
          }
        } else {
          logger.warning("Not converting " + value.getClass() + " to tinker graph");
        }
      }
    }
    return graph;
  }

  static public <T> Optional<T> getProperty(Element container, String property,
      Class<T> type) {
    Optional<T> value = Optional.<T> absent();
    if (container.getPropertyKeys().contains(property)) {
      value = Optional.<T> of(type.cast(container.getProperty(property)));
    }
    return value;
  }

  static public <T> Collection<T> getProperties(Element container, String property,
      Class<T> type) {
    List<T> list = new ArrayList<>();
    if (container.getPropertyKeys().contains(property)) {
      return getPropertiesAsSet(container.getProperty(property), type);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  static <T> Set<T> getPropertiesAsSet(Object value, Class<T> type) {
    Set<T> set = new HashSet<>();
    if (value instanceof Collection) {
      return newHashSet((Collection<T>)value);
    }
    else if (value.getClass().isArray()) {
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

  public static void project(Graph graph, Collection<String> projection) {
    if (projection.contains("*")) {
      return;
    }
    for (Vertex vertex: graph.getVertices()) {
      for (String key: vertex.getPropertyKeys()) {
        if (!projection.contains(key) &&
            !PROTECTED_PROPERTY_KEYS.contains(key)) {
          vertex.removeProperty(key);
        }
      }
    }
  }

}
