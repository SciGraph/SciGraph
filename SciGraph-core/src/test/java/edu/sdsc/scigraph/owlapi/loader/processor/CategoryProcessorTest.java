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
package edu.sdsc.scigraph.owlapi.loader.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.neo4j.IdMap;
import edu.sdsc.scigraph.owlapi.OntologyGraphRule;

public class CategoryProcessorTest {

  @ClassRule
  public static OntologyGraphRule graphRule = new OntologyGraphRule("ontologies/fixtures/CategoryProcessor.owl");

  static IdMap idMap;
  static GraphDatabaseService graphDb;
  Transaction tx;

  @BeforeClass
  public static void setup() throws Exception {
    idMap = graphRule.getIdMap();
    graphDb = graphRule.getGraphDb();
    CategoryProcessor processor = new CategoryProcessor(graphRule.getGraphDb());
    processor.setConfiguration("http://example.org/a : \n - foo");
    processor.process();
  }

  @Before
  public void startTx() {
    tx = graphDb.beginTx();
  }

  @After
  public void closeTx() {
    tx.failure();
  }

  Node getNode(String iri) {
    Long id = idMap.get(iri);
    return graphDb.getNodeById(id);
  }
  
  void verifyNode(Node node) {
    assertThat(GraphUtil.getProperty(node, Concept.CATEGORY, String.class), is(Optional.of("foo")));
    assertThat(node.hasLabel(DynamicLabel.label("foo")), is(true));
  }
  
  @Test
  public void parentCategory_isSet() {
    Node node = getNode("http://example.org/a");
    verifyNode(node);
  }

  @Test
  public void childCategory_isSet() {
    Node node = getNode("http://example.org/b");
    verifyNode(node);
  }

  @Test
  public void grandChildCategory_isSet() {
    Node node = getNode("http://example.org/c");
    verifyNode(node);
  }

  @Test
  public void equivalentCategory_isSet() {
    Node node = getNode("http://example.org/e");
    verifyNode(node);
  }

  @Test
  public void equivalentSubclassCategory_isSet() {
    Node node = getNode("http://example.org/f");
    verifyNode(node);
  }

  @Test
  public void instanceCategory_isSet() {
    Node node = getNode("http://example.org/i");
    verifyNode(node);
  }

}
