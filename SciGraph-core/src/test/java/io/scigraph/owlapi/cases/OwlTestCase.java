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
package io.scigraph.owlapi.cases;

import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.OwlPostprocessor;
import io.scigraph.owlapi.ReasonerUtil;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.ReasonerConfiguration;
import io.scigraph.util.GraphTestBase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Before;
import org.neo4j.graphdb.Node;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

/***
 * An abstract test case for testing simple OWL axiom combinations.
 * 
 * <p>
 * The OWL structure in question should be placed in src/test/resources/ontologies/cases/x.owl
 * (where x matches the name of the test class). In addition to running the unit tests, GraphViz dot
 * files will be produced for each OWL file in target/owl_cases.
 */
public abstract class OwlTestCase extends GraphTestBase {

  boolean performInference = false;

  String getTestName() {
    return getClass().getSimpleName();
  }

  @Before
  public void loadOwl() throws Exception {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    String uri = Resources.getResource("ontologies/cases/" + getTestName() + ".owl").toURI()
        .toString();
    IRI iri = IRI.create(uri);
    OWLOntology ont = manager.loadOntologyFromOntologyDocument(iri);
    if (performInference) {
      ReasonerConfiguration config = new ReasonerConfiguration();
      config.setFactory(ElkReasonerFactory.class.getCanonicalName());
      config.setAddDirectInferredEdges(true);
      ReasonerUtil util = new ReasonerUtil(config, manager, ont);
      util.reason();
    }
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());

    GraphOwlVisitor visitor = new GraphOwlVisitor(walker, graph, new ArrayList<MappedProperty>());
    walker.walkStructure(visitor);

    OwlPostprocessor postprocessor = new OwlPostprocessor(graphDb, Collections.<String, String>emptyMap());
    postprocessor.processSomeValuesFrom();

    drawGraph();
  }

  static Node getNode(String iri) {
    Long id = graph.getNode(iri).get();
    return graphDb.getNodeById(id);
  }
  
  void drawGraph() throws IOException {
    GraphvizWriter writer = new GraphvizWriter();
    Walker walker = Walker.fullGraph(graphDb);
    new File("target/owl_cases").mkdirs();
    writer.emit(new File("target/owl_cases/" + getTestName() + ".dot"), walker);
  }

}
