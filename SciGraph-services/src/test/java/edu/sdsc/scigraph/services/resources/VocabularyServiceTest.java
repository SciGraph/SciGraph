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
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import io.dropwizard.testing.junit.ResourceTestRule;

import javax.ws.rs.core.MediaType;

import org.dozer.DozerBeanMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.owlapi.curies.CurieUtil;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

public class VocabularyServiceTest {

  private static final Vocabulary vocabulary = mock(Vocabulary.class);

  private final Concept hippocampus = new Concept(1L);
  
  private static final CurieUtil curieUtil = mock(CurieUtil.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
  .addResource(new VocabularyService(vocabulary, new DozerBeanMapper(), curieUtil))
  .build();

  @Before
  public void setup() {
    when(vocabulary.getConceptFromUri("http://example.org/none")).thenReturn(Optional.<Concept>absent());
    when(vocabulary.getConceptFromUri("http://example.org/foo")).thenReturn(Optional.of(hippocampus));
    hippocampus.getLabels().add("Hippocampus");
    when(curieUtil.getCurie(anyString())).thenReturn(Optional.<String>absent());
  }

  @Test(expected=Exception.class)
  public void testUnknownUri() {
    resources.client().target("/vocabulary/uri/http%3A%2F%2Fexample.org%2Fnone").request().accept(MediaType.APPLICATION_XML).get(String.class);
  }

  @Test
  public void testKnownUriJson() throws Exception {
    String response = resources.client().target("/vocabulary/uri/http%3A%2F%2Fexample.org%2Ffoo").request().accept(MediaType.APPLICATION_JSON).get(String.class);
    String expected = fixture("fixtures/hippocampus.json");
    assertEquals(expected, response, true);
  }
  
  @Test
  public void testKnownIdJson() throws Exception {
    when(vocabulary.getConceptFromId(any(Vocabulary.Query.class))).thenReturn(newArrayList(hippocampus));
    String response = resources.client().target("/vocabulary/id/foo").request().accept(MediaType.APPLICATION_JSON).get(String.class);
    String expected = fixture("fixtures/hippocampusInList.json");
    assertEquals(expected, response, true);
  }

}
