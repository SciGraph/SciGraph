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
package io.scigraph.owlapi.postprocessors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import io.scigraph.frames.NodeProperties;
import io.scigraph.internal.TinkerGraphUtil;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.util.GraphTestBase;

public class EdgeLabelerTest extends GraphTestBase {

  EdgeLabeler edgeLabeler;

  private final String relationshipType1 = "http://x.org/rel1";
  private final String relationshipType1Label = "relationshipType1Label";
  private final String relationshipType2 = "http://x.org/rel2";
  private final String relationshipType3 = "hasPhenotype";
  Node n1;
  Node n2;

  @Before
  public void setup() {
    n1 = createNode("http://x.org/a");
    n2 = createNode("http://x.org/b");
    Node rel = createNode(relationshipType1);
    rel.setProperty(NodeProperties.LABEL, relationshipType1Label);
    createNode(relationshipType2);

    n1.createRelationshipTo(n2, RelationshipType.withName(relationshipType1));
    n1.createRelationshipTo(n2, RelationshipType.withName(relationshipType2));
    n1.createRelationshipTo(n2, RelationshipType.withName(relationshipType3));

    edgeLabeler = new EdgeLabeler(graphDb);
    edgeLabeler.run();
  }

  @Test
  public void edgeWithLabeledNodeIsTagged() {
    Relationship rel =
        n1.getRelationships(RelationshipType.withName(relationshipType1)).iterator().next();
    assertThat(rel.hasProperty(EdgeLabeler.edgeProperty), is(true));
    assertThat(GraphUtil.getProperty(rel, EdgeLabeler.edgeProperty, String.class).get(),
        is(relationshipType1Label));
  }

  @Test
  public void edgeWithNodeIsTaggedWithType() {
    Relationship rel =
        n1.getRelationships(RelationshipType.withName(relationshipType2)).iterator().next();
    assertThat(rel.hasProperty(EdgeLabeler.edgeProperty), is(true));
    assertThat(GraphUtil.getProperty(rel, EdgeLabeler.edgeProperty, String.class).get(),
        is(relationshipType2));
  }

  @Test
  public void edgeWithoutNodeIsTaggedWithType() {
    Relationship rel =
        n1.getRelationships(RelationshipType.withName(relationshipType3)).iterator().next();
    assertThat(rel.hasProperty(EdgeLabeler.edgeProperty), is(true));
    assertThat(GraphUtil.getProperty(rel, EdgeLabeler.edgeProperty, String.class).get(),
        is(relationshipType3));
  }

  @Test
  public void canBeTransformedToTinkerGraph() {
    TinkerGraphUtil tgu = new TinkerGraphUtil(curieUtil);
    ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
    while (nodes.hasNext()) {
      tgu.addElement(nodes.next());
    }
    ResourceIterator<Relationship> relationships = graphDb.getAllRelationships().iterator();
    while (relationships.hasNext()) {
      tgu.addElement(relationships.next());
    }
  }
}
