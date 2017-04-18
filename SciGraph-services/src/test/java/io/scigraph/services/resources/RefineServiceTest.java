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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.util.Optional;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Charsets;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.scigraph.frames.Concept;
import io.scigraph.services.refine.RefineResults;
import io.scigraph.services.refine.ServiceMetadata;
import io.scigraph.vocabulary.Vocabulary;
import io.scigraph.vocabulary.Vocabulary.Query;

public class RefineServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final Vocabulary vocabulary = mock(Vocabulary.class);
  private static final ServiceMetadata metadata = new ServiceMetadata();

  static {
    metadata.setName("name");
  }

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
  .addResource(new RefineService(vocabulary, metadata))
  .build();

  @Test
  public void smokeConstructor() {
    new RefineService(vocabulary, metadata);
  }

  @Test
  public void metadataIsReturned_whenNoQueriesArePresent() {
    assertThat(
        resources.client().target("/refine/reconcile").request().get(ServiceMetadata.class), instanceOf(ServiceMetadata.class));
  }

  @Test
  public void resultsAreReturned_whenQueryIsPresent() {
    assertThat(
        resources.client().target("/refine/reconcile?query=hippocampus").request().get(RefineResults.class), instanceOf(RefineResults.class));
  }

  @Test
  public void resultsAreReturned_whenMultipleQueriesArePresent() throws Exception {
    String query = URLEncoder.encode("{ \"q0\" : { \"query\" : \"foo\" }, \"q1\" : { \"query\" : \"bar\" } }", Charsets.UTF_8.name());
    assertThat(
        resources.client().target("/refine/reconcile?queries=" + query).request().get(String.class), is("{\"q1\":{\"result\":[]},\"q0\":{\"result\":[]}}"));
  }

  @Test()
  public void exceptionIsThrown_whenJsonIsMalformed() {
    exception.expect(Exception.class);
    resources.client().target("/refine/reconcile?query=%5Bbad%20json%5D%5D").request().get(RefineResults.class);
  }

  @Test
  public void preview_returns404_whenNotFound() {
    when(vocabulary.getConceptFromId(any(Query.class))).thenReturn(Optional.<Concept>empty());
    assertThat(
        resources.client().target("/refine/preview/foo").request().get().getStatus(), is(404));
  }

}
