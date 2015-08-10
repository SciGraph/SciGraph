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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;
import io.scigraph.frames.CommonProperties;
import io.scigraph.services.api.graph.ArrayPropertyTransformer;

import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class ArrayPropertyTransformerTest {

  TinkerGraph graph;
  Vertex v1, v2;
  Edge e;

  @Before
  public void setup() {
    TinkerGraph graph = new TinkerGraph();
    v1 = graph.addVertex(0);
    v2 = graph.addVertex(1);
    v1.setProperty(CommonProperties.IRI, "foo");
    v1.setProperty("foo", "bar");
    e = graph.addEdge(0, v1, v2, "test");
    e.setProperty("foo", 1);
    ArrayPropertyTransformer.transform(graph);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void singleProperties_areConvertedToLists() {
    assertThat((Iterable<String>)v1.getProperty("foo"), contains("bar"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void edgeProperties_areConvertedToIterables() {
    assertThat((Iterable<Integer>)e.getProperty("foo"), contains(1));
  }

  @Test
  public void protectedProperties_stayPrimitive() {
    assertThat((String)v1.getProperty(CommonProperties.IRI), is("foo"));
  }

}
