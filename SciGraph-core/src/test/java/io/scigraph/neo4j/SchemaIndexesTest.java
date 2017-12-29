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

import static com.google.common.collect.Iterables.size;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.rule.ImpermanentDatabaseRule;

public class SchemaIndexesTest {

  @Rule
  public ImpermanentDatabaseRule graphDb = new ImpermanentDatabaseRule();

  @Rule
  public TemporaryFolder graphPath = new TemporaryFolder();

  @Test
  public void supportEmptySchemaDefinition() {
    Neo4jConfiguration configuration = new Neo4jConfiguration();
    configuration.setLocation(graphPath.getRoot().getAbsolutePath());
    Neo4jModule.setupSchemaIndexes(graphDb, configuration);

    try (Transaction tx = graphDb.beginTx()) {
      assertThat(graphDb.schema().getIndexes().iterator().hasNext(), is(false));
      tx.success();
      tx.close();
    }
  }

  @Test
  public void schemaOnSingleProperty() {
    Neo4jConfiguration configuration = new Neo4jConfiguration();
    configuration.setLocation(graphPath.getRoot().getAbsolutePath());
    Set<String> property = new HashSet<String>();
    property.add("property");
    Map<String, Set<String>> schemaIndexes = new HashMap<String, Set<String>>();
    schemaIndexes.put("label", property);
    configuration.setSchemaIndexes(schemaIndexes);

    Neo4jModule.setupSchemaIndexes(graphDb, configuration);

    try (Transaction tx = graphDb.beginTx()) {
      assertThat(size(graphDb.schema().getIndexes()), is(1));
      tx.success();
      tx.close();
    }
  }

  @Test
  public void schemaOnMultipleProperties() {
    Neo4jConfiguration configuration = new Neo4jConfiguration();
    configuration.setLocation(graphPath.getRoot().getAbsolutePath());
    Set<String> property = new HashSet<String>();
    property.add("property");
    property.add("property2");
    Map<String, Set<String>> schemaIndexes = new HashMap<String, Set<String>>();
    schemaIndexes.put("label", property);
    configuration.setSchemaIndexes(schemaIndexes);

    Neo4jModule.setupSchemaIndexes(graphDb, configuration);

    try (Transaction tx = graphDb.beginTx()) {
      assertThat(size(graphDb.schema().getIndexes()), is(2));
      tx.success();
      tx.close();
    }
  }

  @Test
  public void schemaOnMultipleLabelsAndMultipleSingleProperties() {
    Neo4jConfiguration configuration = new Neo4jConfiguration();
    configuration.setLocation(graphPath.getRoot().getAbsolutePath());
    Set<String> property = new HashSet<String>();
    property.add("property11");
    property.add("property12");
    Map<String, Set<String>> schemaIndexes = new HashMap<String, Set<String>>();
    schemaIndexes.put("label", property);

    Set<String> property2 = new HashSet<String>();
    property2.add("property21");
    property2.add("property22");
    schemaIndexes.put("label2", property2);

    configuration.setSchemaIndexes(schemaIndexes);

    Neo4jModule.setupSchemaIndexes(graphDb, configuration);

    try (Transaction tx = graphDb.beginTx()) {
      assertThat(size(graphDb.schema().getIndexes()), is(4));
      tx.success();
      tx.close();
    }
  }
}
