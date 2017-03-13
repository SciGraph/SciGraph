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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.GenericType;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.scigraph.internal.CypherUtil;
import io.scigraph.internal.GraphApi;
import io.scigraph.vocabulary.Vocabulary;
import org.prefixcommons.CurieUtil;

public class GraphServiceTest {

  private static final Vocabulary vocabulary = mock(Vocabulary.class);
  private static final GraphDatabaseService graphDb = mock(GraphDatabaseService.class);
  private static final GraphApi api = mock(GraphApi.class);
  private static final Transaction tx = mock(Transaction.class);
  private static final CurieUtil curieUtil = mock(CurieUtil.class);
  private static final CypherUtil cypherUtil = mock(CypherUtil.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
      .addResource(new GraphService(vocabulary, graphDb, api, curieUtil, cypherUtil)).build();

  @Before
  public void setup() {
    when(api.getAllPropertyKeys()).thenReturn(newArrayList("foo", "bar"));
    when(api.getAllRelationshipTypes()).thenReturn(
        newArrayList(RelationshipType.withName("foo"), RelationshipType.withName("bar")));
    when(graphDb.beginTx()).thenReturn(tx);
    when(api.getEdges(any(RelationshipType.class), anyBoolean(), anyLong(), anyLong()))
        .thenReturn(new TinkerGraph());
  }

  @Test
  public void smokeConstructor() {
    new GraphService(vocabulary, graphDb, api, curieUtil, cypherUtil);
  }

  @Test
  public void propertyKeys_areSorted() {
    assertThat(resources.client().target("/graph/properties").request()
        .get(new GenericType<List<String>>() {}), contains("bar", "foo"));
    verify(api).getAllPropertyKeys();
  }

  @Test
  public void relationshipTypes_areSorted() {
    assertThat(resources.client().target("/graph/relationship_types").request()
        .get(new GenericType<List<String>>() {}), contains("bar", "foo"));
    verify(api).getAllRelationshipTypes();
  }

  @Test
  public void edges_areReturned() {
    assertThat(resources.client().target("/graph/edges/subClassOf").request().get(String.class),
        StringContains.containsString("\"vertices\":[]"));
  }

}
