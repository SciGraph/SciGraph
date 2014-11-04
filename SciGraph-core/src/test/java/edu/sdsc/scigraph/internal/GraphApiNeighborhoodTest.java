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

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiNeighborhoodTest extends GraphTestBase {

  GraphApi graphApi;
  Node a, b, c, d, e, f;
  RelationshipType subclass = DynamicRelationshipType.withName("subclassOf");
  RelationshipType fizz = DynamicRelationshipType.withName("fizz");

  @Before
  public void addNodes() throws Exception {
    Graph graph = new Graph(graphDb);
    a = graph.getOrCreateNode("http://example.org/a");
    b = graph.getOrCreateNode("http://example.org/b");
    c = graph.getOrCreateNode("http://example.org/c");
    d = graph.getOrCreateNode("http://example.org/d");
    e = graph.getOrCreateNode("http://example.org/e");
    f = graph.getOrCreateNode("http://example.org/f");
    b.createRelationshipTo(a, subclass);
    c.createRelationshipTo(b, subclass);
    d.createRelationshipTo(c, subclass);
    e.createRelationshipTo(b, fizz);
    graphApi = new GraphApi(graphDb);
  }

  @Test
  public void test1Neighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(b, 1, Collections.<DirectedRelationshipType>emptySet());
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(4)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(3)));
  }

  @Test
  public void testKNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(b, 10, Collections.<DirectedRelationshipType>emptySet());
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(5)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(4)));
  }

  @Test
  public void testTypedNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(b, 2, newHashSet(new DirectedRelationshipType(subclass, Direction.INCOMING)));
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(3)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(2)));
  }
  
  @Test
  public void testMultiTypedNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(b, 1, 
        newHashSet(new DirectedRelationshipType(subclass, Direction.INCOMING),
            new DirectedRelationshipType(fizz, Direction.INCOMING)));
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(3)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(2)));
  }
  
  @Test
  public void testSingleNodeNeighborhood() {
    TinkerGraph graph = graphApi.getNeighbors(f, 1, Collections.<DirectedRelationshipType>emptySet());
    assertThat(graph.getVertices(), is(IsIterableWithSize.<Vertex>iterableWithSize(1)));
    assertThat(graph.getEdges(), is(IsIterableWithSize.<Edge>iterableWithSize(0)));
  }

}
