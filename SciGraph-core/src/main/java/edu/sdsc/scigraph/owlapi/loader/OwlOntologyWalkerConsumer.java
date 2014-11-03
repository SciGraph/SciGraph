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

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.inject.Inject;

import edu.sdsc.scigraph.neo4j.BatchGraph;
import edu.sdsc.scigraph.owlapi.BatchOwlVisitor;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class OwlOntologyWalkerConsumer implements Callable<Void> {

  BlockingQueue<OWLOntologyWalker> queue;

  BatchGraph graph;
  
  int numProducers;

  @Inject
  OwlOntologyWalkerConsumer(BlockingQueue<OWLOntologyWalker> queue, BatchGraph graph, int numProducers) {
    this.queue = queue;
    this.graph = graph;
    this.numProducers = numProducers;
  }
  
  @Override
  public Void call() throws Exception {
    try {
      int poisonCount = 0;
      while (true) {
        OWLOntologyWalker walker = queue.take();
        if (BatchOwlLoader.POISON == walker) {
          poisonCount++;
          if (numProducers == poisonCount) {
            break;
          }
        } else {
          BatchOwlVisitor visitor = new BatchOwlVisitor(walker, graph, Collections.<MappedProperty>emptyList());
          walker.walkStructure(visitor);
        }
      }  
    } catch (InterruptedException consumed) {}
    return null;
  }

}
