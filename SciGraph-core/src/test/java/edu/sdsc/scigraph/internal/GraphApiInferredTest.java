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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiInferredTest extends GraphTestBase {

  GraphApi graphApi;

  static String BASE_URI = "http://example.org/";

  static String fizzUri = BASE_URI + "#fizz";
  static String fuzzRoleUri = BASE_URI + "#fuzz";
  Node fizz;
  Node fuzzRole;
  Concept fizzConcept;
  Concept inferredConcept;

  /***
   * Fizz is EQUIVALENT to an anonymous OWLObjectSomeValuesFrom node with 
   * PROPERTY birnlex_17 and CLASS fizzRole.
   * fizzRole has an incoming birnlex_17 edge from fuzzRole (the inferred term)
   * @throws Exception
   */
  @Before
  public void addNodes() throws Exception {
    Graph graph = new Graph(graphDb, Concept.class);
    fizz = graph.getOrCreateNode(fizzUri);
    fizzConcept = graph.getOrCreateFramedNode(fizz);
    Node anon = graph.getOrCreateNode(BASE_URI + "#anon");
    graph.setProperty(anon, CommonProperties.TYPE, "OWLObjectSomeValuesFrom");
    graph.getOrCreateRelationship(fizz, anon, OwlRelationships.OWL_EQUIVALENT_CLASS);
    Node birnlex17 = graph.getOrCreateNode("http://ontology.neuinfo.org/NIF/Backend/BIRNLex-OBO-UBO.owl#birnlex_17");
    graph.getOrCreateRelationship(anon, birnlex17, EdgeType.PROPERTY);
    Node fizzRole = graph.getOrCreateNode(BASE_URI + "#fizzRole");
    graph.getOrCreateRelationship(anon, fizzRole, EdgeType.CLASS);
    fuzzRole = graph.getOrCreateNode(fuzzRoleUri);
    inferredConcept = graph.getOrCreateFramedNode(fuzzRole);
    graph.getOrCreateRelationship(fuzzRole, fizzRole, DynamicRelationshipType.withName("birnlex_17"));
    this.graphApi = new GraphApi(graph);
  }

  @Test
  public void testGetInferredClasses() {
    assertThat(graphApi.getInferredClasses(fizzConcept), contains(inferredConcept));
  }

}
