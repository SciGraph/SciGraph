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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.security.WriteOperationsNotAllowedException;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Neo4jModuleTest {

  @Rule
  public TemporaryFolder graphPath = new TemporaryFolder();

  Injector injector;
  Injector injectorReadOnly;

  @Before
  public void setupModule() {
    Neo4jConfiguration configuration = new Neo4jConfiguration();
    configuration.setLocation(graphPath.getRoot().getAbsolutePath());

    injector = Guice.createInjector(new Neo4jModule(configuration));
    injectorReadOnly = Guice.createInjector(new Neo4jModule(configuration, true, true));
  }

  @Test
  public void graphDb_isSingleton() {
    assertThat(injector.getInstance(GraphDatabaseService.class),
        is(injector.getInstance(GraphDatabaseService.class)));
  }

  @Test(expected = WriteOperationsNotAllowedException.class)
  public void graphDbReadOnlyWithApi() {
    GraphDatabaseService graphDb = injectorReadOnly.getInstance(GraphDatabaseService.class);
    Transaction tx = graphDb.beginTx();
    try {
      graphDb.createNode(Label.label("test"));
    } finally {
      tx.close();
    }
  }
  @Test(expected = WriteOperationsNotAllowedException.class)
  public void graphDbReadOnlyWithCypher() {
    GraphDatabaseService graphDb = injectorReadOnly.getInstance(GraphDatabaseService.class);
    Transaction tx = graphDb.beginTx();
    try {
      graphDb.execute("CREATE (n: test)");
    } finally {
      tx.close();
    }
  }
}
