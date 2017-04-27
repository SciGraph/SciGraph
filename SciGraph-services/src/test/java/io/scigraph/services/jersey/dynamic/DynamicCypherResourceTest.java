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
package io.scigraph.services.jersey.dynamic;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.swagger.models.Path;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.Before;
import org.junit.Test;

public class DynamicCypherResourceTest {

  Path path = new Path();
  CypherInflectorFactory factory = mock(CypherInflectorFactory.class);

  @Before
  public void setup() {
    CypherInflector inflector = new CypherInflector(null, null, null, "foo", null, null);
    when(factory.create(eq("foo"), any(Path.class))).thenReturn(inflector);
  }

  @Test
  public void pathIsCorrectlySet() {
    DynamicCypherResource resource = new DynamicCypherResource(factory, "foo", path);
    assertThat(resource.getBuilder().build().getPath(), is("foo"));
  }

  @Test
  public void resourceMethodsAreAdded() {
    DynamicCypherResource resource = new DynamicCypherResource(factory, "foo", path);
    ResourceMethod method = getOnlyElement(resource.getBuilder().build().getResourceMethods());
    assertThat(method.getHttpMethod(), is("GET"));
  }

}
