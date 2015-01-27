package edu.sdsc.scigraph.neo4j;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import com.google.common.base.Optional;

public abstract class GraphTestBase<T extends GraphInterface> {

  private T graph;

  protected abstract T createInstance() throws Exception;

  static RelationshipType TYPE = DynamicRelationshipType.withName("type");
  static Label LABEL1 = DynamicLabel.label("label1");
  static Label LABEL2 = DynamicLabel.label("label2");

  @Before
  public void setup() throws Exception {
    graph = createInstance();
  }

  @Test
  public void nonExistantNodesAreAbsent() {
    assertThat(graph.getNode("foo"), is(Optional.<Long>absent()));
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
    assertThat(graph.getNodeProperty(node, "bar", String.class), is(Optional.<String>absent()));
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
  public void multiNodeProperty_returnsMultiValuedList() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "baz");
    graph.addNodeProperty(node, "bar", "faz");
    assertThat(graph.getNodeProperties(node, "bar", String.class), contains("baz", "faz"));
  }

  @Test(expected = ClassCastException.class)
  public void nodeProperty_castException() {
    long node = graph.createNode("foo");
    graph.setNodeProperty(node, "bar", "baz");
    graph.getNodeProperty(node, "bar", Integer.class).get();
  }

  @Test(expected = IllegalArgumentException.class)
  public void multipleNodePropertyTypes_throwsException() {
    long node = graph.createNode("foo");
    graph.addNodeProperty(node, "bar", "baz");
    graph.addNodeProperty(node, "bar", 1);
  }

  @Test
  public void absentRelationships_areAbsent() {
    long start = graph.createNode("foo");
    long end = graph.createNode("bar");
    assertThat(graph.getRelationship(start, end, TYPE), is(Optional.<Long>absent()));
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
    assertThat(graph.getNodeProperty(node, "bar", String.class), is(Optional.<String>absent()));
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
  public void multiRelationshipProperty_returnsMultiValuedList() {
    long node = graph.createNode("foo");
    long relationship = graph.createRelationship(node, node, TYPE);
    graph.setRelationshipProperty(relationship, "bar", "baz");
    graph.addRelationshipProperty(relationship, "bar", "faz");
    assertThat(graph.getRelationshipProperties(relationship, "bar", String.class), contains("baz", "faz"));
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

}
