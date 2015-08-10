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

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.dropwizard.jackson.Jackson;
import io.scigraph.services.refine.RefineQueries;
import io.scigraph.services.refine.RefineQuery;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestRefineQuery {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  @Test
  public void deserializationIsCorrect_withAllRefineFields() throws Exception {
    RefineQuery expected = new RefineQuery();
    expected.setQuery("foo");
    expected.setLimit(10);
    expected.setType("type");
    expected.setType_strict("any");
    Map<String, Object> properties = new HashMap<>();
    properties.put("bar", "baz");
    expected.setProperties(properties);

    RefineQuery actual = MAPPER.readValue(fixture("fixtures/refineQuery.json"), RefineQuery.class);
    assertThat(actual, is(expected));
  }

  @Test
  public void deserializationIsCorrect_withOptionalFields() throws Exception {
    RefineQuery expected = new RefineQuery();
    expected.setQuery("foo"); 
    RefineQuery actual = MAPPER.readValue(fixture("fixtures/refineQueryOptionalFields.json"), RefineQuery.class);
    assertThat(actual, is(expected));
  }
  
  @Test
  public void deserializationIsCorrect_withMultipleRefineQueries() throws Exception {
    RefineQuery foo = new RefineQuery();
    foo.setQuery("foo");
    RefineQuery bar = new RefineQuery();
    bar.setQuery("bar");
    RefineQueries expected = new RefineQueries();
    expected.put("q0", foo);
    expected.put("q1", bar);
    
    RefineQueries actual = MAPPER.readValue(fixture("fixtures/refineQueries.json"), RefineQueries.class);
    assertThat(actual, is(expected));
  }

}
