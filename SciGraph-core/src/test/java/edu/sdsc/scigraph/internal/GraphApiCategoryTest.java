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
package edu.sdsc.scigraph.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import edu.sdsc.scigraph.neo4j.GraphInterface;
import edu.sdsc.scigraph.neo4j.GraphInterfaceTransactionImpl;
import edu.sdsc.scigraph.neo4j.RelationshipMap;
import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiCategoryTest extends GraphTestBase {

  GraphApi graphApi;
  GraphInterface graph;

  static String BASE_URI = "http://example.org/";

  static String uri = BASE_URI + "#fizz";
  static String uri2 = BASE_URI + "#fuzz";
  static String uri3 = BASE_URI + "#fazz";
  long a;
  long b;
  long c;

  @Before
  public void addNodes() throws Exception {
    graph = new GraphInterfaceTransactionImpl(graphDb, new ConcurrentHashMap<String, Long>(), new RelationshipMap());
    a = graph.createNode(uri);
    graph.addLabel(a, OwlLabels.OWL_CLASS);
    b = graph.createNode(uri2);
    graph.addLabel(b, OwlLabels.OWL_CLASS);
    c = graph.createNode(uri3);
    graph.addLabel(c, OwlLabels.OWL_CLASS);
    graph.createRelationship(a, b, OwlRelationships.RDFS_SUBCLASS_OF);
    this.graphApi = new GraphApi(graphDb);
  }

  @Test
  public void testFoundClass() {
    assertThat(graphApi.classIsInCategory(graphDb.getNodeById(a), graphDb.getNodeById(b)), is(true));
  }

  @Test
  public void testUnconnectedClass() {
    assertThat(graphApi.classIsInCategory(graphDb.getNodeById(b), graphDb.getNodeById(c)), is(false));
  }

  /***
   * TODO: Move this to a GraphApiTest class
   */
  @Test
  public void testSelfLoop() {
    assertThat(graphApi.getSelfLoops(), is(empty()));
    long t = graph.createNode(BASE_URI + "#fozz");
    long r = graph.createRelationship(t, t, OwlRelationships.RDFS_SUBCLASS_OF);
    Relationship relationship = graphDb.getRelationshipById(r);
    assertThat(graphApi.getSelfLoops(), contains(relationship));
  }

}
