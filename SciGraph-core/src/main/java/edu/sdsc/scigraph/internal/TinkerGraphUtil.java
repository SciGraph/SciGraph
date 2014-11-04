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

public class TinkerGraphUtil {

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
}
