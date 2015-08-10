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
package io.scigraph.owlapi;

import io.scigraph.owlapi.loader.OwlLoadConfiguration.ReasonerConfiguration;

import java.util.Collection;
import java.util.logging.Logger;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class SatisfiabilityChecker {

  private static final Logger logger = Logger.getLogger(SatisfiabilityChecker.class.getName());

  public static void main(String[] args) throws Exception {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ont = manager.loadOntology(IRI.create(args[0]));
    ReasonerConfiguration config = new ReasonerConfiguration();
    config.setFactory(ElkReasonerFactory.class.getCanonicalName());
    ReasonerUtil util = new ReasonerUtil(config, manager, ont);
    Collection<OWLOntologyChange> removals = util.removeUnsatisfiableClasses();
    if (!removals.isEmpty()) {
      logger.info("Removed " + removals.size() + " to help prevent unsatisfiable classes.");
      for (OWLOntologyChange removal: removals) {
        logger.info(removal.toString());
      }
    }
    OWLReasoner reasoner = util.getReasoner();
    if (!reasoner.isConsistent()) {
      logger.severe("Ontology is inconsistent");
      System.exit(1);
    }
    Collection<OWLClass> unsatisfiableClasses = util.getUnsatisfiableClasses();
    if (!unsatisfiableClasses.isEmpty()) {
      logger.severe("Ontology is unsatisfiable");
       for (OWLClass unsatisfiableClass: unsatisfiableClasses) {
         logger.severe(unsatisfiableClass.toString());
       }
       System.exit(2);
    }
    logger.info("Ontology is consistent and satisfiable");
    System.exit(0);
  }

}
