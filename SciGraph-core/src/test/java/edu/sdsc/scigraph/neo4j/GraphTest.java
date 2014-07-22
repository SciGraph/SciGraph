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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphTest extends GraphTestBase {

  static String BASE_URI = "http://example.org/";

  static String uri = BASE_URI + "#fizz";
  static String uri2 = BASE_URI + "#fuzz";
  static String uri3 = BASE_URI + "#fazz";
  static String rel = BASE_URI + "#relationship";
  Node a;
  Node b;
  Node c;

  Graph<Concept> graph;

  @Before
  public void addNodes() throws Exception {
    graph = new Graph<Concept>(graphDb, Concept.class);
    a = graph.getOrCreateNode(uri);
    b = graph.getOrCreateNode(uri2);
    c = graph.getOrCreateNode(uri3);
  }

  @Test
  public void testNodeDoesntExist() {
    assertThat("This node should not exist.", graph.nodeExists(BASE_URI + "#noSuchNode"), is(false));
  }

  @Test
  public void testNodeExists() {
    String uri = BASE_URI + "#node";
    graph.getOrCreateNode(uri);
    assertThat("Node should have been created", graph.nodeExists(uri), is(true));
  }

  @Test
  public void testCreateAndRetrieveNode() {
    String uri = BASE_URI + "#createdNode";
    Node node = graph.getOrCreateNode(uri);
    assertThat(node, is(not(nullValue())));
    Node node2 = graph.getOrCreateNode(uri);
    assertThat("The same node should be retrieved", node2, is(equalTo(node)));
    assertThat("There should be four nodes total", newArrayList(GlobalGraphOperations.at(graphDb).getAllNodes()), hasSize(4));
    assertThat("The graph should have the created node", GlobalGraphOperations.at(graphDb).getAllNodes(), hasItems(node));
  }

  @Test
  public void testFragment() {
    assertThat((String)a.getProperty(CommonProperties.FRAGMENT), is(equalTo("fizz")));
  }

  @Test
  public void testHasRelationship() {
    assertFalse("No relationship should exist", graph.hasRelationship(a, b, EdgeType.REL));
    Relationship r = graph.getOrCreateRelationship(a, b, EdgeType.REL);
    assertEquals(a, r.getStartNode());
    assertEquals(b, r.getEndNode());
    assertTrue(graph.hasRelationship(a, b, EdgeType.REL));
    assertThat("There should be one relationship", newArrayList(GlobalGraphOperations.at(graphDb).getAllRelationships()), hasSize(1));
    assertThat("One relationship should exist", GlobalGraphOperations.at(graphDb)
        .getAllRelationships(), hasItems(r));
  }

  @Test
  public void testUniqueRelationship() {
    Relationship r = graph.getOrCreateRelationship(a, b, EdgeType.REL);
    Relationship r1 = graph.getOrCreateRelationship(a, b, EdgeType.REL);
    assertEquals(r, r1);
    assertThat("There should be one relationship", newArrayList(GlobalGraphOperations.at(graphDb)
        .getAllRelationships()), hasSize(1));
    assertThat("The graph should have r", GlobalGraphOperations.at(graphDb).getAllRelationships(), contains(r));
    graph.getOrCreateRelationship(a, b, EdgeType.SUBCLASS_OF);
    assertThat("There should be two relationships", newArrayList(GlobalGraphOperations.at(graphDb).getAllRelationships()), hasSize(2));
  }

  @Test
  public void testHasRelationshipWithUri() {
    Relationship r = graph.getOrCreateRelationship(a, b, EdgeType.REL, rel);
    assertEquals(rel, r.getProperty(CommonProperties.URI));
    assertEquals("relationship", r.getProperty(CommonProperties.FRAGMENT));
    assertTrue(graph.hasRelationship(a, b, EdgeType.REL, rel));
    assertFalse(graph.hasRelationship(a, b, EdgeType.IS_A, rel));
  }

  @Test
  public void testNoUriFragment() {
    Node node = graph.getOrCreateNode(BASE_URI + "Fragment");
    assertThat((String)node.getProperty(CommonProperties.FRAGMENT), is(equalTo("Fragment")));
  }

  @Test(expected = IllegalStateException.class)
  public void testNullNonURI() {
    assertNull(Graph.getURI("4:nonuri"));
  }

  @Test
  public void testSingleProperty() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.addProperty(node, "foo", "bar");
    assertEquals("bar", node.getProperty("foo"));
  }

  @Test
  public void testRelationshipProperty() {
    Relationship r = graph.getOrCreateRelationship(a, b, EdgeType.REL, rel);
    graph.setProperty(r, "foo", false);
    assertThat((Boolean)r.getProperty("foo"), is(false));
  }

  @Test
  public void testMultipleProperties() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.addProperty(node, "foo", "bar");
    graph.addProperty(node, "foo", "baz");
    assertThat(newArrayList((String[])node.getProperty("foo")), contains("bar", "baz"));
    graph.addProperty(node, "foo", "bat");
    assertThat(newArrayList((String[])node.getProperty("foo")), contains("bar", "baz", "bat"));
  }

  @Test
  public void testDuplicateProperties() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.addProperty(node, "foo", "bar");
    graph.addProperty(node, "foo", "bar");
    assertThat(newArrayList((String)node.getProperty("foo")), contains("bar"));
  }

  @Test
  public void testWhiteSpaceProperty() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.setProperty(node, "foo", " ");
    assertThat(node.hasProperty("foo"), is(false));
    graph.addProperty(node, "foo", " ");
    assertThat(node.hasProperty("foo"), is(false));
  }

  @Test
  public void testOtherPropertyTypes() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.addProperty(node, "foo", 1);
    graph.addProperty(node, "foo", 2);
    int[] expected = {1, 2};
    assertTrue(Arrays.equals((int[])node.getProperty("foo"), expected));
    graph.addProperty(node, "foo", 3);
    int[] expected2 = {1, 2, 3};
    assertTrue(Arrays.equals((int[])node.getProperty("foo"), expected2));
  }

  @Test
  public void testGetSingleProperty() {
    Node node = graph.getOrCreateNode(BASE_URI);
    assertFalse("Missing values should be absent", graph.getProperty(node, "foo", String.class).isPresent());
    graph.addProperty(node, "foo", "bar");
    assertEquals("Single values should match", Optional.of("bar"), graph.getProperty(node, "foo", String.class));
  }

  @Test(expected = ClassCastException.class)
  public void testPropertyTypes() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.addProperty(node, "foo", "bar");
    graph.getProperty(node, "foo", Integer.class).get();
  }

  @Test(expected = ClassCastException.class)
  public void testDifferentMultiplePropertyTypes() {
    Node node = graph.getOrCreateNode(BASE_URI);
    graph.addProperty(node, "foo", "bar");
    graph.addProperty(node, "foo", 1);
  }

  @Test
  public void testGetMultipleProperties() {
    Node node = graph.getOrCreateNode(BASE_URI);
    assertTrue("Missing properties return empty collections", graph.getProperties(node, "foo", String.class).isEmpty());
    graph.addProperty(node, "foo", "bar");
    assertThat("Single properties return", graph.getProperties(node, "foo", String.class), contains("bar"));
    graph.addProperty(node, "foo", "baz");
    assertThat("Multiple properties return", graph.getProperties(node, "foo", String.class), contains("bar", "baz"));
  }

  @Test
  public void testGetOrCreateFamedNode() {
    String uri = BASE_URI + "#createdNode";
    Node node = graph.getOrCreateNode(uri);
    graph.setProperty(node, NodeProperties.LABEL, "foo");
    assertThat(getOnlyElement(graph.getOrCreateFramedNode(uri).getLabels()), is("foo"));
  }

  @Test
  public void testGetFramedRelationship() {
    graph.getOrCreateRelationship(a, b, EdgeType.SUPERCLASS_OF);
    Concept aFrame = graph.getOrCreateFramedNode(a);
    Concept bFrame = graph.getOrCreateFramedNode(b);
    assertThat(aFrame.getSubclasses(), contains(bFrame));
  }

  @Test
  public void testGetFramedEquivalence() {
    graph.getOrCreateRelationship(a, b, EdgeType.EQUIVALENT_TO);
    graph.getOrCreateRelationship(b, a, EdgeType.EQUIVALENT_TO);
    Concept aFrame = graph.getOrCreateFramedNode(a);
    Concept bFrame = graph.getOrCreateFramedNode(b);
    assertThat(aFrame.getEquivalentClasses(), contains(bFrame));
    assertThat(bFrame.getEquivalentClasses(), contains(aFrame));
  }

  @Test
  public void testUpdateFramedNode() {
    String uri = BASE_URI + "#foo";
    //assertThat(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*"), hasItems(a, b, c));
    System.out.println(newArrayList(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*").iterator()));
    Concept concept = graph.getOrCreateFramedNode(uri);
    //Node node = graph.getOrCreateNode(uri);
    concept.setFragment("foo");
    //assertThat(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*"), hasItems(a, b, c, node));
    System.out.println(newArrayList(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*").iterator()));
    concept.setFragment("foo2");
    //assertThat(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*"), hasItems(a, b, c, node));
    System.out.println(newArrayList(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*").iterator()));
    concept.setFragment("baz");
    //assertThat(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*"), hasItems(a, b, c));
    System.out.println(newArrayList(graphDb.index().getNodeAutoIndexer().getAutoIndex().query("fragment:f*").iterator()));
  }

  @Test
  public void testCreateRelationshipsPairwise() {
    List<Node> nodes = newArrayList(a, b, c);
    graph.getOrCreateRelationshipPairwise(nodes, EdgeType.EQUIVALENT_TO, Optional.<URI>absent());
    assertThat(graph.hasRelationship(a, b, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(a, c, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(b, c, EdgeType.EQUIVALENT_TO), is(true));
  }

  @Test
  public void testPropertyCopy() {
    graph.setProperty(a, NodeProperties.LABEL, "test");
    assertThat(graph.getProperty(a, NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX, String.class).get(), is(equalTo("test")));
  }

  @Test
  public void testStopwordProperties() {
    // HACK: Don't store stopword properties becuase it ruins indexing...
    graph.setProperty(a, NodeProperties.LABEL, "a");
    assertThat(a.hasProperty(NodeProperties.LABEL), is(false));
  }

}
