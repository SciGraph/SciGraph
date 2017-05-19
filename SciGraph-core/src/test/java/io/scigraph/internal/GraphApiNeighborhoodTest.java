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
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Predicate;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import io.scigraph.neo4j.DirectedRelationshipType;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.util.GraphTestBase;

public class GraphApiNeighborhoodTest extends GraphTestBase {

  GraphApi graphApi;
  Node a, b, c, d, e, f, g, h, i, j;
  RelationshipType fizz = RelationshipType.withName("fizz");
  Optional<Predicate<Node>> absent = Optional.empty();

  @Before
  public void addNodes() throws Exception {
    a = graphDb.createNode();
    b = graphDb.createNode();
    c = graphDb.createNode();
    d = graphDb.createNode();
    e = graphDb.createNode();
    f = graphDb.createNode();
    g = graphDb.createNode();
    h = graphDb.createNode();
    i = graphDb.createNode();
    j = graphDb.createNode();
    b.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    c.createRelationshipTo(b, OwlRelationships.RDFS_SUBCLASS_OF);
    d.createRelationshipTo(c, OwlRelationships.RDFS_SUBCLASS_OF);
    i.createRelationshipTo(h, OwlRelationships.RDFS_SUBCLASS_OF);
    i.createRelationshipTo(g, OwlRelationships.RDFS_SUBCLASS_OF);
    h.createRelationshipTo(j, OwlRelationships.RDFS_SUBCLASS_OF);
    g.createRelationshipTo(j, OwlRelationships.RDFS_SUBCLASS_OF);
    e.createRelationshipTo(b, fizz);
    graphApi = new GraphApi(graphDb, cypherUtil, curieUtil);
  }

  @Test
  public void test1Neighborhood() {
    Graph graph = graphApi.getNeighbors(newHashSet(b), 1, Collections.<DirectedRelationshipType>emptySet(), absent);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(4));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(3));
  }

  @Test
  public void testKNeighborhood() {
    Graph graph = graphApi.getNeighbors(newHashSet(b), 10, Collections.<DirectedRelationshipType>emptySet(), absent);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(5));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(4));
  }

  @Test
  public void testTypedNeighborhood() {
    Graph graph = graphApi.getNeighbors(newHashSet(b), 2, newHashSet(new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING)), absent);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(3));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(2));
  }

  @Test
  public void testMultiTypedNeighborhood() {
    Graph graph = graphApi.getNeighbors(newHashSet(b), 1, 
        newHashSet(new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING),
            new DirectedRelationshipType(fizz, Direction.INCOMING)), absent);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(3));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(2));
  }

  @Test
  public void testSingleNodeNeighborhood() {
    Graph graph = graphApi.getNeighbors(newHashSet(f), 1, Collections.<DirectedRelationshipType>emptySet(), absent);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(1));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(0));
  }

  @Test
  public void testPredicate() {
    Predicate<Node> testPredicate = new Predicate<Node>() {
      @Override
      public boolean apply(Node node) {
        return (node != c);
      }};
      Graph graph = graphApi.getNeighbors(newHashSet(b), 1, Collections.<DirectedRelationshipType>emptySet(), Optional.of(testPredicate));
      assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(4));
      assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(3));
  }

  @Test
  public void multipleAncestors_areReturned() {
    Graph graph = graphApi.getNeighbors(newHashSet(i), 10,
        newHashSet(new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.OUTGOING)), absent);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(4));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(4));
  }

}
