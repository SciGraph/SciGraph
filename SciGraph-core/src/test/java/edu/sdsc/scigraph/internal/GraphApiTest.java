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

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.size;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.Graph;

import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiTest extends GraphTestBase {

  GraphApi graphApi;
  Node a, b, c;

  @Before
  public void addNodes() throws Exception {
    a = graphDb.createNode();
    a.setProperty("foo", "bar");
    b = graphDb.createNode();
    c = graphDb.createNode();
    b.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    c.createRelationshipTo(b, OwlRelationships.OWL_EQUIVALENT_CLASS);
    graphApi = new GraphApi(graphDb, cypherUtil);
  }

  @Test
  public void entailment_isReturned() {
    Collection<Node> entailment = graphApi.getEntailment(a, new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING), false);
    assertThat(entailment, contains(a, b));
  }

  @Test
  public void equivalentEntailment_isReturned() {
    Collection<Node> entailment = graphApi.getEntailment(a, new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING), true);
    assertThat(entailment, contains(a, b, c));
  }

  @Test
  public void allPropertyKeys_areReturned() {
    assertThat(graphApi.getAllPropertyKeys(), contains("foo"));
  }

  @Test
  public void allRelationshipTypes_areReturned() {
    // TODO: RelationshipTypeTokens don't equal RelationshipTypes
    assertThat(getFirst(graphApi.getAllRelationshipTypes(), null).name(), is(OwlRelationships.RDFS_SUBCLASS_OF.name()));
    assertThat(getLast(graphApi.getAllRelationshipTypes()).name(), is(OwlRelationships.OWL_EQUIVALENT_CLASS.name()));
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
  public void edges_areSkipped() {
    Graph graph = graphApi.getEdges(OwlRelationships.RDFS_SUBCLASS_OF, false, Long.MAX_VALUE, 1L);
    assertThat(size(graph.getVertices()), is(0));
    assertThat(size(graph.getEdges()), is(0));
  }

}
