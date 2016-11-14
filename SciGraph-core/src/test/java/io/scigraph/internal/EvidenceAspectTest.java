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
import io.scigraph.util.GraphTestBase;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class EvidenceAspectTest extends GraphTestBase {

  Node subject1, object1, object2, object3, association, association2, source, evidence;
  Graph graph = new TinkerGraph();
  EvidenceAspect aspect;

  @Before
  public void setup() {
    // Note: Do a cleaner test case, only assocation2 is relevant for now.
    String relationName = "http://x.org/hasSomething";
    Node relationNode = createNode(relationName);
    subject1 = createNode("http://x.org/a");
    object1 = createNode("http://x.org/b");
    object2 = createNode("http://x.org/c");
    object3 = createNode("http://x.org/d");
    association = createNode("http://x.org/e");
    association2 = createNode("http://x.org/f");
    source = createNode("http://x.org/g");
    evidence = createNode("http://x.org/h");
    association.createRelationshipTo(subject1, EvidenceAspect.HAS_SUBJECT);
    association.createRelationshipTo(object1, EvidenceAspect.HAS_OBJECT);
    association.createRelationshipTo(object2, EvidenceAspect.HAS_OBJECT);
    association.createRelationshipTo(source, EvidenceAspect.SOURCE);
    association.createRelationshipTo(evidence, EvidenceAspect.EVIDENCE);

    association2.createRelationshipTo(subject1, EvidenceAspect.HAS_SUBJECT);
    association2.createRelationshipTo(object3, EvidenceAspect.HAS_OBJECT);
    Relationship rel =
        subject1.createRelationshipTo(object3, DynamicRelationshipType.withName(relationName));
    association2.createRelationshipTo(relationNode, EvidenceAspect.OBJECT_PROPERTY);

    TinkerGraphUtil.addNode(graph, subject1);
    TinkerGraphUtil.addNode(graph, object1);
    TinkerGraphUtil.addNode(graph, object2);
    TinkerGraphUtil.addNode(graph, object3);
    TinkerGraphUtil.addNode(graph, relationNode);
    TinkerGraphUtil.addEdge(graph, rel);
    aspect = new EvidenceAspect(graphDb);
  }

  @Test
  public void evidenceIsAdded() {
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(5));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(1));
    aspect.invoke(graph);
    assertThat(graph.getVertices(), IsIterableWithSize.<Vertex>iterableWithSize(6));
    assertThat(graph.getEdges(), IsIterableWithSize.<Edge>iterableWithSize(3));
  }


}
