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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.util.GraphTestBase;

public class OwlVisitorTest extends GraphTestBase {

  // static GraphDatabaseService graphDb;
  static Graph graph;
  static final String ROOT = "http://example.com/owl/families";
  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";

  @Before
  public void loadOwl() throws Exception {
    cleanup = false;
    // graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    graph = new Graph(graphDb);
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

      OwlVisitor visitor = new OwlVisitor(walker, graph, categoryMap, propertyMap);
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
      visitor.postProcess();
      tx.success();
    }
  }

  @Test
  public void testConcreteClassCreation() {
    assertThat(graph.nodeExists(ROOT + "/Mother"), is(true));
    Node mother = graph.getNode(ROOT + "/Mother").get();
    assertThat(mother.hasLabel(OwlLabels.OWL_CLASS), is(true));
  }

  @Test
  public void testAnonymousClassCreation() {
    Node complement = graph.getNode("http://ontology.neuinfo.org/anon/-1761792206").get();
    assertThat(graph.getOrCreateFramedNode(complement).isAnonymous(), is(true));
  }

  @Test
  public void testConcreteSubclass() {
    Node mother = graph.getNode(ROOT + "/Mother").get();
    Node woman = graph.getNode(ROOT + "/Woman").get();
    assertThat(
        graph.hasRelationship(mother, woman, OwlRelationships.RDFS_SUBCLASS_OF), is(true));
  }

  @Test
  public void testAnnotationAssertionAxiom() {
    Node person = graph.getNode(ROOT + "/Person").get();
    assertThat(
        graph.getProperty(person, "http://www.w3.org/2000/01/rdf-schema#comment", String.class)
            .get(), is("Represents the set of all people."));
  }

  @Test
  public void testMultiLanguageAnnotationAssertionAxiom() {
    Node person = graph.getNode(ROOT + "/Person").get();
    assertThat(graph
        .getProperty(person, "http://www.w3.org/2000/01/rdf-schema#label", String.class).get(),
        is("Person"));
  }

  @Test
  public void testNonLiteralAnnotationAssertionAxiom() {
    Node person = graph.getNode(ROOT + "/Person").get();
    Node fazz = graph.getNode(ROOT + "/Fazz").get();
    assertThat(
        graph.hasRelationship(person, fazz, DynamicRelationshipType.withName("fizz"), ROOT
            + "/fizz"), is(true));
  }

  @Test
  public void testNamedIndividualTypes() {
    assertThat(graph.nodeExists(ROOT + "/Bill"), is(true));
    Node john = graph.getNode(ROOT + "/John").get();
    Node father = graph.getNode(ROOT + "/Father").get();
    assertThat(graph.hasRelationship(john, father, OwlRelationships.RDF_TYPE), is(true));
    john.hasLabel(OwlLabels.OWL_NAMED_INDIVIDUAL);
  }

  @Test
  public void testSameIndividual() {
    Node james = graph.getNode(ROOT + "/James").get();
    Node jim = graph.getNode(ROOT + "/Jim").get();
    assertThat(graph.hasRelationship(james, jim, OwlRelationships.OWL_SAME_AS), is(true));
  }

  @Test
  public void testDifferentIndividual() {
    Node john = graph.getNode(ROOT + "/John").get();
    Node bill = graph.getNode(ROOT + "/Bill").get();
    assertThat(graph.hasRelationship(john, bill, OwlRelationships.OWL_DIFFERENT_FROM), is(true));
  }

  @Test
  public void testDataPropertyAssertions() {
    Node john = graph.getNode(ROOT + "/John").get();
    assertThat((Integer) john.getProperty(ROOT + "/hasAge"), is(51));
  }

  @Test
  public void testMappedDataPropertyAssertion() {
    Node john = graph.getNode(ROOT + "/John").get();
    assertThat((Integer) john.getProperty("isAged"), is(51));
  }

  @Test
  public void testObjectPropertyAssertions() {
    Node susan = graph.getNode(ROOT + "/Susan").get();
    Node meg = graph.getNode(ROOT + "/Meg").get();
    assertThat(
        graph.hasRelationship(susan, meg, DynamicRelationshipType.withName("hasAncestor")), is(true));
  }

  @Test
  public void testClassEquivalenceRelationships() {
    Node adult = graph.getNode(ROOT + "/Adult").get();
    Node grownup = graph.getNode(OTHER_ROOT + "/Grownup").get();
    assertThat(
        graph.hasRelationship(adult, grownup, OwlRelationships.OWL_EQUIVALENT_CLASS), is(true));
    assertThat(
        graph.hasRelationship(grownup, adult, OwlRelationships.OWL_EQUIVALENT_CLASS), is(true));
  }

  @Test
  public void testDisjointClasses() {
    Node man = graph.getNode(ROOT + "/Man").get();
    Node woman = graph.getNode(ROOT + "/Woman").get();
    assertThat(
        graph.hasRelationship(man, woman, OwlRelationships.OWL_DISJOINT_WITH), is(true));
  }

  @Test
  public void testObjectUnionOf() {
    Node parent = graph.getNode(ROOT + "/Parent").get();
    Node intersection = graph.getOrCreateNode("http://ontology.neuinfo.org/anon/412251922");
    assertThat(
        graph.hasRelationship(parent, intersection, OwlRelationships.OWL_EQUIVALENT_CLASS), is(true));
    Node mother = graph.getNode(ROOT + "/Mother").get();
    Node father = graph.getNode(ROOT + "/Father").get();
    assertThat(graph.hasRelationship(intersection, mother, OwlRelationships.OPERAND), is(true));
    assertThat(graph.hasRelationship(intersection, father, OwlRelationships.OPERAND), is(true));
  }

  @Test
  public void testObjectComplementOf() {
    Node parent = graph.getNode(ROOT + "/Parent").get();
    Node complement = graph.getNode("http://ontology.neuinfo.org/anon/-1761792206").get();
    assertThat(complement.hasLabel(OwlLabels.OWL_COMPLEMENT_OF), is(true));
    assertThat(graph.hasRelationship(complement, parent, OwlRelationships.OPERAND), is(true));
  }

  @Test
  public void testSubPropeties() {
    Node hasWife = graph.getNode(ROOT + "/hasWife").get();
    Node hasSpouse = graph.getNode(ROOT + "/hasSpouse").get();
    assertThat(graph.hasRelationship(hasWife, hasSpouse, OwlRelationships.RDFS_SUB_PROPERTY_OF), is(true));
  }

  @Test
  public void testChainedObjectProperties() {
    Node chain = graph.getNode(ROOT + "/hasUncle").get();
    Node father = graph.getNode(ROOT + "/hasFather").get();
    Node brother = graph.getNode(ROOT + "/hasBrother").get();
    assertThat(graph.hasRelationship(chain, father, OwlRelationships.RDFS_SUB_PROPERTY_OF), is(true));
    assertThat(graph.hasRelationship(chain, brother, OwlRelationships.RDFS_SUB_PROPERTY_OF), is(true));
    Relationship firstLink = graph.getOrCreateRelationship(chain, father, OwlRelationships.RDFS_SUB_PROPERTY_OF);
    Relationship secondLink = graph.getOrCreateRelationship(chain, brother, OwlRelationships.RDFS_SUB_PROPERTY_OF);
    assertThat(graph.getProperty(firstLink, "order", Integer.class).get(), is(0));
    assertThat(graph.getProperty(secondLink, "order", Integer.class).get(), is(1));
  }

  @Test
  public void testCardinalityRestrction() {
    Node restriction = graph.getNode("http://ontology.neuinfo.org/anon/-583677237").get();
    Node hasChild = graph.getNode(ROOT + "/hasChild").get();
    Node parent = graph.getNode(ROOT + "/Parent").get();
    assertThat(graph.getProperty(restriction, "cardinality", Integer.class).get(), is(2));
    assertThat(graph.hasRelationship(restriction, hasChild, OwlRelationships.PROPERTY), is(true));
    assertThat(graph.hasRelationship(restriction, parent, OwlRelationships.CLASS), is(true));
  }

  @Test
  public void testSomeValuesFrom() {
    Node svf = graph.getNode("http://ontology.neuinfo.org/anon/-1615296904").get();
    Node hasChild = graph.getNode(ROOT + "/hasChild").get();
    Node happyPerson = graph.getOrCreateNode(ROOT + "/HappyPerson");
    assertThat(graph.hasRelationship(svf, hasChild, OwlRelationships.PROPERTY), is(true));
    assertThat(graph.hasRelationship(svf, happyPerson, OwlRelationships.CLASS), is(true));
  }

  @Test
  public void testAllValuesFrom() {
    Node avf = graph.getNode("http://ontology.neuinfo.org/anon/-1615359878").get();
    Node hasChild = graph.getNode(ROOT + "/hasChild").get();
    Node happyPerson = graph.getNode(ROOT + "/HappyPerson").get();
    assertThat(graph.hasRelationship(avf, hasChild, OwlRelationships.PROPERTY), is(true));
    assertThat(graph.hasRelationship(avf, happyPerson, OwlRelationships.CLASS), is(true));
  }

  /*** http://www.w3.org/TR/owl2-new-features/#F12:_Punning */
  @Test
  public void testPunning() {
    Node eagle = graph.getOrCreateNode(ROOT + "/Eagle");
    assertThat(eagle.getLabels(), containsInAnyOrder(OwlLabels.OWL_CLASS, OwlLabels.OWL_NAMED_INDIVIDUAL));
  }

  @Test
  public void testDataProperties() {
    assertThat(graph.nodeExists(ROOT + "/hasAge"), is(true));
  }

  @Test
  public void testObjectProperties() {
    assertThat(graph.nodeExists(ROOT + "/hasParent"), is(true));
  }

}