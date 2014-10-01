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

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class Csv2OwlTest {

  OWLOntology ontology;
  OWLDataFactory df = OWLManager.getOWLDataFactory();

  OWLClass parent = mock(OWLClass.class);
  OWLClass term = mock(OWLClass.class);
  OWLClass term2 = mock(OWLClass.class);

  @Before
  public void setUp() throws Exception {
    Csv2Owl converter = new Csv2Owl();
    Reader reader = new StringReader("1\tterm\tcomment\n2\tterm1\t\n");
    ontology = converter.convert("http://example.org", reader, "http://example.org/parent");
    when(parent.getIRI()).thenReturn(IRI.create("http://example.org/parent"));
    when(term.getIRI()).thenReturn(IRI.create("http://example.org#1"));
    when(term2.getIRI()).thenReturn(IRI.create("http://example.org#2"));
  }

  @Test
  public void verifyClassCreation() throws Exception {
    assertThat(ontology.getClassesInSignature(), containsInAnyOrder(parent, term2, term));
  }

  @Test
  public void testSubclassCreation() throws Exception {
    for (OWLSubClassOfAxiom axiom: ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
      assertThat(axiom.getSuperClass().asOWLClass(), is(parent));
    }
  }

  @Test
  public void veryLabelAssertion() throws Exception {
    OWLEntity entity = getOnlyElement(ontology.getEntitiesInSignature(IRI.create("http://example.org#1")));
    OWLAnnotation annotation = getOnlyElement(entity.getAnnotations(ontology, df.getRDFSLabel()));
    assertThat((OWLLiteral) annotation.getValue(), is(df.getOWLLiteral("term")));
  }

}
