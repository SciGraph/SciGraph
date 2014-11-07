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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Level;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.inject.Inject;

public class OwlOntologyWalkerProducer implements Callable<Void>{

  private static final Logger logger = Logger.getLogger(OwlOntologyWalkerProducer.class.getName());

  private final BlockingQueue<OWLOntologyWalker> queue;
  private final BlockingQueue<String> urlQueue;
  private final int numConsumers;
  private final UrlValidator validator = UrlValidator.getInstance();
  private final static OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
  private final static OWLDataFactory factory = OWLManager.getOWLDataFactory();

  @Inject
  OwlOntologyWalkerProducer(BlockingQueue<OWLOntologyWalker> queue, BlockingQueue<String> urlQueue, int numConsumers) {
    this.queue = queue;
    this.urlQueue = urlQueue;
    this.numConsumers = numConsumers;
  }

  public static void addDirectInferredEdges(OWLOntologyManager manager) {
    org.apache.log4j.Logger.getLogger("org.semanticweb.elk").setLevel(Level.WARN);

    for (OWLOntology ont: manager.getOntologies()) {
      OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
      reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

      //For each class x
      for (OWLClass ce: ont.getClassesInSignature()) {
        //find direct inferred superclasses, D
        NodeSet<OWLClass> directSuperclasses = reasoner.getSuperClasses(ce, true);
        //find indirect superclasses, I
        NodeSet<OWLClass> indirectSuperclasses = reasoner.getSuperClasses(ce, false);
        //find asserted superclasses, A
        Set<OWLClassExpression> assertedSuperclasses = ce.asOWLClass().getSuperClasses(ont);
        //for each d in D, add an edge SubClassOf(x d)
        for (Node<OWLClass> directSuperclass: directSuperclasses) {
          OWLAxiom axiom = factory.getOWLSubClassOfAxiom(ce, directSuperclass.getRepresentativeElement());
          AddAxiom addAxiom = new AddAxiom(ont, axiom);
          manager.applyChange(addAxiom);
        }
        //for each a in A
        for (OWLClassExpression assertedSuperclass: assertedSuperclasses) {
          //if a is in I and not in D, then remove edge SubClassOf(x a)
          if (indirectSuperclasses.containsEntity(assertedSuperclass.asOWLClass()) &&
              !directSuperclasses.containsEntity(assertedSuperclass.asOWLClass())) {
            OWLAxiom axiom = factory.getOWLSubClassOfAxiom(ce, assertedSuperclass);
            RemoveAxiom removeAxiom = new RemoveAxiom(ont, axiom);
            manager.applyChange(removeAxiom);
          }
        }
      }
      reasoner.dispose();
    }

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
          if (validator.isValid(url)) {
            manager.loadOntology(IRI.create(url));
          } else {
            manager.loadOntologyFromOntologyDocument(new File(url));
          }
          addDirectInferredEdges(manager);

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
