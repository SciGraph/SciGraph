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
package edu.sdsc.scigraph.owlapi.cases;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphBatchImpl;
import edu.sdsc.scigraph.neo4j.IdMap;
import edu.sdsc.scigraph.neo4j.RelationshipMap;
import edu.sdsc.scigraph.owlapi.GraphOwlVisitor;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.ReasonerConfiguration;
import edu.sdsc.scigraph.owlapi.OwlPostprocessor;
import edu.sdsc.scigraph.owlapi.ReasonerUtil;

/***
 * An abstract test case for testing simple OWL axiom combinations.
 * 
 * <p>
 * The OWL structure in question should be placed in src/test/resources/ontologies/cases/x.owl
 * (where x matches the name of the test class). In addition to running the unit tests, GraphViz dot
 * files will be produced for each OWL file in target/owl_cases.
 */
public abstract class OwlTestCase {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  
  String path;
  GraphDatabaseService graphDb;
  ReadableIndex<Node> nodeIndex;
  
  boolean performInference = false;

  String getTestName() {
    return getClass().getSimpleName();
  }

  Node getNode(String uri) {
    return nodeIndex.get("uri", uri).getSingle();
  }

  @Before
  public void loadOwl() throws Exception {
    path = folder.newFolder().getAbsolutePath();

    BatchInserter inserter = BatchInserters.inserter(path);
    DB maker = DBMaker.newMemoryDB().make();
    Graph batchGraph = new GraphBatchImpl(inserter, "uri", Collections.<String> emptySet(),
        Collections.<String> emptySet(), new IdMap(maker), new RelationshipMap(maker));
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

    GraphOwlVisitor visitor = new GraphOwlVisitor(walker, batchGraph, new ArrayList<MappedProperty>());
    walker.walkStructure(visitor);
    batchGraph.shutdown();
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path.toString());
    nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();

    OwlPostprocessor postprocessor = new OwlPostprocessor(graphDb, Collections.<String, String>emptyMap());
    postprocessor.processSomeValuesFrom();

    graphDb.beginTx();
    drawGraph();
  }

  void drawGraph() throws IOException {
    GraphvizWriter writer = new GraphvizWriter();
    Walker walker = Walker.fullGraph(graphDb);
    new File("target/owl_cases").mkdirs();
    writer.emit(new File("target/owl_cases/" + getTestName() + ".dot"), walker);
  }

  @After
  public void tearDownAfterClass() throws Exception {
    // TODO: Why does this fail on Windows
    //FileUtils.deleteDirectory(path.toFile());
  }

}
