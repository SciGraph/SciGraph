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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.prefixcommons.CurieUtil;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.NodeProperties;
import scala.collection.convert.Wrappers.SeqWrapper;

/***
 * Utilities for building TinkerGraphs from Neo4j objects
 */
public class TinkerGraphUtil {

  static final Logger logger = Logger.getLogger(TinkerGraphUtil.class.getName());

  static final ImmutableSet<String> PROTECTED_PROPERTY_KEYS = ImmutableSet.of(CommonProperties.IRI, CommonProperties.CURIE);

  private final CurieUtil curieUtil;
  
  private Graph graph;

  @Inject
  public TinkerGraphUtil(CurieUtil curieUtil) {
    this.curieUtil = curieUtil;
    this.graph = new TinkerGraph();
  }

  public TinkerGraphUtil(Graph graph, CurieUtil curieUtil) {
    this.graph = graph;
    this.curieUtil = curieUtil;
  }

  public CurieUtil getCurieUtil() {
    return curieUtil;
  }

  public Graph getGraph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  void copyProperties(PropertyContainer container, Element element) {
    for (String key : container.getPropertyKeys()) {
      Object property = container.getProperty(key);
      if (property.getClass().isArray()) {
        List<Object> propertyList = new ArrayList<>();
        for (int i = 0; i < Array.getLength(property); i++) {
          propertyList.add(Array.get(property, i));
        }
        property = propertyList;
      }
      else if (key.equals(CommonProperties.IRI) && String.class.isAssignableFrom(property.getClass())) {
        property = curieUtil.getCurie((String)property).orElse((String)property);
      }
      element.setProperty(key, property);
    }
  }

  Vertex addNode(Node node) {
    Vertex vertex = graph.getVertex(node.getId());
    if (null == vertex) {
      vertex = graph.addVertex(node.getId());
      copyProperties(node, vertex);
      Set<String> labels = new HashSet<>();
      for (Label label : node.getLabels()) {
        labels.add(label.name());
      }
      vertex.setProperty("types", labels);
    }
    return vertex;
  }

  Edge addEdge(Relationship relationship) {
    Edge edge = graph.getEdge(relationship.getId());
    if (null == edge) {
      Vertex outVertex = addNode(relationship.getStartNode());
      Vertex inVertex = addNode(relationship.getEndNode());
      String label = relationship.getType().name();
      Optional<String> curieLabel = curieUtil.getCurie(label);
      edge = graph.addEdge(relationship.getId(), outVertex, inVertex, curieLabel.orElse(label));
      copyProperties(relationship, edge);
    }
    return edge;
  }

  // TODO unit test that
  boolean removeEdge(Relationship relationship) {
    Edge edge = graph.getEdge(relationship.getId());
    if (null != edge) {
      graph.removeEdge(edge);
      return true;
    } else {
      return false;
    }
  }

  public Element addElement(PropertyContainer container) {
    if (container instanceof Node) {
      return addNode((Node) container);
    } else {
      return addEdge((Relationship) container);
    }
  }

  public void addPath(Iterable<PropertyContainer> path) {
    for (PropertyContainer container : path) {
      addElement(container);
    }
  }

  static void copyProperties(Element source, Element target) {
    for (String key : source.getPropertyKeys()) {
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

  Vertex addNode(Vertex node) {
    Vertex vertex = graph.getVertex(node.getId());
    if (null == vertex) {
      vertex = graph.addVertex(node.getId());
      copyProperties(node, vertex);
    }
    return vertex;
  }

  Edge addEdge(Edge edge) {
    Edge newEdge = graph.getEdge(edge.getId());
    if (null == newEdge) {
      Vertex outVertex = addNode(edge.getVertex(Direction.OUT));
      Vertex inVertex = addNode(edge.getVertex(Direction.IN));
      String label = edge.getLabel();
      newEdge = graph.addEdge(edge.getId(), outVertex, inVertex, label);
      copyProperties(edge, edge);
    }
    return newEdge;
  }


  public Element addElement(Element element) {
    if (element instanceof Vertex) {
      return addNode((Vertex) element);
    } else {
      return addEdge((Edge) element);
    }
  }

  public void addGraph(Graph addition) {
    for (Vertex vertex : addition.getVertices()) {
      addElement(vertex);
    }
    for (Edge edge : addition.getEdges()) {
      addElement(edge);
    }
  }

  public Graph combineGraphs(Graph graph2) {
    TinkerGraphUtil tgu = new TinkerGraphUtil(curieUtil);
    tgu.addGraph(graph);
    tgu.addGraph(graph2);
    return tgu.getGraph();
  }

  public Graph resultToGraph(Result result) {
    graph = new TinkerGraph();
    while (result.hasNext()) {
      Map<String, Object> map = result.next();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        Object value = entry.getValue();
        String key = entry.getKey();
        if (null == value) {
          continue;
        } else if (value instanceof PropertyContainer) {
          addElement((PropertyContainer) value);
        } else if (value instanceof Path) {
          for (PropertyContainer container : (Path) value) {
            addElement(container);
          }
        } else if (value instanceof SeqWrapper) {
          for (Object thing : (SeqWrapper<?>) value) {
            if (thing instanceof PropertyContainer) {
              addElement((PropertyContainer) thing);
            }
          }
        } else if (value instanceof Boolean) {
          // generates a lonely node which contains the result
          Vertex vertex = graph.addVertex(key);
          vertex.setProperty(key, value);
          vertex.setProperty(NodeProperties.LABEL, "Boolean result");
          vertex.setProperty(CommonProperties.IRI, key);
        } else {
          logger.warning("Not converting " + value.getClass() + " to tinker graph");
        }
      }
    }
    return graph;
  }

  static public <T> Optional<T> getProperty(Element container, String property, Class<T> type) {
    Optional<T> value = Optional.<T>empty();
    if (container.getPropertyKeys().contains(property)) {
      value = Optional.<T>of(type.cast(container.getProperty(property)));
    }
    return value;
  }

  static public <T> Collection<T> getProperties(Element container, String property, Class<T> type) {
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
      return newHashSet((Collection<T>) value);
    } else if (value.getClass().isArray()) {
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

  public void project(Collection<String> projection) {
    if (projection.contains("*")) {
      return;
    }
    for (Vertex vertex : graph.getVertices()) {
      for (String key : vertex.getPropertyKeys()) {
        if (!projection.contains(key) && !PROTECTED_PROPERTY_KEYS.contains(key)) {
          vertex.removeProperty(key);
        }
      }
    }
  }

}
