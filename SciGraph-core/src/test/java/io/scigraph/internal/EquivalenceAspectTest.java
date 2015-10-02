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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.scigraph.frames.NodeProperties;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.owlapi.curies.CurieUtil;
import io.scigraph.util.GraphTestBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import com.google.common.base.Optional;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class EquivalenceAspectTest extends GraphTestBase {

  Node clique11, clique12, clique13, clique21, clique22;
  Graph graph = new TinkerGraph();
  EquivalenceAspect aspect;

  @Before
  public void setup() {
    clique11 = createNode("http://x.org/a");
    clique12 = createNode("http://x.org/b");
    clique13 = createNode("http://x.org/c");
    clique21 = createNode("http://x.org/d");
    clique22 = createNode("http://x.org/e");
    Relationship r1 = clique11.createRelationshipTo(clique12, EquivalenceAspect.IS_EQUIVALENT);
    Relationship r2 = clique12.createRelationshipTo(clique13, EquivalenceAspect.IS_EQUIVALENT);
    Relationship r3 = clique21.createRelationshipTo(clique22, EquivalenceAspect.IS_EQUIVALENT);
    Relationship r4 = clique12.createRelationshipTo(clique22, DynamicRelationshipType.withName("hasPhenotype"));
    Relationship r5 = clique13.createRelationshipTo(clique21, DynamicRelationshipType.withName("hasPhenotype"));

    Map<String, String> curieMap = new HashMap<String, String>();
    CurieUtil curieUtil = new CurieUtil(curieMap);
    CypherUtil cypherUtil = new CypherUtil(graphDb, curieUtil);

    aspect = new EquivalenceAspect(graphDb);
  }

  @Test
  public void edgesAreMovedToLeader() {


    // Iterator<Vertex> vertices = graph.getVertices().iterator();
    // Vertex v1 = vertices.next();
    // Vertex v2 = vertices.next();
    // Vertex v3 = vertices.next();
    //
    // assertThat((String) v1.getProperty(NodeProperties.IRI), is("http://x.org/a"));
    // assertThat((String) v2.getProperty(NodeProperties.IRI), is("http://x.org/b"));
    // assertThat((String) v3.getProperty(NodeProperties.IRI), is("http://x.org/c"));
    // assertThat(v1.getVertices(Direction.BOTH, "hasPhenotype"),
    // IsIterableWithSize.<Vertex>iterableWithSize(0));
    // assertThat(v2.getVertices(Direction.BOTH, "hasPhenotype"),
    // IsIterableWithSize.<Vertex>iterableWithSize(1));
    // assertThat(v3.getVertices(Direction.BOTH, "hasPhenotype"),
    // IsIterableWithSize.<Vertex>iterableWithSize(1));
    // aspect.invoke(graph);
    // assertThat(v1.getVertices(Direction.BOTH, "hasPhenotype"),
    // IsIterableWithSize.<Vertex>iterableWithSize(2));
    // assertThat(v2.getVertices(Direction.BOTH, "hasPhenotype"),
    // IsIterableWithSize.<Vertex>iterableWithSize(0));
    // assertThat(v3.getVertices(Direction.BOTH, "hasPhenotype"),
    // IsIterableWithSize.<Vertex>iterableWithSize(0));
    //


    GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(graphDb);

    Transaction tx = graphDb.beginTx();
    ResourceIterator<Node> allNodes = globalGraphOperations.getAllNodes().iterator();
    Node n1 = getNode("http://x.org/a", allNodes);
    Node n2 = getNode("http://x.org/b", allNodes);
    Node n3 = getNode("http://x.org/c", allNodes);
    assertThat(n1.getDegree(DynamicRelationshipType.withName("hasPhenotype")), is(0));
    assertThat(n2.getDegree(DynamicRelationshipType.withName("hasPhenotype")), is(1));
    assertThat(n3.getDegree(DynamicRelationshipType.withName("hasPhenotype")), is(1));
    assertThat(n1.getDegree(EquivalenceAspect.IS_EQUIVALENT), is(1));
    assertThat(n2.getDegree(EquivalenceAspect.IS_EQUIVALENT), is(2));
    assertThat(n3.getDegree(EquivalenceAspect.IS_EQUIVALENT), is(1));

    aspect.invoke(graph);

    assertThat(n1.getDegree(DynamicRelationshipType.withName("hasPhenotype")), is(2));
    assertThat(n2.getDegree(DynamicRelationshipType.withName("hasPhenotype")), is(0));
    assertThat(n3.getDegree(DynamicRelationshipType.withName("hasPhenotype")), is(0));
    assertThat(n1.getDegree(EquivalenceAspect.IS_EQUIVALENT), is(2));
    assertThat(n2.getDegree(EquivalenceAspect.IS_EQUIVALENT), is(1));
    assertThat(n3.getDegree(EquivalenceAspect.IS_EQUIVALENT), is(1));


    // Check that the edges are tagged with a reference of the original node
    // for(Edge edge : graph.getEdges()){
    // String equivalentOriginalNodeSource =
    // edge.getProperty(EquivalenceAspect.ORIGINAL_REFERENCE_KEY_SOURCE);
    // if(equivalentOriginalNodeSource != null) {
    // assertThat(equivalentOriginalNodeSource.equals("http://x.org/b") ||
    // equivalentOriginalNodeSource.equals("http://x.org/c"), is(true));
    // }
    // String equivalentOriginalNodeTarget =
    // edge.getProperty(EquivalenceAspect.ORIGINAL_REFERENCE_KEY_TARGET);
    // if(equivalentOriginalNodeTarget != null) {
    // assertThat(equivalentOriginalNodeTarget.equals("http://x.org/e") ||
    // equivalentOriginalNodeTarget.equals("http://x.org/d"), is(true));
    // }
    // }
  }

  @Test
  public void leaderPrioritizer() {
    Node a = createNode("http://x.org/a");
    Node c = createNode("http://y.org/c");
    Node d = createNode("http://z.org/d");
    List<Node> clique = Arrays.asList(a, createNode("http://x.org/b"), c, d, createNode("http://x.org/e"));
    assertThat(aspect.electCliqueLeader(clique, new ArrayList<String>()).getId(), is(a.getId()));
    assertThat(aspect.electCliqueLeader(clique, Arrays.asList("http://z.org/", "http://x.org/", "http://y.org/")).getId(), is(d.getId()));
    assertThat(aspect.electCliqueLeader(clique, Arrays.asList("fake", "fake", "fake")).getId(), is(a.getId()));
    assertThat(aspect.electCliqueLeader(clique, Arrays.asList("http://y.org/", "http://x.org/", "http://y.org/")).getId(), is(c.getId()));
    assertThat(aspect.electCliqueLeader(clique, Arrays.asList("http://x.org/", "http://x.org/", "http://y.org/")).getId(), is(a.getId()));
  }

  private Node getNode(String iri, Iterator<Node> allNodes) {
    while (allNodes.hasNext()) {
      Node currentNode = allNodes.next();
      Optional<String> optionalIri = GraphUtil.getProperty(currentNode, NodeProperties.IRI, String.class);
      if (optionalIri.isPresent()) {
        if (optionalIri.get().equals(iri)) {
          return currentNode;
        }
      }
    }
    return null;
  }
}
