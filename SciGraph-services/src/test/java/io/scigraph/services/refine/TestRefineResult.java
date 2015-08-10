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
package io.scigraph.services.refine;

import static com.google.common.collect.Sets.newHashSet;
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import io.dropwizard.jackson.Jackson;
import io.scigraph.services.refine.RefineResult;
import io.scigraph.services.refine.RefineResults;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestRefineResult {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();
  RefineResult result1, result2;

  @Before
  public void setup() {
    result1 = new RefineResult();
    result1.setId("id");
    result1.setName("name");
    result1.setScore(10.8);
    result1.setMatch(true);
    result1.setType(newHashSet("type"));
    
    result2 = new RefineResult();
    result2.setId("id2");
    result2.setName("name2");
    result2.setScore(10.6);
    result2.setMatch(false);
    result2.setType(newHashSet("type"));
  }

  @Test
  public void serializationIsCorrect_withAllResultFields() throws Exception {
    RefineResults map = new RefineResults();
    map.addResult(result1);
    map.addResult(result2);
    String actual = MAPPER.writeValueAsString(map);
    String expected = fixture("fixtures/refineResult.json");
    assertEquals(expected, actual, true);
  }

  @Test
  public void deserializationIsCorrect_withMultipleRefineQueries() throws Exception {
    Map<String, RefineResults> resultMap = new HashMap<>();
    RefineResults map = new RefineResults();
    map.addResult(result1);
    resultMap.put("q0", map);
    map = new RefineResults();
    map.addResult(result2);
    resultMap.put("q1", map);

    String actual = MAPPER.writeValueAsString(resultMap);
    String expected = fixture("fixtures/refineResults.json");
    assertEquals(expected, actual, true);
  }

}
