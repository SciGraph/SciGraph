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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.test.rule.ImpermanentDatabaseRule;

public class GraphUtilTest {

  @Rule
  public ImpermanentDatabaseRule graphDb = new ImpermanentDatabaseRule();

  Node node;

  @Before
  public void setup() {
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

  @Test
  public void testAddMultipleTypedValues() {
    GraphUtil.addProperty(node, "p", 1);
    GraphUtil.addProperty(node, "p", "bad");
    assertThat((String[]) node.getProperty("p"), is(new String[] { "1", "bad" }));
  }

  @Test
  public void testGetProperty() {
    assertThat(GraphUtil.getProperty(node, "foo", String.class), is(Optional.of("bar")));
    assertThat(GraphUtil.getProperty(node, "fizz", Integer.class), is(Optional.of(1)));
  }

  @Test
  public void testUnknownProperty() {
    assertThat(GraphUtil.getProperty(node, "nothing", String.class), is(Optional.<String>empty()));
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
    assertThat(GraphUtil.getProperties(node, "p", String.class), containsInAnyOrder("a", "b"));
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

  @Test
  public void newProperty_IsAtomicValue() {
    assertThat((String)GraphUtil.getNewPropertyValue(null, "1"), is("1"));
  }

  @Test
  public void newProperty_IsMultiValuedArray() {
    assertThat((String[])GraphUtil.getNewPropertyValue("1", "2"), is(new String[]{"1", "2"}));
  }

  @Test
  public void newProperty_IsMultiValuedArray_whenStartingFromAnArray() {
    assertThat((String[])GraphUtil.getNewPropertyValue(new String[]{"1", "2"}, "3"), is(new String[]{"1", "2", "3"}));
  }

  @Test
  public void newProperties_useSetSemantics_fromSingleValues() {
    assertThat((String)GraphUtil.getNewPropertyValue("2", "2"), is("2"));
  }

  @Test
  public void newProperties_useSetSemantics_fromMultipleValues() {
    assertThat((String[])GraphUtil.getNewPropertyValue(new String[]{"1", "2"}, "2"), is(new String[]{"1", "2"}));
  }

  @Test
  public void heterogeneousValues_transformToStrings() {
    assertThat((String[])GraphUtil.getNewPropertyValue(1, "2"), is(new Object[]{"1", "2"}));
  }

  @Test
  public void multipleHeterogeneousValues_transformToStrings() {
    assertThat((String[])GraphUtil.getNewPropertyValue(new int[]{1, 2}, "3"), is(new String[]{"1", "2", "3"}));
  }

}
