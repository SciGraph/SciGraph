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
import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.owlapi.loader.bindings.IndicatesMappedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfProducerThreads;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfShutdownProducers;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;

final class OwlOntologyConsumer implements Callable<Long> {

  private static final Logger logger = Logger.getLogger(OwlOntologyConsumer.class.getName());

  private final BlockingQueue<OWLCompositeObject> queue;
  private final int numProducers;
  private final GraphOwlVisitor visitor;
  private final AtomicInteger numProducersShutdown;

  @Inject
  OwlOntologyConsumer(BlockingQueue<OWLCompositeObject> queue, Graph graph, @IndicatesNumberOfProducerThreads int numProducers,
      @IndicatesMappedProperties List<MappedProperty> mappedProperties,
      @IndicatesNumberOfShutdownProducers AtomicInteger numProducersShutdown) {
    logger.info("Ontology consumer starting up...");
    this.queue = queue;
    this.numProducers = numProducers;
    this.numProducersShutdown = numProducersShutdown;
    visitor = new GraphOwlVisitor(null, graph, mappedProperties);
  }

  @Override
  public Long call() {
    long objectCount = 0;
    try {
      while (true) {
        if (numProducersShutdown.get() < numProducers || !queue.isEmpty()) {
          // TODO: This should be removed with #141 work - no more incomplete ontology loads 
          OWLCompositeObject owlObject = queue.poll(1, TimeUnit.MINUTES);
          if (null == owlObject) {
            continue;
          }

          if (0 == queue.size() % 100_000) {
            logger.info("Currently " + queue.size() + " objects remaining in the queue");
          }
          try {
            visitor.setOntology(owlObject.getOntology());
            owlObject.getObject().accept(visitor);
          } catch (RuntimeException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
          }
          objectCount++;
        } else {
          break;
        }
      }
    } catch (InterruptedException consumed) {
      logger.log(Level.WARNING, consumed.getMessage(), consumed);
    }
    logger.info("Ontology consumer shutting after processing " + objectCount + " objects...");
    return objectCount;
  }

}
