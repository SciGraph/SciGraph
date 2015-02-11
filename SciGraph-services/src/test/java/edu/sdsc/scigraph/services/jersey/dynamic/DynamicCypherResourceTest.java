package edu.sdsc.scigraph.services.jersey.dynamic;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.Before;
import org.junit.Test;

public class DynamicCypherResourceTest {

  CypherResourceConfig config = new CypherResourceConfig();
  CypherInflectorFactory factory = mock(CypherInflectorFactory.class);

  @Before
  public void setup() {
    CypherInflector inflector = new CypherInflector(null, null, null);
    when(factory.create(any(CypherResourceConfig.class))).thenReturn(inflector);
  }

  @Test
  public void pathIsCorrectlySet() {
    config.setPath("foo");
    DynamicCypherResource resource = new DynamicCypherResource(factory, config);
    assertThat(resource.getBuilder().build().getPath(), is("foo"));
  }

  @Test
  public void resourceMethodsAreAdded() {
    DynamicCypherResource resource = new DynamicCypherResource(factory, config);
    ResourceMethod method = getOnlyElement(resource.getBuilder().build().getResourceMethods());
    assertThat(method.getHttpMethod(), is("GET"));
  }

}
