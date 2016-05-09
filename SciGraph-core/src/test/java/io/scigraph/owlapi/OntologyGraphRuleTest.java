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

import static com.google.common.collect.Iterables.size;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class OntologyGraphRuleTest {

  @Rule
  public OntologyGraphRule graphRule = new OntologyGraphRule("ontologies/pizza.owl");

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}

  @Test
  public void ontology_isLoaded() {
    GraphDatabaseService graphDb = graphRule.getGraphDb();
    try (Transaction tx = graphDb.beginTx()) {
      assertThat(size(graphDb.getAllNodes()), is(greaterThan(0)));
      tx.success();
    }
  }

}
