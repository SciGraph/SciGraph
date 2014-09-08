package edu.sdsc.scigraph.owlapi.cases;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

import edu.sdsc.scigraph.neo4j.BatchGraph;
import edu.sdsc.scigraph.owlapi.BatchOwlVisitor;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.owlapi.OwlPostprocessor;

/***
 * An abstract test case for testing simple OWL axiom combinations.
 * 
 * <p>
 * The OWL structure in question should be placed in src/test/resources/ontologies/cases/x.owl
 * (where x matches the name of the test class). In addition to running the unit tests, GraphViz dot
 * files will be produced for each OWL file in target/owl_cases.
 */
public abstract class OwlTestCase {

  Path path;
  GraphDatabaseService graphDb;
  ReadableIndex<Node> nodeIndex;

  String getTestName() {
    return getClass().getSimpleName();
  }

  Node getNode(String uri) {
    return nodeIndex.get("uri", uri).getSingle();
  }

  @Before
  public void loadOwl() throws Exception {
    path = Files.createTempDirectory("SciGraph-OwlTest");

    BatchInserter inserter = BatchInserters.inserter(path.toFile().getAbsolutePath());
    BatchGraph batchGraph = new BatchGraph(inserter, "uri", Collections.<String> emptySet(),
        Collections.<String> emptySet());
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    String uri = Resources.getResource("ontologies/cases/" + getTestName() + ".owl").toURI()
        .toString();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());

    BatchOwlVisitor visitor = new BatchOwlVisitor(walker, batchGraph, new ArrayList<MappedProperty>());
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
    FileUtils.deleteDirectory(path.toFile());
  }

}
