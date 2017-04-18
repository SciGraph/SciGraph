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

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.size;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Collection;
import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.Graph;

import io.scigraph.frames.NodeProperties;
import io.scigraph.neo4j.DirectedRelationshipType;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.util.GraphTestBase;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

public class GraphApiTest extends GraphTestBase {

  GraphApi graphApi;
  Node a, b, c;

  @Before
  public void addNodes() throws Exception {
    a = graphDb.createNode();
    a.setProperty(NodeProperties.IRI, "a");
    a.setProperty("foo", "bar");
    a.addLabel(Label.label("alabel"));
    b = graphDb.createNode();
    b.setProperty(NodeProperties.IRI, "b");
    c = graphDb.createNode();
    c.setProperty(NodeProperties.IRI, "c");
    b.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    c.createRelationshipTo(b, OwlRelationships.OWL_EQUIVALENT_CLASS);
    graphApi = new GraphApi(graphDb, cypherUtil, curieUtil);
  }

  @Test
  public void entailment_isReturned() {
    Collection<Node> entailment = graphApi.getEntailment(a,
        new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING), false);
    assertThat(entailment, contains(a, b));
  }

  @Test
  public void equivalentEntailment_isReturned() {
    Collection<Node> entailment = graphApi.getEntailment(a,
        new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING), true);
    assertThat(entailment, contains(a, b, c));
  }

  @Test
  public void allPropertyKeys_areReturned() {
    assertThat(graphApi.getAllPropertyKeys(), hasItems("foo"));
  }

  @Test
  public void allRelationshipTypes_areReturned() {
    // TODO: RelationshipTypeTokens don't equal RelationshipTypes
    assertThat(getFirst(graphApi.getAllRelationshipTypes(), null).name(),
        is(OwlRelationships.RDFS_SUBCLASS_OF.name()));
    assertThat(getLast(graphApi.getAllRelationshipTypes()).name(),
        is(OwlRelationships.OWL_EQUIVALENT_CLASS.name()));
  }

  @Test
  public void edges_areReturned() {
    Graph graph = graphApi.getEdges(OwlRelationships.RDFS_SUBCLASS_OF, false, 0L, 1L);
    assertThat(size(graph.getVertices()), is(2));
    assertThat(size(graph.getEdges()), is(1));
  }

  @Test
  public void edges_queryIsEntailed() {
    Graph graph = graphApi.getEdges(OwlRelationships.RDFS_SUBCLASS_OF, true, 0L, 1L);
    assertThat(size(graph.getVertices()), is(2));
    assertThat(size(graph.getEdges()), is(1));
  }

  @Test
  public void getReachableNodes_areReturned() {
    Graph graph = graphApi.getReachableNodes(b,
        Lists.newArrayList(OwlRelationships.RDFS_SUBCLASS_OF.name()), Sets.newHashSet());
    assertThat(size(graph.getVertices()), is(1));
    assertThat(size(graph.getEdges()), is(0));
    graph = graphApi.getReachableNodes(c,
        Lists.newArrayList(OwlRelationships.OWL_EQUIVALENT_CLASS.name(),
            OwlRelationships.RDFS_SUBCLASS_OF.name()),
        Sets.newHashSet());
    assertThat(size(graph.getVertices()), is(1));
    assertThat(size(graph.getEdges()), is(0));
  }

  @Test
  public void getReachableNodes_nothingReturnedForFakeLabel() {
    Graph graph = graphApi.getReachableNodes(c,
        Lists.newArrayList(OwlRelationships.OWL_EQUIVALENT_CLASS.name(),
            OwlRelationships.RDFS_SUBCLASS_OF.name()),
        Sets.newHashSet("fakeLabel"));
    assertThat(size(graph.getVertices()), is(0));
    assertThat(size(graph.getEdges()), is(0));
  }

  @Test
  public void getReachableNodes_traverseAllRels() {
    Graph graph = graphApi.getReachableNodes(c, Lists.newArrayList(), Sets.newHashSet());
    assertThat(size(graph.getVertices()), is(1));
    assertThat(size(graph.getEdges()), is(0));
  }
  
  @Test
  public void getReachableNodes_fetchesAll() {
    Graph graph = graphApi.getReachableNodes(c, Lists.newArrayList("*"), Sets.newHashSet());
    assertThat(size(graph.getVertices()), is(2));
    assertThat(size(graph.getEdges()), is(0));
  }
  
  @Test
  public void getReachableNodes_filtersCorrectly() {
    Graph graph = graphApi.getReachableNodes(c, Lists.newArrayList("*"), Sets.newHashSet("alabel"));
    assertThat(size(graph.getVertices()), is(1));
    assertThat(size(graph.getEdges()), is(0));
  }
  
  @Test
  public void getNode_isReturned() {
    Optional<String> empty = Optional.empty();
    Optional<Node> node = graphApi.getNode("a", empty);
    assertThat(node.isPresent(), is(true));
    assertThat(node.get(), is(a));
  }

  @Test
  public void getNode_nothingReturnedForFakeLabel() {
    Optional<Node> node = graphApi.getNode("a", Optional.of("fakeLabel"));
    assertThat(node.isPresent(), is(false));
  }

  @Test
  public void getNode_nodeReturnedForValidLabel() {
    Optional<Node> node = graphApi.getNode("a", Optional.of("alabel"));
    assertThat(node.isPresent(), is(true));
    assertThat(node.get(), is(a));
  }

  @Test
  public void getNode_nothingReturnedForNonExisting() {
    Optional<String> empty = Optional.empty();
    Optional<Node> node = graphApi.getNode("z", empty);
    assertThat(node.isPresent(), is(false));
  }

  @Test
  @Ignore // Not sorting works in production but not in test
  public void edges_areSkipped() {
    Graph graph = graphApi.getEdges(OwlRelationships.RDFS_SUBCLASS_OF, false, Long.MAX_VALUE, 1L);
    assertThat(size(graph.getVertices()), is(0));
    assertThat(size(graph.getEdges()), is(0));
  }

}
