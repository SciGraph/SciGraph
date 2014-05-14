/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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

import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Resources;

@Ignore
public class LoggingOWLVisitorTest {

  @Test
  public void testLogging() throws Exception {
    String uri = Resources.getResource("ontologies/family.owl").toURI().toString();
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
    LoggingOWLVisitor visitor = new LoggingOWLVisitor(walker);
    
    for (OWLOntology ontology: manager.getOntologies()) {
      OWLOntologyFormat format = manager.getOntologyFormat(ontology);
      System.out.println(format.asPrefixOWLOntologyFormat().getPrefixName2PrefixMap());
      ListMultimap<String, String> prefixMap = 
          Multimaps.invertFrom(Multimaps.forMap(format.asPrefixOWLOntologyFormat().getPrefixName2PrefixMap()), ArrayListMultimap.<String, String>create());
      System.out.println(prefixMap);
    }
    
    walker.walkStructure(visitor);
  }

}