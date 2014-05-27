package edu.sdsc.scigraph.internal.reachability;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.base.Predicate;
import com.google.common.io.Resources;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.owlapi.OwlVisitor;

public class ReachabilityIndexIT {

  static Graph<Concept> graph;
  static final String ROOT = "http://example.com/owl/families";
  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";
  static ReachabilityIndex irx;

  @BeforeClass
  public static void setup() throws Exception {
    GraphDatabaseService graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    graph = new Graph<Concept>(graphDb, Concept.class);
    String uri = Resources.getResource("ontologies/family.owl").toURI().toString();
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
    Transaction tx = graphDb.beginTx();

    OwlVisitor visitor = new OwlVisitor(walker, graph, 
        new HashMap<String, String>(), new HashMap<String, String>(), new ArrayList<MappedProperty>());
    walker.walkStructure(visitor);
    tx.success();
    tx.finish();

    final Node owlThing = graph.getNode("http://www.w3.org/2002/07/owl#Thing").get();

    irx = new ReachabilityIndex(graph.getGraphDb());
    irx.createIndex(new Predicate<Node>() {
      @Override
      public boolean apply(Node input) {
        return !input.equals(owlThing);
      }
    });
  }

  @AfterClass
  public static void destroyTestDatabase() {
    irx.dropIndex();
    graph.shutdown();
  }

  @Test
  public void testReachabilityIdxAccess() {
    Node parent = graph.getNode("http://example.com/owl/families/Parent").get();
    Node john = graph.getNode("http://example.com/owl/families/John").get();
    Node female = graph.getNode("http://example.com/owl/families/Female").get();
    assertThat(irx.canReach(john, parent), is(true));
    assertThat(irx.canReach(parent, john), is(false));
    assertThat(irx.canReach(parent, female), is(false));
  }

}
