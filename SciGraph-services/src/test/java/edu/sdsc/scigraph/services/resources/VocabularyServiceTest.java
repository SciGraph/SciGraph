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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.dropwizard.testing.FixtureHelpers;
import io.dropwizard.testing.junit.ResourceTestRule;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.dozer.DozerBeanMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.base.Optional;
import com.sun.jersey.api.client.UniformInterfaceException;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

public class VocabularyServiceTest {

  @SuppressWarnings("unchecked")
  private static final Vocabulary vocabulary = mock(Vocabulary.class);

  private final Concept hippocampus = mock(Concept.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
  .addResource(new VocabularyService(vocabulary, new DozerBeanMapper()))
  .build();

  @Before
  public void setup() {
    when(vocabulary.getConceptFromUri("http://example.org/none")).thenReturn(Optional.<Concept>absent());
    when(vocabulary.getConceptFromUri("http://example.org/foo")).thenReturn(Optional.of(hippocampus));
    when(hippocampus.getLabels()).thenReturn(newArrayList("Hippocampus"));
  }

  @Test(expected=UniformInterfaceException.class)
  public void testUnknownUri() {
    resources.client().resource("/vocabulary/uri/http%3A%2F%2Fexample.org%2Fnone").accept(MediaType.APPLICATION_XML).get(String.class);
  }

  @Test
  public void testKnownUriJson() throws IOException {
    String response = resources.client().resource("/vocabulary/uri/http%3A%2F%2Fexample.org%2Ffoo").accept(MediaType.APPLICATION_JSON).get(String.class);
    assertThat(response, is(equalTo(FixtureHelpers.fixture("fixtures/hippocampus.json"))));
  }
  
  @Test
  public void testKnownIdJson() throws IOException {
    /*when(vocabulary.getConceptFromId(any())).thenReturn(newArrayList(hippocampus));
    String response = resources.client().resource("/vocabulary/id/foo").accept(MediaType.APPLICATION_JSON).get(String.class);
    assertThat(response, is(equalTo(FixtureHelpers.fixture("fixtures/hippocampus.json"))));*/
  }

}
