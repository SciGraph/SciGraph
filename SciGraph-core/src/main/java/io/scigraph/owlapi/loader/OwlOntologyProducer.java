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
package io.scigraph.owlapi.loader;

import io.scigraph.neo4j.Graph;
import io.scigraph.owlapi.OwlApiUtils;
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.owlapi.ReasonerUtil;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfShutdownProducers;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationAssertionAxiomImpl;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.inject.Inject;

final class OwlOntologyProducer implements Callable<Long> {

  private static final Logger logger = Logger.getLogger(OwlOntologyProducer.class.getName());

  private final BlockingQueue<OWLCompositeObject> queue;
  private final BlockingQueue<OntologySetup> ontologQueue;
  private final AtomicInteger numProducersShutdown;
  private final Graph graph;

  @Inject
  OwlOntologyProducer(BlockingQueue<OWLCompositeObject> queue,
      BlockingQueue<OntologySetup> ontologyQueue,
      @IndicatesNumberOfShutdownProducers AtomicInteger numProducersShutdown, Graph graph) {
    logger.info("Producer starting up...");
    this.queue = queue;
    this.ontologQueue = ontologyQueue;
    this.numProducersShutdown = numProducersShutdown;
    this.graph = graph;
  }

  public void reason(OWLOntologyManager manager, OWLOntology ont, OntologySetup config)
      throws Exception {
    if (config.isSkipImports() == false && config.getReasonerConfiguration().isPresent()) {
      String origThreadName = Thread.currentThread().getName();
      Thread.currentThread().setName("reasoning - " + config);
      ReasonerUtil util = new ReasonerUtil(config.getReasonerConfiguration().get(), manager, ont);
      util.reason();
      Thread.currentThread().setName(origThreadName);
    }
  }

  public void queueObjects(OWLOntologyManager manager, OntologySetup ontologyConfig)
      throws InterruptedException {
    String origThreadName = Thread.currentThread().getName();
    Thread.currentThread().setName("queueing axioms - " + ontologyConfig);
    logger.info("Queueing axioms for: " + ontologyConfig);
    long objectCount = 0;
    for (OWLOntology ontology : manager.getOntologies()) {
      String ontologyIri = OwlApiUtils.getIri(ontology);
      queue.put(new OWLCompositeObject(ontologyIri, ontology));

      for (OWLObject object : ontology.getNestedClassExpressions()) {
        queue.put(new OWLCompositeObject(ontologyIri, object));
        objectCount++;
      }
      for (OWLObject object : ontology.getClassesInSignature(false)) {
        queue.put(new OWLCompositeObject(ontologyIri, object));
        objectCount++;
      }
            
      for (OWLObject object : ontology.getAxioms()) { // only in the current ontology
        queue.put(new OWLCompositeObject(ontologyIri, object));
        objectCount++;
      }
      
      for (OWLAnnotation annotation : ontology.getAnnotations()) { // Get annotations on ontology iri
        OWLAnnotationSubject ontologySubject = IRI.create(ontologyIri);
        OWLAnnotationAssertionAxiom object = 
                new OWLAnnotationAssertionAxiomImpl(ontologySubject,
                        annotation.getProperty(), annotation.getValue(),
                        new ArrayList<OWLAnnotation>());
        queue.put(new OWLCompositeObject(ontologyIri, object));
        objectCount++;
      }
    }
    Thread.currentThread().setName(origThreadName);
    logger.info("Finished queueing " + objectCount + " axioms for: " + ontologyConfig);
  }

  void addOntologyStructure(OWLOntologyManager manager, OWLOntology ontology) {
    long parent = graph.createNode(OwlApiUtils.getIri(ontology));
    graph.addLabel(parent, OwlLabels.OWL_ONTOLOGY);
    for (OWLImportsDeclaration importDeclaration : ontology.getImportsDeclarations()) {
      OWLOntology childOnt = manager.getImportedOntology(importDeclaration);
      if (null == childOnt) {
        // TODO: Why is childOnt sometimes null (when importing rdf)?
        continue;
      }
      long child = graph.createNode(OwlApiUtils.getIri(childOnt));
      graph.addLabel(parent, OwlLabels.OWL_ONTOLOGY);
      if (graph.getRelationship(child, parent, OwlRelationships.RDFS_IS_DEFINED_BY).isPresent()) {
        continue;
      }
      graph.createRelationship(child, parent, OwlRelationships.RDFS_IS_DEFINED_BY);
      addOntologyStructure(manager, childOnt);
    }
  }

  @Override
  public Long call() throws Exception {
    try {
      while (true) {
        final OntologySetup ontologyConfig = ontologQueue.take();
        if (BatchOwlLoader.POISON_STR == ontologyConfig) {
          break;
        } else {
          final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
          if (ontologyConfig.isSkipImports()) {
            final Set<IRI> emptyOntologies = new HashSet<IRI>();
            manager.addIRIMapper(new OWLOntologyIRIMapper() {
              @Override
              public IRI getDocumentIRI(IRI ontologyIRI) {
                // quick check:
                // do nothing for the original url and known empty ontologies
                if (ontologyConfig.url().equals(ontologyIRI.toString())
                    || emptyOntologies.contains(ontologyIRI)) {
                  return null;
                }
                emptyOntologies.add(ontologyIRI);
                try {
                  OWLOntology emptyOntology = manager.createOntology(ontologyIRI);
                  return emptyOntology.getOntologyID().getDefaultDocumentIRI();
                } catch (OWLOntologyCreationException e) {
                  logger.log(Level.SEVERE, "This should never happen: " + e);
                  return null;
                }
              }
            });
          }
          logger.info("Processing ontology: " + ontologyConfig);
          try {
            OWLOntology ontology = OwlApiUtils.loadOntology(manager, ontologyConfig.url());
            reason(manager, ontology, ontologyConfig);
            logger.info("Adding ontology structure");
            addOntologyStructure(manager, ontology);
            logger.info("Finished adding ontology structure");
            queueObjects(manager, ontologyConfig);
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load ontology: " + ontologyConfig, e);
            throw new Exception("Failed to load ontology: " + ontologyConfig + "\n" + e);
          }
        }
      }
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    } finally {
      numProducersShutdown.incrementAndGet();
      logger.info("Producer shutting down...");
    }
    return 0L;

  }

}
