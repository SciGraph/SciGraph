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
package io.scigraph.owlapi.postprocessors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.OwlPostprocessor;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.util.GraphTestBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

public class CliqueOntologyTest extends GraphTestBase {

  Clique clique;

  @Before
  public void setup() throws Exception {
    CliqueConfiguration cliqueConfiguration = new CliqueConfiguration();
    Set<String> rel = new HashSet<String>();
    rel.add(OwlRelationships.OWL_EQUIVALENT_CLASS.name());
    cliqueConfiguration.setRelationships(rel);
    cliqueConfiguration.setLeaderAnnotation("http://www.monarchinitiative.org/MONARCH_cliqueLeader");
    clique = new Clique(graphDb, cliqueConfiguration);

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
    Node zfin1 = null;
    Node zfin2 = null;
    Node phenotype1 = null;
    Node phenotype2 = null;
    Node phenotype3 = null;
    try (Transaction tx = graphDb.beginTx()) {
      for (Node n : graphDb.getAllNodes()) {
        if (n.getProperty(NodeProperties.IRI).equals("http://www.ncbi.nlm.nih.gov/gene/ZG1")) {
          zfin1 = n;
        }
        if (n.getProperty(NodeProperties.IRI).equals("http://zfin.org/ZG1")) {
          zfin2 = n;
        }
        if (n.getProperty(NodeProperties.IRI).equals("http://purl.obolibrary.org/obo/ZP_0000001")) {
          phenotype1 = n;
        }
        if (n.getProperty(NodeProperties.IRI).equals("http://purl.obolibrary.org/obo/MP_0000001")) {
          phenotype2 = n;
        }
        if (n.getProperty(NodeProperties.IRI).equals("http://purl.obolibrary.org/obo/HP_0000001")) {
          phenotype3 = n;
        }
      }

      assertThat(zfin1.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(6));
      assertThat(zfin2.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(6));
      assertThat(phenotype1.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(4));
      assertThat(phenotype2.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(4));
      assertThat(phenotype3.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(8));

      clique.run();

      assertThat(zfin2.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(11));
      assertThat(zfin1.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(1));
      assertThat(phenotype1.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(1));
      assertThat(phenotype2.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(1));
      assertThat(phenotype3.getRelationships(), IsIterableWithSize.<Relationship>iterableWithSize(14));
      assertThat(zfin2.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(true));
      assertThat(zfin1.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(false));
      assertThat(phenotype1.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(false));
      assertThat(phenotype2.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(false));
      assertThat(phenotype3.hasLabel(Clique.CLIQUE_LEADER_LABEL), is(true));
      


      tx.success();
    }
  }

}
