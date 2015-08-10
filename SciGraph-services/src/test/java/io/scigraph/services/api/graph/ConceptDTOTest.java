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
package io.scigraph.services.api.graph;

import static  io.dropwizard.testing.FixtureHelpers.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;
import io.dropwizard.jackson.Jackson;
import io.scigraph.services.api.graph.ConceptDTO;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConceptDTOTest {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  @Test
  public void emptyListsAreReturned_whenPropertiesAreAbsent() throws Exception {
    ConceptDTO dto = new ConceptDTO();
    dto.setIri("http://example.org/foo");
    String actual = MAPPER.writeValueAsString(dto);
    String expected = fixture("fixtures/simpleConceptDTO.json");
    assertEquals(expected, actual, true);
  }

}
