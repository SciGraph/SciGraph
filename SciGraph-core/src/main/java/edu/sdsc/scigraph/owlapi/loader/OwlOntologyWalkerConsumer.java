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

import javax.inject.Named;

import org.semanticweb.owlapi.model.OWLObject;

import com.google.inject.Inject;

import edu.sdsc.scigraph.neo4j.BatchGraph;
import edu.sdsc.scigraph.owlapi.BatchOwlVisitor;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class OwlOntologyWalkerConsumer implements Callable<Void> {

  BlockingQueue<OWLObject> queue;

  BatchGraph graph;
  
  int numProducers;
  
  List<MappedProperty> mappedProperties;
  
  BatchOwlVisitor visitor;

  // TODO: Switch this to assisted inject
  @Inject
  OwlOntologyWalkerConsumer(BlockingQueue<OWLObject> queue, BatchGraph graph, int numProducers,
      @Named("owl.mappedProperties") List<MappedProperty> mappedProperties) {
    this.queue = queue;
    this.graph = graph;
    this.numProducers = numProducers;
    this.mappedProperties = mappedProperties;
    visitor = new BatchOwlVisitor(null, graph, mappedProperties);
  }
  
  @Override
  public Void call() throws Exception {
    try {
      int poisonCount = 0;
      while (true) {
        OWLObject walker = queue.take();
        if (BatchOwlLoader.POISON == walker) {
          poisonCount++;
          if (numProducers == poisonCount) {
            break;
          }
        } else {
          walker.accept(visitor);
        }
      }  
    } catch (InterruptedException consumed) {}
    return null;
  }

}
