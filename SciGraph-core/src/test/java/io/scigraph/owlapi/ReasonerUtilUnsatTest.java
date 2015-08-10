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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import io.scigraph.owlapi.ReasonerUtil;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.ReasonerConfiguration;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.io.Resources;

public class ReasonerUtilUnsatTest {

  OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
  OWLOntologyManager manager;
  OWLOntology ont;
  OWLOntology unsatImport;
  ReasonerUtil util;

  @Before
  public void setup() throws Exception {
    String uri = Resources.getResource("ontologies/unsat_parent.owl").toURI().toString();
    IRI iri = IRI.create(uri);
    manager = OWLManager.createOWLOntologyManager();
    manager.addIRIMapper(new OWLOntologyIRIMapper() {
      @Override
      public IRI getDocumentIRI(IRI origIri) {
        String uri = null;
        try {
          uri = Resources.getResource("ontologies/unsat.owl").toURI().toString();
        } catch (URISyntaxException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return IRI.create(uri);
      }
    });
    ont = manager.loadOntologyFromOntologyDocument(iri);
    ReasonerConfiguration config = new ReasonerConfiguration();
    config.setFactory(ElkReasonerFactory.class.getCanonicalName());
    util = new ReasonerUtil(config, manager, ont);
  }

  @Test
  public void removesAxiomsInImportedOntologies() throws Exception {
    assertThat(util.getUnsatisfiableClasses(), is(not(empty())));
    util.removeUnsatisfiableClasses();
    assertThat(util.getUnsatisfiableClasses(), is(empty()));
    assertThat(util.reason(), is(true));
  }

}
