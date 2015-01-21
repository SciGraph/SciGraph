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
package edu.sdsc.scigraph.owlapi;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.io.Resources;

public class ReasonerUtilTest {

  OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
  OWLOntologyManager manager;
  OWLOntology ont;
  ReasonerUtil util;

  @Before
  public void setup() throws Exception {
    String uri = Resources.getResource("ontologies/reasoner.owl").toURI().toString();
    IRI iri = IRI.create(uri);
    manager = OWLManager.createOWLOntologyManager();
    ont = manager.loadOntologyFromOntologyDocument(iri);
    util = new ReasonerUtil(new ElkReasonerFactory(), manager, ont);
  }

  @Test
  public void fullEquivalenceClassesAreAdded() throws Exception {
    OWLClass e0 = dataFactory.getOWLClass(IRI.create("http://example.org/e0"));
    OWLClass e1 = dataFactory.getOWLClass(IRI.create("http://example.org/e1"));
    OWLClass e2 = dataFactory.getOWLClass(IRI.create("http://example.org/e2"));
    OWLClassAxiom fullEquivalence = dataFactory.getOWLEquivalentClassesAxiom(e0, e1, e2);
    assertThat(ont.containsAxiom(fullEquivalence), is(false));
    util.reason(true, false);
    assertThat(ont.containsAxiom(fullEquivalence), is(true));
  }

  @Test
  public void subclassHierarchyIsRepaired() throws Exception {
    OWLClass dx = dataFactory.getOWLClass(IRI.create("http://example.org/dx"));
    OWLClass cx = dataFactory.getOWLClass(IRI.create("http://example.org/cx"));
    OWLClass root = dataFactory.getOWLClass(IRI.create("http://example.org/root"));
    OWLClassAxiom inferrredSubclass = dataFactory.getOWLSubClassOfAxiom(dx, cx);
    OWLClassAxiom originalSubclass = dataFactory.getOWLSubClassOfAxiom(dx, root);
    assertThat(ont.containsAxiom(inferrredSubclass), is(false));
    assertThat(ont.containsAxiom(originalSubclass), is(true));
    util.reason(false, true);
    assertThat(ont.containsAxiom(inferrredSubclass), is(true));
    assertThat(ont.containsAxiom(originalSubclass), is(false));
  }

  @Test
  @Ignore
  //TODO: Why doesn't this fail?
  public void doesNotReason_whenOntologyHasSubclassesOfNothing() {
    OWLClass e0 = dataFactory.getOWLClass(IRI.create("http://example.org/e0"));
    OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(e0, dataFactory.getOWLNothing());
    AddAxiom change = new AddAxiom(ont, axiom);
    manager.applyChange(change);
    util = new ReasonerUtil(new ElkReasonerFactory(), manager, ont);
    assertThat(util.shouldReason(new ElkReasonerFactory().createReasoner(ont), ont), is(false));
  }
  
}
