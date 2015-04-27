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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class TinkerGraphUtilTest {

  TinkerGraph graph;
  Node node, otherNode;
  Relationship relationship;

  Node mockNode(long id) {
    Node node = mock(Node.class);
    when(node.getId()).thenReturn(id);
    when(node.getPropertyKeys()).thenReturn(Collections.<String>emptySet());
    when(node.getLabels()).thenReturn(Collections.<Label>emptySet());
    return node;
  }

  Relationship mockRealtionship(Node start, Node end) {
    Relationship r = mock(Relationship.class);
    when(r.getPropertyKeys()).thenReturn(Collections.<String>emptySet());
    when(r.getType()).thenReturn(DynamicRelationshipType.withName("FOO"));
    when(r.getStartNode()).thenReturn(start);
    when(r.getEndNode()).thenReturn(end);
    return r;
  }

  @Before
  public void setup() {
    node = mockNode(0L);
    otherNode = mockNode(1L);
    graph = new TinkerGraph();
    relationship = mockRealtionship(node, otherNode);
  }

  @Test
  public void idsAreTranslated() {
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat(v.getId(), is((Object)"0"));
  }

  @Test
  public void addNodeIsIdempotent() {
    Vertex v1 = TinkerGraphUtil.addNode(graph, node);
    Vertex v2 = TinkerGraphUtil.addNode(graph, node);
    assertThat(v1, is(v2));
  }

  @Test
  public void pathsAreTranslated() {
    Iterable<PropertyContainer> path = newArrayList(node, relationship, otherNode);
    TinkerGraphUtil.addPath(graph, path);
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(2)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(1)));
  }

  @Test
  public void propertiesAreTranslated() {
    when(node.getPropertyKeys()).thenReturn(newHashSet("foo", "baz"));
    when(node.getProperty("foo")).thenReturn("bar");
    when(node.getProperty("baz")).thenReturn(true);
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat(v.getProperty("foo"), is((Object)"bar"));
    assertThat(v.getProperty("baz"), is((Object)true));
  }

  @Test
  public void properties_areCopied() {
    Vertex v1 = graph.addVertex(1L);
    v1.setProperty("foo", "bar");
    Vertex v2 = graph.addVertex(2L);
    TinkerGraphUtil.copyProperties(v1, v2);
    assertThat((String)v2.getProperty("foo"), is("bar"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void arrayProperties_areCopied() {
    Vertex v1 = graph.addVertex(1L);
    v1.setProperty("foo", new String[] {"bar", "baz"});
    Vertex v2 = graph.addVertex(2L);
    TinkerGraphUtil.copyProperties(v1, v2);
    assertThat((List<String>)v2.getProperty("foo"), contains("bar", "baz"));
  }

  @Test
  public void arrayProperties_areMappedToLists() {
    when(node.getPropertyKeys()).thenReturn(newHashSet("foo", "bar"));
    when(node.getProperty("foo")).thenReturn(new String[]{"elt1", "elt2"});
    when(node.getProperty("bar")).thenReturn(new int[]{1,2});
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat(v.getProperty("foo"), is((Object)newArrayList("elt1", "elt2")));
    assertThat(v.getProperty("bar"), is((Object)newArrayList(1, 2)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void labelsAreTranslated() {
    Label label = DynamicLabel.label("label");
    when(node.getLabels()).thenReturn(newHashSet(label));
    Vertex v = TinkerGraphUtil.addNode(graph, node);
    assertThat((Iterable<String>)v.getProperty("types"), IsIterableContainingInAnyOrder.containsInAnyOrder("label"));
  }

  @Test
  public void relationshipsAreTranslated() {
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

  @Test
  public void graphsAreMerged() {
    TinkerGraph graph1 = new TinkerGraph();
    Vertex g1v1 = graph1.addVertex(0);
    Vertex g1v2 = graph1.addVertex(1);
    Edge g1e1 = graph1.addEdge(0, g1v1, g1v2, "test");
    TinkerGraph graph2 = new TinkerGraph();
    Vertex g2v1 = graph2.addVertex(1);
    Vertex g2v2 = graph2.addVertex(2);
    Edge g2e1 = graph1.addEdge(1, g2v1, g2v2, "test2");
    Graph graph = TinkerGraphUtil.combineGraphs(graph1, graph2);
    assertThat(graph.getVertices(), containsInAnyOrder(g1v1, g1v2, g2v2));
    assertThat(graph.getEdges(), containsInAnyOrder(g1e1, g2e1));
  }

  @Test
  public void primitivePropertiesAreReturned() {
    TinkerGraph graph = new TinkerGraph();
    Vertex v = graph.addVertex(1);
    assertThat(TinkerGraphUtil.getProperty(v, "foo", String.class), is(Optional.<String>absent()));
    v.setProperty("foo", "bar");
    assertThat(TinkerGraphUtil.getProperty(v, "foo", String.class), is(Optional.of("bar")));
  }

  @Test
  public void collectionsAreReturned() {
    TinkerGraph graph = new TinkerGraph();
    Vertex v = graph.addVertex(1);
    assertThat(TinkerGraphUtil.getProperties(v, "foo", String.class), is(empty()));
    v.setProperty("foo", "bar");
    assertThat(TinkerGraphUtil.getProperties(v, "foo", String.class), contains("bar"));
    v.setProperty("foo", newHashSet("bar", "baz"));
    assertThat(TinkerGraphUtil.getProperties(v, "foo", String.class), containsInAnyOrder("bar", "baz"));
    v.setProperty("foo", new String[] {"bar", "baz"});
    assertThat(TinkerGraphUtil.getProperties(v, "foo", String.class), containsInAnyOrder("bar", "baz"));
  }

}
