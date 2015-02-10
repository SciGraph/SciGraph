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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import io.dropwizard.testing.junit.ResourceTestRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import edu.sdsc.scigraph.services.refine.RefineResults;
import edu.sdsc.scigraph.services.refine.ServiceMetadata;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

public class RefineServiceTest {

  private static final Vocabulary vocabulary = mock(Vocabulary.class);
  private static final ServiceMetadata metadata = new ServiceMetadata();

  static {
    metadata.setName("name");
  }

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
  .addResource(new RefineService(vocabulary, metadata))
  .build();

  @Before
  public void setup() {
  }

  @Test
  public void metadataIsReturned_whenNoQueriesArePresent() {
    assertThat(
        resources.client().target("/refine/reconcile").request().get(ServiceMetadata.class), instanceOf(ServiceMetadata.class));
  }

  @Test
  public void resultsAreReturned_whenQueriesArePresent() {
    assertThat(
        resources.client().target("/refine/reconcile?query=hippocampus").request().get(RefineResults.class), instanceOf(RefineResults.class));
  }

  @Test(expected=Exception.class)
  public void exceptionIsThrown_whenJsonIsMalformed() {
      resources.client().target("/refine/reconcile?query=%5Bbad%20json%5D%5D").request().get(RefineResults.class);
  }

}
