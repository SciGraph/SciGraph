package edu.sdsc.scigraph.services.refine;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.dropwizard.jackson.Jackson;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestRefineQuery {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  @Test
  public void complexQuery() throws Exception {
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

}
