/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
package edu.sdsc.scigraph.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.internal.GraphApi;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiCategoryTest extends GraphTestBase {

  GraphApi graphApi;

  static String BASE_URI = "http://example.org/";

  static String uri = BASE_URI + "#fizz";
  static String uri2 = BASE_URI + "#fuzz";
  static String uri3 = BASE_URI + "#fazz";
  Node a;
  Node b;
  Node c;

  @Before
  public void addNodes() throws Exception {
    Graph<Concept> graph = new Graph<Concept>(graphDb, Concept.class);
    a = graph.getOrCreateNode(uri);
    graph.setProperty(a, NodeProperties.TYPE, "OWLClass");
    b = graph.getOrCreateNode(uri2);
    graph.setProperty(b, NodeProperties.TYPE, "OWLClass");
    c = graph.getOrCreateNode(uri3);
    graph.setProperty(c, NodeProperties.TYPE, "OWLClass");
    graph.getOrCreateRelationship(a, b, EdgeType.SUBCLASS_OF);
    this.graphApi = new GraphApi(graph);
  }

  @Test
  public void testFoundClass() {
    assertThat(graphApi.classIsInCategory(a, b), is(true));
  }

  @Test
  public void testUnconnectedClass() {
    assertThat(graphApi.classIsInCategory(b, c), is(false));
  }

}
