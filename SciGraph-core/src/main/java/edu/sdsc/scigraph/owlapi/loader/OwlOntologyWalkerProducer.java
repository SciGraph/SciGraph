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
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.inject.Inject;

import edu.sdsc.scigraph.owlapi.FileCachingIRIMapper;

public class OwlOntologyWalkerProducer implements Callable<Void>{

  private static final Logger logger = Logger.getLogger(OwlOntologyWalkerProducer.class.getName());

  private final BlockingQueue<OWLOntologyWalker> queue;
  private final BlockingQueue<String> urlQueue;
  private final int numConsumers;
  private final UrlValidator validator = UrlValidator.getInstance();

  @Inject
  OwlOntologyWalkerProducer(BlockingQueue<OWLOntologyWalker> queue, BlockingQueue<String> urlQueue, int numConsumers) {
    this.queue = queue;
    this.urlQueue = urlQueue;
    this.numConsumers = numConsumers;
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
          manager.addIRIMapper(new FileCachingIRIMapper());
          logger.info("Loading ontology: " + url);
          if (validator.isValid(url)) {
            manager.loadOntology(IRI.create(url));
          } else {
            manager.loadOntologyFromOntologyDocument(new File(url));
          }
          queue.offer(new OWLOntologyWalker(manager.getOntologies()));
        }
      }
    } catch (Exception e) { /* fall through */ }
    finally {
      int poisonCount = numConsumers;
      while (true) {
        try {
          while (poisonCount-- > 0) {
            queue.put(BatchOwlLoader.POISON);
          }
          break;
        } catch (InterruptedException e1) { /* Retry */}
      }
    }
    return null;
  }

}
