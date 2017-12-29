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
package io.scigraph.owlapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.test.TestGraphDatabaseFactory;

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.Concept;
import io.scigraph.neo4j.GraphUtil;
import org.neo4j.test.rule.ImpermanentDatabaseRule;

public class OwlPostprocessorTest {

  @Rule
  public ImpermanentDatabaseRule graphDb = new ImpermanentDatabaseRule();

  //GraphDatabaseService graphDb;
  Node parent, child, grandChild, equivalent, equivalentSubclass, instance;
  OwlPostprocessor postprocessor;

  void enableIndexing() {
    AutoIndexer<Node> nodeIndex = graphDb.index().getNodeAutoIndexer();
    nodeIndex.startAutoIndexingProperty(CommonProperties.IRI);
    nodeIndex.setEnabled(true);
  }

  @Before
  public void setup() throws InterruptedException, ExecutionException {
    //graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    Transaction tx = graphDb.beginTx();
    enableIndexing();
    parent = graphDb.createNode();
    parent.setProperty(CommonProperties.IRI, "http://example.org/a");
    child = graphDb.createNode();
    child.createRelationshipTo(parent, OwlRelationships.RDFS_SUBCLASS_OF);
    grandChild = graphDb.createNode();
    grandChild.createRelationshipTo(child, OwlRelationships.RDFS_SUBCLASS_OF);
    equivalent = graphDb.createNode();
    equivalentSubclass = graphDb.createNode();
    equivalentSubclass.createRelationshipTo(equivalent, OwlRelationships.RDFS_SUBCLASS_OF);
    equivalent.createRelationshipTo(child, OwlRelationships.OWL_EQUIVALENT_CLASS);
    instance = graphDb.createNode();
    instance.createRelationshipTo(grandChild, OwlRelationships.RDF_TYPE);
    tx.success();
    postprocessor = new OwlPostprocessor(graphDb, Collections.<String, String>emptyMap());
    Map<String, String> categoryMap = new HashMap<>();
    categoryMap.put("http://example.org/a", "foo");
    tx.close();
    postprocessor.processCategories(categoryMap);
    tx = graphDb.beginTx();
  }

  @Test
  public void parentCategory_isSet() {
    assertThat(GraphUtil.getProperty(parent, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(parent.hasLabel(Label.label("foo")), is(true));
  }

  @Test
  public void childCategory_isSet() {
    assertThat(GraphUtil.getProperty(child, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(child.hasLabel(Label.label("foo")), is(true));
  }

  @Test
  public void grandChildCategory_isSet() {
    assertThat(GraphUtil.getProperty(grandChild, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(grandChild.hasLabel(Label.label("foo")), is(true));
  }

  @Test
  public void equivalentCategory_isSet() {
    assertThat(GraphUtil.getProperty(equivalent, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(equivalent.hasLabel(Label.label("foo")), is(true));
  }

  @Test
  public void equivalentSubclassCategory_isSet() {
    assertThat(GraphUtil.getProperty(equivalentSubclass, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(equivalentSubclass.hasLabel(Label.label("foo")), is(true));
  }

  @Test
  public void instanceCategory_isSet() {
    assertThat(GraphUtil.getProperty(instance, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(instance.hasLabel(Label.label("foo")), is(true));
  }

}
