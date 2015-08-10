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
package io.scigraph.services.health;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.scigraph.services.health.Neo4jHealthCheck;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

public class Neo4jHealthCheckTest {

  GraphDatabaseService graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
  Neo4jHealthCheck healthCheck = new Neo4jHealthCheck(graphDb);

  @Test
  public void whenNoNodes_unhealty() throws Exception {
    assertThat(healthCheck.check().isHealthy(), is(false));
  }

  @Test
  public void whenNodes_healty() throws Exception {
    try (Transaction tx = graphDb.beginTx()) {
      graphDb.createNode();
      tx.success();
    }
    assertThat(healthCheck.check().isHealthy(), is(true));
  }

}
