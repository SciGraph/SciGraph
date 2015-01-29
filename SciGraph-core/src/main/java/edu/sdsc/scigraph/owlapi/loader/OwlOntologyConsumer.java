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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.inject.Named;

import org.semanticweb.owlapi.model.OWLObject;

import com.google.inject.Inject;

import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.GraphOwlVisitor;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class OwlOntologyConsumer implements Callable<Void> {

  private static final Logger logger = Logger.getLogger(OwlOntologyConsumer.class.getName());

  private final BlockingQueue<OWLObject> queue;
  private final int numProducers;
  private final GraphOwlVisitor visitor;
  private final AtomicInteger numProducersShutdown;

  // TODO: Switch this to assisted inject
  @Inject
  OwlOntologyConsumer(BlockingQueue<OWLObject> queue, Graph graph, int numProducers,
      @Named("owl.mappedProperties") List<MappedProperty> mappedProperties,
      AtomicInteger numProducersShutdown) {
    logger.info("Ontology consumer starting up...");
    this.queue = queue;
    this.numProducers = numProducers;
    this.numProducersShutdown = numProducersShutdown;
    visitor = new GraphOwlVisitor(null, graph, mappedProperties);
  }

  @Override
  public Void call() {
    try {
      while (true) {
        OWLObject walker = null;
        if (numProducersShutdown.get() < numProducers) {
          walker = queue.take();
        } else {
          if (!queue.isEmpty()) {
            walker = queue.take();
          } else {
            break;
          }
        }
        walker.accept(visitor);
      }  
    } catch (InterruptedException consumed) {}
    logger.info("Ontology consumer shutting down...");
    return null;
  }

}
