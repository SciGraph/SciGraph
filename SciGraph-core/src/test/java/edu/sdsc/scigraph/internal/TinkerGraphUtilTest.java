package edu.sdsc.scigraph.internal;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class TinkerGraphUtilTest {

  TinkerGraph graph;
  Node node, otherNode;

  Node mockNode() {
    Node node = mock(Node.class);
    when(node.getPropertyKeys()).thenReturn(Collections.<String>emptySet());
    when(node.getLabels()).thenReturn(Collections.<Label>emptySet());
    return node;
  }
  
  @Before
  public void setup() {
    node = mockNode();
    otherNode = mockNode();
    when(node.getPropertyKeys()).thenReturn(Collections.<String>emptySet());
    when(node.getLabels()).thenReturn(Collections.<Label>emptySet());
    graph = new TinkerGraph();
  }

  @Test
  public void testIdTranslation() {
    when(node.getId()).thenReturn(1L);
    when(node.getPropertyKeys()).thenReturn(Collections.<String>emptySet());
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat(v.getId(), is((Object)"1"));
  }

  @Test
  public void testMultipleIds() {
    when(node.getId()).thenReturn(1L);
    when(node.getPropertyKeys()).thenReturn(Collections.<String>emptySet());
    Vertex v1 = TinkerGraphUtil.addNode(graph, node);
    Vertex v2 = TinkerGraphUtil.addNode(graph, node);
    assertThat(v1, is(v2));
  }

  @Test
  public void testPropertyTranslation() {
    when(node.getPropertyKeys()).thenReturn(newHashSet("foo", "baz"));
    when(node.getProperty("foo")).thenReturn("bar");
    when(node.getProperty("baz")).thenReturn(true);
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat(v.getProperty("foo"), is((Object)"bar"));
    assertThat(v.getProperty("baz"), is((Object)true));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLabelTranslation() {
    Label label = DynamicLabel.label("label");
    when(node.getLabels()).thenReturn(newHashSet(label));
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat((Iterable<String>)v.getProperty("types"), IsIterableContainingInAnyOrder.containsInAnyOrder("label"));
  }
  
  @Test
  public void testRelationshipTranslation() {
    when(node.getId()).thenReturn(1L);
    when(otherNode.getId()).thenReturn(2L);
    Vertex u = TinkerGraphUtil.addNode(graph, node);
    Vertex v = TinkerGraphUtil.addNode(graph, otherNode);
    Relationship relationship = mock(Relationship.class);
    when(relationship.getEndNode()).thenReturn(node);
    when(relationship.getStartNode()).thenReturn(otherNode);
    when(relationship.getType()).thenReturn(DynamicRelationshipType.withName("foo"));
    when(relationship.getPropertyKeys()).thenReturn(newHashSet("bar"));
    when(relationship.getProperty("bar")).thenReturn("baz");
    Edge edge = TinkerGraphUtil.addEdge(graph, relationship);
    assertThat(edge.getVertex(Direction.IN), is(u));
    assertThat(edge.getVertex(Direction.OUT), is(v));
    assertThat(edge.getLabel(), is("foo"));
    assertThat((String)edge.getProperty("bar"), is("baz"));
    Edge edge2 = TinkerGraphUtil.addEdge(graph, relationship);
    assertThat(edge, is(edge2));
  }

}
