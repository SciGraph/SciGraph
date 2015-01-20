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

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import com.google.inject.Inject;

import edu.sdsc.scigraph.owlapi.ReasonerUtil;

public class OwlOntologyProducer implements Callable<Void>{

  private static final Logger logger = Logger.getLogger(OwlOntologyProducer.class.getName());

  private final BlockingQueue<OWLObject> queue;
  private final BlockingQueue<String> urlQueue;
  private final UrlValidator validator = UrlValidator.getInstance();
  private final static OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
  private final AtomicInteger numProducersShutdown;

  @Inject
  OwlOntologyProducer(BlockingQueue<OWLObject> queue, BlockingQueue<String> urlQueue, AtomicInteger numProducersShutdown) {
    logger.info("Producer starting up...");
    this.queue = queue;
    this.urlQueue = urlQueue;
    this.numProducersShutdown = numProducersShutdown;
  }

  @Override
  public Void call() throws Exception {
    try {
      while (true) {
        String url = urlQueue.take();
        if (BatchOwlLoader.POISON_STR == url) {
          break;
        } else {
          OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
          //manager.addIRIMapper(new FileCachingIRIMapper());
          logger.info("Loading ontology: " + url);
          try {
            OWLOntology ont = null;
            if (validator.isValid(url)) {
              ont = manager.loadOntology(IRI.create(url));
            } else {
              ont = manager.loadOntologyFromOntologyDocument(new File(url));
            }
            //if ("http://purl.obolibrary.org/obo/upheno/monarch.owl".equals(url)) {
            // TODO: fix this - move to configuration

            ReasonerUtil util = new ReasonerUtil(reasonerFactory, manager, ont);
            util.reason(true, true);
            //}
            logger.info("Adding axioms for: " + url);
            for (OWLOntology ontology: manager.getOntologies()) {
              for (OWLObject object: ontology.getNestedClassExpressions()) {
                queue.put(object);
              }
              for (OWLObject object: ontology.getSignature(true)) {
                queue.put(object);
              }
              for (OWLObject object: ontology.getAxioms()) {
                queue.put(object);
              }
            }
            logger.info("Finished processing ontology: " + url);
          } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load ontology: " + url, e);
          }
        }
      }
    } catch (Exception e) { 
      logger.log(Level.WARNING, "Failed to load ontology", e);
    }
    finally {
      numProducersShutdown.incrementAndGet();
    }

    logger.info("Producer shutting down...");
    return null;
  }

}
