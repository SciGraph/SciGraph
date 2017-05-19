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
package io.scigraph.neo4j;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

public abstract class GraphTestBase<T extends Graph> {

  private T graph;

  protected abstract T createInstance() throws Exception;

  static RelationshipType TYPE = RelationshipType.withName("type");
  static Label LABEL1 = Label.label("label1");
  static Label LABEL2 = Label.label("label2");

  @Before
  public void setup() throws Exception {
    graph = createInstance();
  }

  @Test
  public void nonExistantNodesAreAbsent() {
    assertThat(graph.getNode("foo"), is(Optional.<Long>empty()));
  }

  @Test
  public void nodesArePresent_afterCreation() {
    long nodeId = graph.createNode("foo");
    assertThat(graph.getNode("foo").get(), is(nodeId));
  }

  @Test
  public void nodeCreationIsIdempotent() {
    long firstId = graph.createNode("foo");
    long secondId = graph.createNode("foo");
    assertThat(firstId, is(secondId));
  }

  @Test
  public void absentNodesProperties_areAbsent() {
    long node = graph.createNode("foo");
    assertThat(graph.getNodeProperty(node, "bar", String.class), is(Optional.<String>empty()));
  }

  @Test
  public void singleNodePropertyIsSet() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "baz");
    assertThat(graph.getNodeProperty(node, "bar", String.class), is(Optional.of("baz")));
  }

  @Test
  public void nodePropertyIsReset() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "faz");
    graph.setNodeProperty(node, "bar", "baz");
    assertThat(graph.getNodeProperty(node, "bar", String.class), is(Optional.of("baz")));
  }

  @Test
  public void nonexistantNodeProperty_returnsEmptyList() {
    long node = graph.createNode("foo");
    assertThat(graph.getNodeProperties(node, "bar", String.class), is(empty()));
  }

  @Test
  public void singleNodeProperty_returnsSingleValuedList() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "baz");
    assertThat(graph.getNodeProperties(node, "bar", String.class), contains("baz"));
  }

  @Test
  public void multiNodeProperty_returnsMultiValuedCollection() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "baz");
    graph.addNodeProperty(node, "bar", "faz");
    assertThat(graph.getNodeProperties(node, "bar", String.class), containsInAnyOrder("baz", "faz"));
  }

  @Test(expected = ClassCastException.class)
  public void nodeProperty_castException() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "baz");
    @SuppressWarnings("unused")
    Integer unused = graph.getNodeProperty(node, "bar", Integer.class).get();
  }

  @Test
  public void multipleNodePropertyTypes_convertToString() {
    long node = graph.createNode("foo");
    graph.addNodeProperty(node, "bar", "baz");
    graph.addNodeProperty(node, "bar", 1);
    assertThat(graph.getNodeProperties(node, "bar", String.class), containsInAnyOrder("baz", "1"));
  }

  @Test
  public void absentRelationships_areAbsent() {
    long start = graph.createNode("foo");
    long end = graph.createNode("bar");
    assertThat(graph.getRelationship(start, end, TYPE), is(Optional.<Long>empty()));
  }

  @Test
  public void relationships_areCreated() {
    long start = graph.createNode("foo");
    long end = graph.createNode("bar");
    assertThat(graph.getRelationship(start, end, TYPE).isPresent(), is(false));
    long relationship = graph.createRelationship(start, end, TYPE);
    assertThat(graph.getRelationship(start, end, TYPE), is(Optional.of(relationship)));
  }

  @Test
  public void relationshipCreation_isIdempotent() {
    long start = graph.createNode("foo");
    long end = graph.createNode("bar");
    long relationship1 = graph.createRelationship(start, end, TYPE);
    long relationship2 = graph.createRelationship(start, end, TYPE);
    assertThat(relationship1, is(relationship2));
  }

  @Test
  public void absentRelationshipProperties_areAbsent() {
    long node = graph.createNode("foo");
    assertThat(graph.getNodeProperty(node, "bar", String.class), is(Optional.<String>empty()));
  }

  @Test
  public void singleRelationshipPropertyIsSet() {
    long node = graph.createNode("foo");
    long relationship = graph.createRelationship(node, node, TYPE);
    graph.setRelationshipProperty(relationship, "bar", "baz");
    assertThat(graph.getRelationshipProperty(relationship, "bar", String.class), is(Optional.of("baz")));
  }

  @Test
  public void relationshipPropertyIsReset() {
    long node = graph.createNode("foo");
    long relationship = graph.createRelationship(node, node, TYPE);
    graph.setRelationshipProperty(relationship, "bar", "faz");
    graph.setRelationshipProperty(relationship, "bar", "baz");
    assertThat(graph.getRelationshipProperty(relationship, "bar", String.class), is(Optional.of("baz")));
  }

  @Test
  public void nonexistantRelationshipProperty_returnsEmptyList() {
    long node = graph.createNode("foo");
    long relationship = graph.createRelationship(node, node, TYPE);
    assertThat(graph.getRelationshipProperties(relationship, "bar", String.class), is(empty()));
  }

  @Test
  public void singleRelationshipProperty_returnsSingleValuedList() {
    long node = graph.createNode("foo");
    long relationship = graph.createRelationship(node, node, TYPE);
    graph.setRelationshipProperty(relationship, "bar", "baz");
    assertThat(graph.getRelationshipProperties(relationship, "bar", String.class), contains("baz"));
  }

  @Test
  public void multiRelationshipProperty_returnsMultiValuedCollection() {
    long node = graph.createNode("foo");
    long relationship = graph.createRelationship(node, node, TYPE);
    graph.setRelationshipProperty(relationship, "bar", "baz");
    graph.addRelationshipProperty(relationship, "bar", "faz");
    assertThat(graph.getRelationshipProperties(relationship, "bar", String.class), containsInAnyOrder("baz", "faz"));
  }

  @Test
  public void noLabels_returnsEmptyList() {
    long node = graph.createNode("foo");
    assertThat(graph.getLabels(node), is(empty()));
  }

  @Test
  public void labels_areSet() {
    long node = graph.createNode("foo");
    graph.setLabel(node, LABEL1);
    assertThat(graph.getLabels(node), contains(LABEL1));
  }

  @Test
  public void labels_areReset() {
    long node = graph.createNode("foo");
    graph.setLabel(node, LABEL1);
    graph.setLabel(node, LABEL2);
    assertThat(graph.getLabels(node), contains(LABEL2));
  }

  @Test
  public void labels_areMultivalued() {
    long node = graph.createNode("foo");
    graph.addLabel(node, LABEL1);
    graph.addLabel(node, LABEL2);
    assertThat(graph.getLabels(node), containsInAnyOrder(LABEL1, LABEL2));
  }
  
  @Test
  public void labelsAreAdded_afterBeingSet() {
    long node = graph.createNode("foo");
    graph.setLabel(node, LABEL1);
    graph.addLabel(node, LABEL2);
    assertThat(graph.getLabels(node), containsInAnyOrder(LABEL1, LABEL2));
  }

  @Test
  public void pairwiseRelationshipsAreCreated() {
    long a = graph.createNode("a");
    long b = graph.createNode("b");
    long c = graph.createNode("c");
    graph.createRelationshipsPairwise(newHashSet(a, b, c), TYPE);
    assertThat(graph.getRelationship(a, b, TYPE).isPresent(), is(true));
    assertThat(graph.getRelationship(b, a, TYPE).isPresent(), is(false));
    assertThat(graph.getRelationship(a, c, TYPE).isPresent(), is(true));
    assertThat(graph.getRelationship(c, a, TYPE).isPresent(), is(false));
    assertThat(graph.getRelationship(b, c, TYPE).isPresent(), is(true));
    assertThat(graph.getRelationship(c, b, TYPE).isPresent(), is(false));
  }

  @Test
  public void nonUsableNodeProperties_areIgnored() {
    long a = graph.createNode("a");
    graph.setNodeProperty(a, "foo", "the");
    assertThat(graph.getNodeProperty(a, "foo", String.class).isPresent(), is(false));
  }

  @Test
  public void nonUsableAddedNodeProperties_areIgnored() {
    long a = graph.createNode("a");
    graph.addNodeProperty(a, "foo", "the");
    assertThat(graph.getNodeProperty(a, "foo", String.class).isPresent(), is(false));
  }

  @Test
  public void nonUsableRelationshipProperties_areIgnored() {
    long a = graph.createNode("a");
    long relationship = graph.createRelationship(a, a, TYPE);
    graph.setRelationshipProperty(relationship, "foo", "the");
    assertThat(graph.getRelationshipProperty(relationship, "foo", String.class).isPresent(), is(false));
  }

  @Test
  public void nonUsableAddedRelationshipProperties_areIgnored() {
    long a = graph.createNode("a");
    long relationship = graph.createRelationship(a, a, TYPE);
    graph.addRelationshipProperty(relationship, "foo", "the");
    assertThat(graph.getRelationshipProperty(relationship, "foo", String.class).isPresent(), is(false));
  }

}
