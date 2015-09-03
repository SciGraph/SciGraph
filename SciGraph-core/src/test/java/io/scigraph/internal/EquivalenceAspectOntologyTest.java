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
package io.scigraph.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.OwlPostprocessor;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.util.GraphTestBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class EquivalenceAspectOntologyTest extends GraphTestBase {

  EquivalenceAspect aspect = new EquivalenceAspect(graphDb);

  @Before
  public void setup() throws Exception {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    String uri = Resources.getResource("ontologies/equivalence-cliques-test.owl").toURI().toString();
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
  public void edgesAreMovedToLeader() {
    Graph tinkerGraph = new TinkerGraph();
    GlobalGraphOperations globalGraphOperations = GlobalGraphOperations.at(graphDb);
    Node zfin1 = null;
    Node zfin2 = null;
    try (Transaction tx = graphDb.beginTx()) {
      for (Node n : globalGraphOperations.getAllNodes()) {
        tinkerGraph.addVertex(n);
        if (n.getProperty(NodeProperties.IRI).equals("http://www.ncbi.nlm.nih.gov/gene/ZG1")) {
          zfin1 = n;
        }
        if (n.getProperty(NodeProperties.IRI).equals("http://zfin.org/ZG1")) {
          zfin2 = n;
        }
        System.out.println(n);
        System.out.println(n.getProperty(NodeProperties.IRI));
        for (Relationship relationship : n.getRelationships()) {
          try {
            tinkerGraph.addVertex(relationship);
          } catch (Exception ex) {
            // ignore doublons
            // System.out.println(ex);
          }
          System.out.println("--- " + relationship.getType());
        }
      }
      
      assertThat(zfin1.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(6));
      assertThat(zfin2.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(6));

      aspect.invoke(tinkerGraph);

      System.out.println("===============================");
      for (Node n : globalGraphOperations.getAllNodes()) {
        System.out.println(n);
        System.out.println(n.getProperty(NodeProperties.IRI));
        for (Relationship relationship : n.getRelationships()) {
          System.out.println("--- " + relationship.getType());
        }
      }
      
      assertThat(zfin1.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(11));
      assertThat(zfin2.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(1));


      tx.success();
    }
  }

}
