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
package edu.sdsc.scigraph.frames;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.Property;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.util.FramedMultivalueGraphFactory;

public class MultiPropertyMethodHandlerTest {

  FramedGraph<Graph> framedGraph;
  Neo4jGraph graph;
  TestNode node;

  static interface TestNode extends CommonProperties {
    @Property("string")
    public void addString(String string);

    @Property("string")
    public void addStrings(Collection<String> string);

    @Property("string")
    public void setStrings(Collection<String> string);

    @Property("string")
    public void setString(String string);

    @Property("string")
    public Iterable<String> getStrings();

    @Property("string")
    public boolean hasStrings();

    @Property("string")
    public String getString();

    @Property("string")
    public void removeString();

    @Property("numbers")
    public void addNumber(int number);

    @Property("numbers")
    public Iterable<Integer> getNumbers();

    @Property("numbers")
    public Iterable<Boolean> getNumbersAsBooleans();

    @Property("numbers")
    public String getNumbersAsString();

    @Property("boolean")
    public void setBoolean(boolean bool);

    @Property("boolean")
    public boolean isBoolean();
  }

  @Before
  public void setUp() throws Exception {
    GraphDatabaseService graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    graph = new Neo4jGraph(graphDb);
    framedGraph = new FramedMultivalueGraphFactory().create((Graph)graph);
    node = framedGraph.addVertex(1, TestNode.class);
  }

  @After
  public void tearDown() throws Exception {
    graph = null;
    framedGraph = null; 
    node = null;
  }

  @Test
  public void testInitialState() {
    assertThat("No properties should be set", node.getString(), is(nullValue()));
    assertThat("No properties should be set", node.getStrings(), is(emptyIterable()));
  }

  @Test
  public void testAddSingleProperty() {
    node.addString("a");
    assertThat(node.getStrings(), contains("a"));
  }

  @Test
  public void testPrimitiveGetMethod() {
    node.addString("a");
    assertThat(node.getString(), is(equalTo("a")));
  }

  @Test
  public void testAddCollection() {
    node.addStrings(newArrayList("a", "b"));
    assertThat(node.getStrings(), contains("a", "b"));
  }

  @Test
  public void testAddDuplicateCollection() {
    node.addStrings(newArrayList("a", "a"));
    assertThat(node.getStrings(), contains("a"));
  }

  @Test
  public void testAddMultipleProperties() {
    node.addString("a");
    node.addString("b");
    assertThat(node.getStrings(), contains("a", "b"));
  }

  @Test
  public void testAddDuplicateProperties() {
    node.addString("a");
    node.addString("a");
    assertThat(node.getStrings(), contains("a"));
  }

  @Test
  public void testSetCollection() {
    node.setString("a");
    node.setStrings(newArrayList("a", "b"));
    assertThat(node.getStrings(), contains("a", "b"));
  }

  @Test
  public void testResetToSingleProperty() {
    node.addString("a");
    node.addString("b");
    node.setString("a");
    assertThat(node.getStrings(), contains("a"));
  }

  @Test
  public void testNonStringType() {
    node.addNumber(1);
    node.addNumber(2);
    assertThat(node.getNumbers(), contains(1, 2));
  }

  @Test(expected=IllegalStateException.class)
  public void testSingleGetMethodOnMultivaluedProperty() {
    node.addString("a");
    node.addString("b");
    node.getString();
  }

  @Test
  public void testRemove() {
    node.addString("a");
    node.removeString();
    assertThat(node.getString(), is(nullValue()));
    assertThat(node.getStrings(), is(emptyIterable()));
  }

  @Test(expected=ClassCastException.class)
  public void testIncompatibleTypes() {
    node.addNumber(1);
    node.getNumbersAsString();
  }

  @Test(expected=ClassCastException.class)
  public void testIncompatibleTypesCollections() {
    node.addNumber(1);
    Iterable<Boolean> bools = node.getNumbersAsBooleans();
    bools.iterator().next().booleanValue();
  }

  @Test(expected=NullPointerException.class)
  public void testAddNull() {
    node.addString(null);
  }

  @Test
  public void testHasMethod() {
    assertThat("Node should not have any strings", node.hasStrings(), is(false));
    node.addString("a");
    assertThat("Node should now have strings", node.hasStrings(), is(true));
  }

  @Test
  public void testIsMethod() {
    node.setBoolean(true);
    assertThat(node.isBoolean(), is(true));
    node.setBoolean(false);
    assertThat(node.isBoolean(), is(false));
  }

}
