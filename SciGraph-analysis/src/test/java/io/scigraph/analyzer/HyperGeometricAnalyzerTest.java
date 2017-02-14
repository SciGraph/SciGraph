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
package io.scigraph.analyzer;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import io.scigraph.analyzer.AnalyzeRequest;
import io.scigraph.analyzer.HyperGeometricAnalyzer;
import io.scigraph.frames.NodeProperties;
import io.scigraph.internal.CypherUtil;
import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.OwlPostprocessor;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.util.GraphTestBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.prefixcommons.CurieUtil;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

public class HyperGeometricAnalyzerTest extends GraphTestBase {

  static HyperGeometricAnalyzer analyzer;
  static CurieUtil util;

  @BeforeClass
  public static void loadPizza() throws Exception {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    String uri = Resources.getResource("pizza.owl").toURI().toString();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());

    MappedProperty mappedProperty = new MappedProperty(NodeProperties.LABEL);
    List<String> properties = new ArrayList<String>();
    properties.add("http://www.w3.org/2000/01/rdf-schema#label");
    properties.add("http://www.w3.org/2004/02/skos/core#prefLabel");
    mappedProperty.setProperties(properties);

    ArrayList<MappedProperty> mappedPropertyList = new ArrayList<MappedProperty>();
    mappedPropertyList.add(mappedProperty);

    GraphOwlVisitor visitor = new GraphOwlVisitor(walker, graph, mappedPropertyList);
    walker.walkStructure(visitor);
    Map<String, String> categories = new HashMap<>();
    categories.put("http://www.co-ode.org/ontologies/pizza/pizza.owl#NamedPizza", "pizza");
    categories.put("http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaTopping", "topping");
    try (Transaction tx = graphDb.beginTx()) {
      OwlPostprocessor postprocessor = new OwlPostprocessor(graphDb, categories);
      postprocessor.processCategories(categories);
      postprocessor.processSomeValuesFrom();
      tx.success();
    }

    Map<String, String> map = new HashMap<>();
    map.put("pizza", "http://www.co-ode.org/ontologies/pizza/pizza.owl#");
    util = new CurieUtil(map);
    CypherUtil cypherUtil = new CypherUtil(graphDb, util);
    analyzer = new HyperGeometricAnalyzer(graphDb, util, graph, cypherUtil);
  }

  @Test
  public void smokeTest() {
    AnalyzeRequest request = new AnalyzeRequest();
    request.getSamples()
    .addAll(newHashSet("pizza:FourSeasons", "pizza:AmericanHot", "pizza:Cajun"));
    request.setOntologyClass("pizza:Pizza");
    request.setPath("-[:pizza:hasTopping]->");
    assertThat(analyzer.analyze(request), is(not(empty())));
  }

  @Test
  public void processRequestDoesNotMutate() throws Exception {
    AnalyzeRequest request = new AnalyzeRequest();
    request.setPath("pizza:foo");
    request.setOntologyClass("pizza:bar");
    analyzer.processRequest(request);
    assertThat(request.getPath(), is("pizza:foo"));
    assertThat(request.getOntologyClass(), is("pizza:bar"));
  }

}
