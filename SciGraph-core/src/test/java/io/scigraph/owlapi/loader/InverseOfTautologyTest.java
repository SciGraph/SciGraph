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
package io.scigraph.owlapi.loader;

import static com.google.common.collect.Iterables.size;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.OwlPostprocessor;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.util.GraphTestBase;

public class InverseOfTautologyTest extends GraphTestBase {


  @Before
  public void setup() throws Exception {

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    String uri =
        Resources.getResource("ontologies/cases/TestInverseOfTautology.owl").toURI().toString();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());

    MappedProperty mappedProperty = new MappedProperty(NodeProperties.LABEL);
    List<String> properties = new ArrayList<String>();
    properties.add("http://www.w3.org/2000/01/rdf-schema#label");
    properties.add("http://www.w3.org/2004/02/skos/core#prefLabel");
    mappedProperty.setProperties(properties);

    ArrayList<MappedProperty> mappedPropertyList = new ArrayList<MappedProperty>();
    mappedPropertyList.add(mappedProperty);

    GraphOwlVisitor visitor = new GraphOwlVisitor(walker, graph, mappedPropertyList);
    walker.walkStructure(visitor);
    Map<String, String> categories = new HashMap<>();
    try (Transaction tx = graphDb.beginTx()) {
      OwlPostprocessor postprocessor = new OwlPostprocessor(graphDb, categories);
      postprocessor.processCategories(categories);
      postprocessor.processSomeValuesFrom();
      tx.success();
    }
  }

  @Test
  public void noExtraCreatedNodes() {
    try (Transaction tx = graphDb.beginTx()) {
      assertEquals(size(graphDb.getAllNodes()), 2);
      tx.success();
    }
  }

}
