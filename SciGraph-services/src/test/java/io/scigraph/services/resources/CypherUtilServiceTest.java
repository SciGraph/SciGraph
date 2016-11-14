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
package io.scigraph.services.resources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.dropwizard.testing.junit.ResourceTestRule;
import io.scigraph.internal.CypherUtil;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.guard.Guard;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.test.TestGraphDatabaseFactory;

public class CypherUtilServiceTest {

  private static final CypherUtil cypherUtil = mock(CypherUtil.class);
  private static final GraphDatabaseService graphDb = mock(GraphDatabaseService.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
      .addResource(new CypherUtilService(cypherUtil, graphDb)).build();

  @Before
  public void setup() {
    when(cypherUtil.resolveRelationships("foo!")).thenReturn("foo");
    when(cypherUtil.resolveStartQuery("foo!")).thenReturn("foo!");
  }

  @Test
  public void smokeConstructor() {
    new CypherUtilService(cypherUtil, graphDb);
  }

  @Test
  public void resolveTest() {
    assertThat(
        resources.client().target("/cypher/resolve?cypherQuery=foo!").request().get(String.class),
        equalTo("foo"));
  }

  @Test
  public void guardAvailibityTest() {
    GraphDatabaseAPI db =
        (GraphDatabaseAPI) new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder()
            .setConfig(GraphDatabaseSettings.execution_guard_enabled, Settings.TRUE)
            .newGraphDatabase();
    Guard guard = db.getDependencyResolver().resolveDependency(Guard.class);
    assertNotNull(guard);
  }

}
