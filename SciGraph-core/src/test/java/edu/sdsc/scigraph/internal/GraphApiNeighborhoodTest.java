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

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.neo4j.GraphInterface;
import edu.sdsc.scigraph.neo4j.GraphInterfaceTransactionImpl;
import edu.sdsc.scigraph.neo4j.RelationshipMap;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiNeighborhoodTest extends GraphTestBase {

  GraphApi graphApi;
  Node a, b, c, d, e, f;
  RelationshipType fizz = DynamicRelationshipType.withName("fizz");
  Optional<Predicate<Node>> absent = Optional.absent();
  GraphInterface graph;

  Node createNode(String uri) {
    long node = graph.createNode(uri);
    graph.setNodeProperty(node, "uri", uri);
    return graphDb.getNodeById(node);
  }

  @Before
  public void addNodes() throws Exception {
    graph = new GraphInterfaceTransactionImpl(graphDb, new ConcurrentHashMap<String, Long>(), new RelationshipMap());
    a = createNode("http://example.org/a");
    b = createNode("http://example.org/b");
    c = createNode("http://example.org/c");
    d = createNode("http://example.org/d");
    e = createNode("http://example.org/e");
    f = createNode("http://example.org/f");
    b.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    c.createRelationshipTo(b, OwlRelationships.RDFS_SUBCLASS_OF);
    d.createRelationshipTo(c, OwlRelationships.RDFS_SUBCLASS_OF);
    e.createRelationshipTo(b, fizz);
    graphApi = new GraphApi(graphDb);
  }

  @Test
  public void test1Neighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(newHashSet(b), 1, Collections.<DirectedRelationshipType>emptySet(), absent);
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(4)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(3)));
  }

  @Test
  public void testKNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(newHashSet(b), 10, Collections.<DirectedRelationshipType>emptySet(), absent);
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(5)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(4)));
  }

  @Test
  public void testTypedNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(newHashSet(b), 2, newHashSet(new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING)), absent);
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(3)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(2)));
  }

  @Test
  public void testMultiTypedNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(newHashSet(b), 1, 
        newHashSet(new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING),
            new DirectedRelationshipType(fizz, Direction.INCOMING)), absent);
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(3)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(2)));
  }

  @Test
  public void testSingleNodeNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(newHashSet(f), 1, Collections.<DirectedRelationshipType>emptySet(), absent);
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(1)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(0)));
  }

  @Test
  public void testPredicate() {
    Predicate<Node> testPredicate = new Predicate<Node>() {
      @Override
      public boolean apply(Node node) {
        return !((String)node.getProperty("uri")).endsWith("c");
      }};
      TinkerGraph graph = graphApi.getNeighbors(newHashSet(b), 1, Collections.<DirectedRelationshipType>emptySet(), Optional.of(testPredicate));
      assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(3)));
      assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(2)));
  }

}
