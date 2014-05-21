package edu.sdsc.scigraph.internal.reachability;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.google.common.io.Resources;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.OwlVisitor;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class ReachabilityIndexTest {

	  static GraphDatabaseService graphDb;
	  static Graph<Concept> graph;
	  static final String ROOT = "http://example.com/owl/families";
	  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";
	  static ReachabilityIndex irx;
	  

	  @BeforeClass
	  public static void setup() throws Exception {
	    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
	    graph = new Graph<Concept>(graphDb, Concept.class);
	    String uri = Resources.getResource("ontologies/family.owl").toURI().toString();
	    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	    IRI iri = IRI.create(uri);
	    manager.loadOntologyFromOntologyDocument(iri);
	    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
	    Map<String, String> curieMap = new HashMap<>();
	    curieMap.put("http://example.org/otherOntologies/families/", "otherOnt");
	    Map<String, String> categoryMap = new HashMap<>();
	    Transaction tx = graphDb.beginTx();

	    List<MappedProperty> propertyMap = new ArrayList<>();
	    MappedProperty age = mock(MappedProperty.class);
	    when(age.getName()).thenReturn("isAged");
	    when(age.getProperties()).thenReturn(newArrayList(ROOT + "/hasAge"));
	    propertyMap.add(age);

	    OwlVisitor visitor = new OwlVisitor(walker, graph, curieMap, categoryMap, propertyMap);
	    walker.walkStructure(visitor);
	    visitor.postProcess();
	    tx.success();
	    tx.finish();
	    
	    irx = new ReachabilityIndex(graph);
	    try { 
	    	irx.creatIndex();
	    }catch (ReachabilityIndexException e) {
	    	fail(e.getMessage());
	    }

	  }

	  @AfterClass
	  public static void destroyTestDatabase() {
	    try {
				irx.dropIndex();
	    } catch (ReachabilityIndexException e) {
         	fail(e.getMessage());
	    }
	    
	    graphDb.shutdown();
	    irx = null;
	    graphDb = null;
	    graph = null;
	  }

	  @Test
	  public void testReachabilityIdxAccess()  {
	    Node n1 = graph.getNode("http://example.com/owl/families/Parent").get();
	    Node n2 = graph.getNode("http://example.com/owl/families/John").get();
	    Node n3 = graph.getNode("http://example.com/owl/families/Female").get();
	    try { 
	      assertTrue( irx.canReach(n2, n1));	
	      assertTrue(!irx.canReach(n1, n2));
	      assertTrue(!irx.canReach(n1, n3));
	    }catch (ReachabilityIndexException e) {
	    	fail(e.getMessage());
	    }
	  }


	
}
