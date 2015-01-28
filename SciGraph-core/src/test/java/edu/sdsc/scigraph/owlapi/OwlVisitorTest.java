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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.base.Optional;
import com.google.common.io.Resources;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.GraphInterface;
import edu.sdsc.scigraph.neo4j.GraphInterfaceTransactionImpl;
import edu.sdsc.scigraph.neo4j.RelationshipMap;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.util.GraphTestBase;

public class OwlVisitorTest extends GraphTestBase {

  // static GraphDatabaseService graphDb;
  static GraphInterface graph;
  static final String ROOT = "http://example.com/owl/families";
  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";

  @Before
  public void loadOwl() throws Exception {
    cleanup = false;
    // graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    graph = new GraphInterfaceTransactionImpl(graphDb, new ConcurrentHashMap<String, Long>(), new RelationshipMap());
    String uri = Resources.getResource("ontologies/family.owl").toURI().toString();
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);

    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
    Map<String, String> categoryMap = new HashMap<>();
    try (Transaction tx = graphDb.beginTx()) {
      List<MappedProperty> propertyMap = new ArrayList<>();
      MappedProperty age = mock(MappedProperty.class);
      when(age.getName()).thenReturn("isAged");
      when(age.getProperties()).thenReturn(newArrayList(ROOT + "/hasAge"));
      propertyMap.add(age);

      GraphOwlVisitor visitor = new GraphOwlVisitor(walker, graph, propertyMap);
      for (OWLOntology ontology: manager.getOntologies()) {
        for (OWLObject object: ontology.getNestedClassExpressions()) {
          object.accept(visitor);
        }
        for (OWLObject entity: ontology.getSignature(true)) {
          entity.accept(visitor);
        }
        for (OWLObject axiom: ontology.getAxioms()) {
          axiom.accept(visitor);
        }

      }  
      //walker.walkStructure(visitor);
      // TODO: add this back visitor.postProcess();
      tx.success();
    }
  }

  @Test
  public void classesAreCreated() {
    long mother = graph.getNode(ROOT + "/Mother").get();
    assertThat(graph.getLabels(mother), hasItem(OwlLabels.OWL_CLASS));
    assertThat(graph.getNodeProperty(mother, CommonProperties.URI, String.class).get(), is(ROOT + "/Mother"));
  }

  @Test
  public void testAnonymousClassCreation() {
    long complement = graph.getNode("http://ontology.neuinfo.org/anon/-1761792206").get();
    assertThat(graph.getLabels(complement), hasItem(OwlLabels.OWL_ANONYMOUS));
  }

  @Test
  public void testConcreteSubclass() {
    long mother = graph.getNode(ROOT + "/Mother").get();
    long woman = graph.getNode(ROOT + "/Woman").get();
    assertThat(
        graph.getRelationship(mother, woman, OwlRelationships.RDFS_SUBCLASS_OF).isPresent(), is(true));
  }

  @Test
  public void testAnnotationAssertionAxiom() {
    long person = graph.getNode(ROOT + "/Person").get();
    assertThat(
        graph.getNodeProperty(person, "http://www.w3.org/2000/01/rdf-schema#comment", String.class)
        .get(), is("Represents the set of all people."));
  }

  @Test
  public void testMultiLanguageAnnotationAssertionAxiom() {
    long person = graph.getNode(ROOT + "/Person").get();
    assertThat(graph
        .getNodeProperty(person, "http://www.w3.org/2000/01/rdf-schema#label", String.class).get(),
        is("Person"));
  }

  @Test
  public void testNonLiteralAnnotationAssertionAxiom() {
    long person = graph.getNode(ROOT + "/Person").get();
    long fazz = graph.getNode(ROOT + "/Fazz").get();
    Optional<Long> relationship = graph.getRelationship(person, fazz, DynamicRelationshipType.withName("fizz"));
    assertThat(relationship.isPresent(), is(true));
    assertThat(graph.getRelationshipProperty(relationship.get(), CommonProperties.URI, String.class).get(),
        is(ROOT + "/fizz"));
  }

  @Test
  public void testNamedIndividualTypes() {
    assertThat(graph.getNode(ROOT + "/Bill").isPresent(), is(true));
    long john = graph.getNode(ROOT + "/John").get();
    long father = graph.getNode(ROOT + "/Father").get();
    assertThat(graph.getRelationship(john, father, OwlRelationships.RDF_TYPE).isPresent(), is(true));
    assertThat(graph.getLabels(john), contains(OwlLabels.OWL_NAMED_INDIVIDUAL));
  }

  @Test
  public void testSameIndividual() {
    long james = graph.getNode(ROOT + "/James").get();
    long jim = graph.getNode(ROOT + "/Jim").get();
    assertThat(graph.getRelationship(james, jim, OwlRelationships.OWL_SAME_AS).isPresent(), is(true));
  }

  @Test
  public void testDifferentIndividual() {
    long john = graph.getNode(ROOT + "/John").get();
    long bill = graph.getNode(ROOT + "/Bill").get();
    assertThat(graph.getRelationship(bill, john, OwlRelationships.OWL_DIFFERENT_FROM).isPresent(), is(true));
  }

  @Test
  public void testDataPropertyAssertions() {
    long john = graph.getNode(ROOT + "/John").get();
    assertThat(graph.getNodeProperty(john, ROOT + "/hasAge", String.class).get(), is("51"));
  }

  @Test
  public void testMappedDataPropertyAssertion() {
    long john = graph.getNode(ROOT + "/John").get();
    assertThat(graph.getNodeProperty(john, "isAged", String.class).get(), is("51"));
  }

  @Test
  public void testObjectPropertyAssertions() {
    long susan = graph.getNode(ROOT + "/Susan").get();
    long meg = graph.getNode(ROOT + "/Meg").get();
    assertThat(
        graph.getRelationship(susan, meg, DynamicRelationshipType.withName("hasAncestor")).isPresent(), is(true));
  }

  @Test
  public void testClassEquivalenceRelationships() {
    long adult = graph.getNode(ROOT + "/Adult").get();
    long grownup = graph.getNode(OTHER_ROOT + "/Grownup").get();
    assertThat(
        graph.getRelationship(adult, grownup, OwlRelationships.OWL_EQUIVALENT_CLASS).isPresent(), is(true));
  }

  @Test
  public void testDisjointClasses() {
    long man = graph.getNode(ROOT + "/Man").get();
    long woman = graph.getNode(ROOT + "/Woman").get();
    assertThat(
        graph.getRelationship(man, woman, OwlRelationships.OWL_DISJOINT_WITH).isPresent(), is(true));
  }

  @Test
  public void testObjectUnionOf() {
    long parent = graph.getNode(ROOT + "/Parent").get();
    long intersection = graph.getNode("http://ontology.neuinfo.org/anon/412251922").get();
    assertThat(
        graph.getRelationship(parent, intersection, OwlRelationships.OWL_EQUIVALENT_CLASS).isPresent(), is(true));
    long mother = graph.getNode(ROOT + "/Mother").get();
    long father = graph.getNode(ROOT + "/Father").get();
    assertThat(graph.getRelationship(intersection, mother, OwlRelationships.OPERAND).isPresent(), is(true));
    assertThat(graph.getRelationship(intersection, father, OwlRelationships.OPERAND).isPresent(), is(true));
  }

  @Test
  public void testObjectComplementOf() {
    long parent = graph.getNode(ROOT + "/Parent").get();
    long complement = graph.getNode("http://ontology.neuinfo.org/anon/-1761792206").get();
    assertThat(graph.getLabels(complement), hasItem(OwlLabels.OWL_COMPLEMENT_OF));
    assertThat(graph.getRelationship(complement, parent, OwlRelationships.OPERAND).isPresent(), is(true));
  }

  @Test
  public void testSubPropeties() {
    long hasWife = graph.getNode(ROOT + "/hasWife").get();
    long hasSpouse = graph.getNode(ROOT + "/hasSpouse").get();
    assertThat(graph.getRelationship(hasWife, hasSpouse, OwlRelationships.RDFS_SUB_PROPERTY_OF).isPresent(), is(true));
  }

  @Test
  public void testChainedObjectProperties() {
    long chain = graph.getNode(ROOT + "/hasUncle").get();
    long father = graph.getNode(ROOT + "/hasFather").get();
    long brother = graph.getNode(ROOT + "/hasBrother").get();
    long firstLink = graph.getRelationship(chain, father, OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM).get();
    long secondLink = graph.getRelationship(chain, brother, OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM).get();
    assertThat(graph.getRelationshipProperty(firstLink, "order", Integer.class).get(), is(0));
    assertThat(graph.getRelationshipProperty(secondLink, "order", Integer.class).get(), is(1));
  }

  @Test
  public void testCardinalityRestrction() {
    long restriction = graph.getNode("http://ontology.neuinfo.org/anon/-583677237").get();
    long hasChild = graph.getNode(ROOT + "/hasChild").get();
    long parent = graph.getNode(ROOT + "/Parent").get();
    assertThat(graph.getNodeProperty(restriction, "cardinality", Integer.class).get(), is(2));
    assertThat(graph.getRelationship(restriction, hasChild, OwlRelationships.PROPERTY).isPresent(), is(true));
    assertThat(graph.getRelationship(restriction, parent, OwlRelationships.CLASS).isPresent(), is(true));
  }

  @Test
  public void testSomeValuesFrom() {
    long svf = graph.getNode("http://ontology.neuinfo.org/anon/-1615296904").get();
    long hasChild = graph.getNode(ROOT + "/hasChild").get();
    long happyPerson = graph.getNode(ROOT + "/HappyPerson").get();
    assertThat(graph.getRelationship(svf, hasChild, OwlRelationships.PROPERTY).isPresent(), is(true));
    assertThat(graph.getRelationship(svf, happyPerson, OwlRelationships.FILLER).isPresent(), is(true));
  }

  @Test
  public void testAllValuesFrom() {
    long avf = graph.getNode("http://ontology.neuinfo.org/anon/-1615359878").get();
    long hasChild = graph.getNode(ROOT + "/hasChild").get();
    long happyPerson = graph.getNode(ROOT + "/HappyPerson").get();
    assertThat(graph.getRelationship(avf, hasChild, OwlRelationships.PROPERTY).isPresent(), is(true));
    assertThat(graph.getRelationship(avf, happyPerson, OwlRelationships.FILLER).isPresent(), is(true));
  }

  /*** http://www.w3.org/TR/owl2-new-features/#F12:_Punning */
  @Test
  public void testPunning() {
    long eagle = graph.getNode(ROOT + "/Eagle").get();
    assertThat(graph.getLabels(eagle), containsInAnyOrder(OwlLabels.OWL_CLASS, OwlLabels.OWL_NAMED_INDIVIDUAL));
  }

  @Test
  public void testDataProperties() {
    assertThat(graph.getNode(ROOT + "/hasAge").isPresent(), is(true));
  }

  @Test
  public void testObjectProperties() {
    assertThat(graph.getNode(ROOT + "/hasParent").isPresent(), is(true));
  }

}