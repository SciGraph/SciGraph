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

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.dozer.DozerBeanMapper;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.prefixcommons.CurieUtil;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.scigraph.frames.Concept;
import io.scigraph.vocabulary.Vocabulary;

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
    hippocampus.getLabels().add("Hippocampus");
    when(curieUtil.getCurie(anyString())).thenReturn(Optional.<String>empty());
  }

  @Test
  public void testKnownIdJson() throws Exception {
    when(vocabulary.getConceptFromId(any(Vocabulary.Query.class))).thenReturn(Optional.of((hippocampus)));
    String response = resources.client().target("/vocabulary/id/foo").request().accept(MediaType.APPLICATION_JSON).get(String.class);
    String expected = fixture("fixtures/hippocampusInList.json");
    assertEquals(expected, response, true);
  }

}
