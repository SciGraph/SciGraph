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
import io.scigraph.frames.CommonProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.ext.MessageBodyWriter;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public abstract class GraphWriterTestBase {

  TinkerGraph graph = new TinkerGraph();
  MessageBodyWriter<Graph> writer = getWriter();

  @Before
  public void setup() {
    Vertex v = graph.addVertex(0);
    Vertex w = graph.addVertex(1);
    v.setProperty(CommonProperties.IRI, "http://x.org/v");
    v.setProperty("list", newArrayList("elt1", "elt2"));
    v.setProperty("array", new String[]{"elt1", "elt2"});
    w.setProperty(CommonProperties.IRI, "http://x.org/w");
    v.addEdge("edge", w);
  }

  abstract MessageBodyWriter<Graph> getWriter();

  @Test
  public void write_doesntOutputNull() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writer.writeTo(graph, Graph.class, null, null, null, null, os);
    String output = new String(os.toByteArray(), Charsets.UTF_8);
    assertThat(output, is(notNullValue()));
  }

  @Test
  public void length_isNegOne() {
    assertThat(writer.getSize(null, null, null, null, null), is(-1L));
  }

  @Test
  public void assignable_fromGraph() {
    assertThat(writer.isWriteable(TinkerGraph.class, null, null, null), is(true));
    assertThat(writer.isWriteable(String.class, null, null, null), is(false));
  }

}
