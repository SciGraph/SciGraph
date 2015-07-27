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
import org.junit.AfterClass;
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

  static Server server = new Server(10000);
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceHandler handler = new ResourceHandler();
    handler.setBaseResource(Resource.newClassPathResource("/ontologies/import/"));
    server.setHandler(handler);
    server.start();
  }
  
  @AfterClass
  public static void teardown() throws Exception {
    server.stop();
  }

  @Before
  public void setup() {
    producer = new OwlOntologyProducer(queue, null, new AtomicInteger(), graph);
  }

  @Test
  public void ontologyStructure_isAdded() throws OWLOntologyCreationException {
    OWLOntology ontology = manager.loadOntology(IRI.create("http://localhost:10000/main.owl"));
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
  public void circularOntologies_dontRecurseInfintely() throws Exception {
    OWLOntology ontology = manager.loadOntology(IRI.create("http://localhost:10000/circular_parent.owl"));
    producer.addOntologyStructure(manager, ontology);
    Optional<Long> parent = graph.getNode("http://example.org/ParentOntology");
    Optional<Long> grandchild = graph.getNode("http://example.org/GrandChildOntology");
    assertThat(graph.getRelationship(parent.get(), grandchild.get(), OwlRelationships.RDFS_IS_DEFINED_BY).isPresent(), is(true));
    assertThat(graph.getRelationship(grandchild.get(), grandchild.get(), OwlRelationships.RDFS_IS_DEFINED_BY).isPresent(), is(true));
  }

  @Test
  public void objects_areQueued() throws InterruptedException {
    OntologySetup ontologyConfig = new OntologySetup();
    ontologyConfig.setUrl("http://localhost:10000/main.owl");
    producer.queueObjects(manager, ontologyConfig);
    assertThat(queue.size(), is(greaterThan(0)));
  }

}
