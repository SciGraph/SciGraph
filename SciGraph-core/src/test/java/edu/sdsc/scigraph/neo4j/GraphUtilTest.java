package edu.sdsc.scigraph.neo4j;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.common.base.Optional;

public class GraphUtilTest {

  GraphDatabaseService graphDb;
  Node node;

  @Before
  public void setup() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    graphDb.beginTx();
    node = graphDb.createNode();
    node.setProperty("foo", "bar");
    node.setProperty("fizz", 1);
  }

  @Test
  public void testAddValue() {
    GraphUtil.addProperty(node, "p", 1);
    assertThat((Integer) node.getProperty("p"), is(1));
  }

  @Test
  public void testAddMultipleValues() {
    GraphUtil.addProperty(node, "p", 1);
    GraphUtil.addProperty(node, "p", 2);
    assertThat((int[]) node.getProperty("p"), is(new int[] { 1, 2 }));
  }

  @Test(expected = ClassCastException.class)
  public void testAddMultipleTypedValues() {
    GraphUtil.addProperty(node, "p", 1);
    GraphUtil.addProperty(node, "p", "bad");
  }

  @Test
  public void testGetProperty() {
    assertThat(GraphUtil.getProperty(node, "foo", String.class), is(Optional.of("bar")));
    assertThat(GraphUtil.getProperty(node, "fizz", Integer.class), is(Optional.of(1)));
  }

  @Test
  public void testUnknownProperty() {
    assertThat(GraphUtil.getProperty(node, "nothing", String.class), is(Optional.<String> absent()));
  }

  @Test(expected = ClassCastException.class)
  public void testGetWrongPropertyType() {
    GraphUtil.getProperty(node, "foo", Float.class);
  }

  @Test
  public void testGetEmptyProperties() {
    assertThat(GraphUtil.getProperties(node, "nothing", String.class), is(empty()));
  }

  @Test
  public void testGetSingleProperties() {
    GraphUtil.addProperty(node, "p", "a");
    assertThat(GraphUtil.getProperties(node, "p", String.class), contains("a"));
  }

  @Test
  public void testGetSingleNonStringProperties() {
    GraphUtil.addProperty(node, "p", 1L);
    assertThat(GraphUtil.getProperties(node, "p", Long.class), contains(1L));
  }

  @Test
  public void testGetProperties() {
    GraphUtil.addProperty(node, "p", "a");
    GraphUtil.addProperty(node, "p", "b");
    assertThat(GraphUtil.getProperties(node, "p", String.class), contains("a", "b"));
  }

  @Test
  public void testGetNonStringProperties() {
    GraphUtil.addProperty(node, "p", 1);
    GraphUtil.addProperty(node, "p", 2);
    assertThat(GraphUtil.getProperties(node, "p", Integer.class), contains(1, 2));
  }

  @Test(expected = ClassCastException.class)
  public void testGetWrongPropertiesType() {
    GraphUtil.addProperty(node, "p", 1);
    GraphUtil.getProperties(node, "p", Float.class);
  }

}
