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
package edu.sdsc.scigraph.services.resources;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.GenericType;

import io.dropwizard.testing.junit.ResourceTestRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import edu.sdsc.scigraph.internal.GraphApi;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

public class GraphServiceTest {

  private static final Vocabulary vocabulary = mock(Vocabulary.class);
  private static final GraphDatabaseService graphDb = mock(GraphDatabaseService.class);
  private static final GraphApi api = mock(GraphApi.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
  .addResource(new GraphService(vocabulary, graphDb, api)).build();

  @Before
  public void setup() {
    when(api.getAllPropertyKeys()).thenReturn(newArrayList("foo", "bar"));
  }

  @Test
  public void testPrefix() {
    assertThat(
        resources.client().target("/graph/properties").request().get(new GenericType<List<String>>(){}),
        contains("bar", "foo"));
    verify(api).getAllPropertyKeys();
  }

}
