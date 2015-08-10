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

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.scigraph.owlapi.ReasonerUtil;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.ReasonerConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.io.Resources;

public class ReasonerUtilTest {

  OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
  OWLOntologyManager manager;
  OWLOntology ont;
  OWLOntology unsatImport;
  ReasonerUtil util;

  @Before
  public void setup() throws Exception {
    String uri = Resources.getResource("ontologies/reasoner.owl").toURI().toString();
    IRI iri = IRI.create(uri);
    manager = OWLManager.createOWLOntologyManager();
    ont = manager.loadOntologyFromOntologyDocument(iri);
    ReasonerConfiguration config = new ReasonerConfiguration();
    config.setFactory(ElkReasonerFactory.class.getCanonicalName());
    config.setAddDirectInferredEdges(true);
    config.setAddInferredEquivalences(true);
    util = new ReasonerUtil(config, manager, ont);
  }

  @Test
  public void fullEquivalenceClassesAreAdded() throws Exception {
    OWLClass e0 = dataFactory.getOWLClass(IRI.create("http://example.org/e0"));
    OWLClass e1 = dataFactory.getOWLClass(IRI.create("http://example.org/e1"));
    OWLClass e2 = dataFactory.getOWLClass(IRI.create("http://example.org/e2"));
    OWLClassAxiom fullEquivalence = dataFactory.getOWLEquivalentClassesAxiom(e0, e1, e2);
    assertThat(ont.containsAxiom(fullEquivalence), is(false));
    util.removeUnsatisfiableClasses();
    util.reason();
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
    util.removeUnsatisfiableClasses();
    util.reason();
    assertThat(ont.containsAxiom(inferrredSubclass), is(true));
    assertThat(ont.containsAxiom(originalSubclass), is(false));
  }

  @Test
  public void doesNotReason_whenOntologyIsInconsistent() throws Exception{
    OWLClass c0 = dataFactory.getOWLClass(IRI.generateDocumentIRI());
    OWLClass c1 = dataFactory.getOWLClass(IRI.generateDocumentIRI());
    OWLDisjointClassesAxiom disjoint = dataFactory.getOWLDisjointClassesAxiom(c0, c1);
    OWLIndividual i1 = dataFactory.getOWLNamedIndividual(IRI.generateDocumentIRI());
    OWLClassAssertionAxiom a1 = dataFactory.getOWLClassAssertionAxiom(c0, i1);
    OWLClassAssertionAxiom a2 = dataFactory.getOWLClassAssertionAxiom(c1, i1);
    manager.addAxioms(ont, newHashSet(disjoint, a1, a2));
    util.flush();
    assertThat(util.shouldReason(), is(false));
  }

  @Test
  public void doesNotReason_whenOntologyIsUnsatisfiable() throws Exception {
    OWLAxiom axiom = dataFactory.getOWLSubClassOfAxiom(
        dataFactory.getOWLClass(IRI.generateDocumentIRI()), dataFactory.getOWLNothing());
    manager.addAxiom(ont, axiom);
    util.flush();
    assertThat(util.shouldReason(), is(false));
  }

  @Test
  public void redundantAxioms_areRemoved() throws Exception {
    OWLClass e = dataFactory.getOWLClass(IRI.create("http://example.org/e"));
    OWLClass c = dataFactory.getOWLClass(IRI.create("http://example.org/c"));
    OWLClassAxiom originalSubclass = dataFactory.getOWLSubClassOfAxiom(e, c);
    assertThat(ont.containsAxiom(originalSubclass), is(true));
    util.removeRedundantAxioms();
    assertThat(ont.containsAxiom(originalSubclass), is(false)); 
  }

}
