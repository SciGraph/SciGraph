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
package edu.sdsc.scigraph.internal;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/***
 * Utilities for building TinkerGraphs from Neo4j objects
 */
public final class TinkerGraphUtil {

  static void mapProperties(PropertyContainer container, Element element) {
    for (String key: container.getPropertyKeys()) {
      element.setProperty(key, container.getProperty(key));
    }
  }

  static Vertex addNode(TinkerGraph graph, Node node) {
    Vertex vertex = graph.getVertex(node.getId());
    if (null == vertex) {
      vertex = graph.addVertex(node.getId());
      mapProperties(node, vertex);
      Set<String> labels = new HashSet<>();
      for (Label label: node.getLabels()) {
        labels.add(label.name());
      }
      vertex.setProperty("types", labels);
    }
    return vertex;
  }

  static Edge addEdge(TinkerGraph graph, Relationship relationship) {
    Edge edge = graph.getEdge(relationship.getId());
    if (null == edge) {
      Vertex outVertex = addNode(graph, relationship.getStartNode());
      Vertex inVertex = addNode(graph, relationship.getEndNode());
      String label = relationship.getType().name();
      edge = graph.addEdge(relationship.getId(), outVertex, inVertex, label);
      mapProperties(relationship, edge);
    }
    return edge;
  }

  public static Element addElement(TinkerGraph graph, PropertyContainer container) {
    if (container instanceof Node) {
      return addNode(graph, (Node) container);
    } else {
      return addEdge(graph, (Relationship) container);
    }
  }

}
