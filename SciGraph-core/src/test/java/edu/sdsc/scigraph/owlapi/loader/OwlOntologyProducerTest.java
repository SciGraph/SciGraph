package edu.sdsc.scigraph.owlapi.loader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;
import edu.sdsc.scigraph.util.GraphTestBase;

public class OwlOntologyProducerTest extends GraphTestBase {

  static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  OwlOntologyProducer producer;
  BlockingQueue<OWLCompositeObject> queue = new LinkedBlockingQueue<OWLCompositeObject>();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Server server = new Server(8080);
    ResourceHandler handler = new ResourceHandler();
    handler.setBaseResource(Resource.newClassPathResource("/ontologies/import/"));
    server.setHandler(handler);
    server.start();
  }

  @Before
  public void setup() {
    producer = new OwlOntologyProducer(queue, null, new AtomicInteger(), graph);
  }

  @Test
  public void ontologyStructure_isAdded() throws OWLOntologyCreationException {
    OWLOntology ontology = manager.loadOntology(IRI.create("http://localhost:8080/main.owl"));
    producer.addOntologyStructure(manager, ontology);
    Optional<Long> main = graph.getNode("http://example.org/MainOntology");
    Optional<Long> child1 = graph.getNode("http://example.org/Child1");
    Optional<Long> child2 = graph.getNode("http://example.org/Child2");
    Optional<Long> grandchild = graph.getNode("http://example.org/GrandChild");
    assertThat(main.isPresent(), is(true));
    assertThat(child1.isPresent(), is(true));
    assertThat(child2.isPresent(), is(true));
    assertThat(grandchild.isPresent(), is(true));
    assertThat(graph.getRelationship(child1.get(), main.get(), OwlRelationships.RDFS_IS_DEFINED_BY).isPresent(), is(true));
  }

  @Test
  public void objects_areQueued() throws InterruptedException {
    OntologySetup ontologyConfig = new OntologySetup();
    ontologyConfig.setUrl("http://localhost:8080/main.owl");
    producer.queueObjects(manager, ontologyConfig);
    assertThat(queue.size(), is(greaterThan(0)));
  }

}
