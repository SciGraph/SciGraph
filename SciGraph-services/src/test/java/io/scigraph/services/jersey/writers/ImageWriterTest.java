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
package io.scigraph.services.jersey.writers;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import io.scigraph.services.jersey.CustomMediaTypes;
import io.scigraph.services.jersey.writers.ImageWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class ImageWriterTest {

  ImageWriter writer = new ImageWriter();
  TinkerGraph graph = new TinkerGraph();

  @Before
  public void setup() {
    Vertex v = graph.addVertex(0);
    v.setProperty("list", newArrayList("elt1", "elt2"));
    v.setProperty("array", new String[]{"elt1", "elt2"});
    writer.request = mock(HttpServletRequest.class);
  }

  @Test
  public void smoke() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writer.writeTo(graph, Graph.class, null, null, CustomMediaTypes.IMAGE_PNG_TYPE, null, os);
    String image = new String(os.toByteArray(), Charsets.UTF_8);
    assertThat(image, is(notNullValue()));
  }

  @Test
  public void length_isNegOne() {
    assertThat(writer.getSize(null, null, null, null, null), is(-1L));
  }


}
