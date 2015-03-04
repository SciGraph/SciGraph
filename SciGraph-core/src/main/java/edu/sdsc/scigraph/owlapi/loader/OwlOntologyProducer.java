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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLObject;

import com.google.inject.Inject;

import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.OntologySetup;

final class OwlOntologyProducer implements Callable<Void>{

  private static final Logger logger = Logger.getLogger(OwlOntologyProducer.class.getName());

  private final BlockingQueue<OWLObject> queue;
  private final BlockingQueue<OntologySetup> ontologQueue;
  private final AtomicInteger numProducersShutdown;

  @Inject
  OwlOntologyProducer(BlockingQueue<OWLObject> queue, BlockingQueue<OntologySetup> ontologyQueue, AtomicInteger numProducersShutdown) {
    logger.info("Producer starting up...");
    this.queue = queue;
    this.ontologQueue = ontologyQueue;
    this.numProducersShutdown = numProducersShutdown;
  }

  @Override
  public Void call() throws Exception {
    try {
      while (true) {
        OntologySetup ontologyConfig = ontologQueue.take();
        if (BatchOwlLoader.POISON_STR == ontologyConfig) {
          break;
        } else {
          logger.info("Loading ontology: " + ontologyConfig);
          Producer producer = null;
          if (ontologyConfig.url().endsWith(".ttl") && !ontologyConfig.getReasonerConfiguration().isPresent()) {
            producer = new TtlProducer(queue);
          } else {
            producer = new OwlApiProducer(ontologyConfig, queue);
          }
          producer.produce();
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
