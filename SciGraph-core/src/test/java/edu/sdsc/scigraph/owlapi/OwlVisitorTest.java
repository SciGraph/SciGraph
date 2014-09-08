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
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.EdgeProperties;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;
import edu.sdsc.scigraph.util.GraphTestBase;

public class OwlVisitorTest extends GraphTestBase {

  // static GraphDatabaseService graphDb;
  static Graph graph;
  static final String ROOT = "http://example.com/owl/families";
  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";

  @BeforeClass
  public static void loadOwl() throws Exception {
    cleanup = false;
    // graphDb = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    graph = new Graph(graphDb, Concept.class);
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
      walker.walkStructure(visitor);
      visitor.postProcess();
      tx.success();
    }
  }

  @Test
  public void testConcreteClassCreation() {
    assertThat(graph.nodeExists(ROOT + "/Mother"), is(true));
    assertThat(getOnlyElement(graph.getFramedNode(ROOT + "/Mother").get().getTypes()),
        is(equalTo("OWLClass")));
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
        graph.hasRelationship(mother, woman, EdgeType.SUBCLASS_OF, OwlVisitor.RDFS_PREFIX
            + "subClassOf"), is(true));
  }

  @Test
  public void testGeneratedInverseRelationship() {
    Node mother = graph.getNode(ROOT + "/Mother").get();
    Node woman = graph.getNode(ROOT + "/Woman").get();
    assertThat(graph.hasRelationship(woman, mother, EdgeType.SUPERCLASS_OF), is(true));
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
    Relationship r = graph.getOrCreateRelationship(person, fazz,
        DynamicRelationshipType.withName("fizz"));
    assertThat(graph.getProperty(r, CommonProperties.TYPE, String.class).get(),
        is("OWLAnnotationAssertionAxiom"));
  }

  @Test
  public void testNamedIndividualTypes() {
    assertThat(graph.nodeExists(ROOT + "/Bill"), is(true));
    Node john = graph.getNode(ROOT + "/John").get();
    Node father = graph.getNode(ROOT + "/Father").get();
    assertThat(graph.hasRelationship(john, father, EdgeType.IS_A), is(true));
    assertThat(graph.getProperties(john, CommonProperties.TYPE, String.class),
        contains("OWLIndividual"));
  }

  @Test
  public void testSameIndividual() {
    Node james = graph.getNode(ROOT + "/James").get();
    Node jim = graph.getNode(ROOT + "/Jim").get();
    assertThat(
        graph.hasRelationship(james, jim, EdgeType.SAME_AS, OwlVisitor.OWL_PREFIX + "sameAs"),
        is(true));
  }

  @Test
  public void testDifferentIndividual() {
    Node john = graph.getNode(ROOT + "/John").get();
    Node bill = graph.getNode(ROOT + "/Bill").get();
    assertThat(
        graph.hasRelationship(john, bill, EdgeType.DIFFERENT_FROM, OwlVisitor.OWL_PREFIX
            + "differentFrom"), is(true));
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
    Relationship relationship = graph.getOrCreateRelationship(susan, meg,
        DynamicRelationshipType.withName("hasAncestor"));
    assertThat(graph.getProperty(relationship, EdgeProperties.TRANSITIVE, Boolean.class).get(),
        is(true));
  }

  @Test
  public void testClassEquivalenceRelationships() {
    Node adult = graph.getNode(ROOT + "/Adult").get();
    Node grownup = graph.getNode(OTHER_ROOT + "/Grownup").get();
    assertThat(
        graph.hasRelationship(adult, grownup, EdgeType.EQUIVALENT_TO, OwlVisitor.OWL_PREFIX
            + "equivalentClass"), is(true));
    assertThat(
        graph.hasRelationship(grownup, adult, EdgeType.EQUIVALENT_TO, OwlVisitor.OWL_PREFIX
            + "equivalentClass"), is(true));
  }

  @Test
  public void testDisjointClasses() {
    Node man = graph.getNode(ROOT + "/Man").get();
    Node woman = graph.getNode(ROOT + "/Woman").get();
    assertThat(
        graph.hasRelationship(man, woman, EdgeType.DISJOINT_WITH, OwlVisitor.OWL_PREFIX
            + "disjointWith"), is(true));
  }

  @Test
  public void testObjectUnionOf() {
    Node parent = graph.getNode(ROOT + "/Parent").get();
    Node intersection = graph.getOrCreateNode("http://ontology.neuinfo.org/anon/412251922");
    assertThat(
        graph.hasRelationship(parent, intersection, EdgeType.EQUIVALENT_TO, OwlVisitor.OWL_PREFIX
            + "equivalentClass"), is(true));
    Node mother = graph.getNode(ROOT + "/Mother").get();
    Node father = graph.getNode(ROOT + "/Father").get();
    assertThat(graph.hasRelationship(intersection, mother, EdgeType.REL), is(true));
    assertThat(graph.hasRelationship(intersection, father, EdgeType.REL), is(true));
  }

  @Test
  public void testObjectComplementOf() {
    Node parent = graph.getNode(ROOT + "/Parent").get();
    Node complement = graph.getNode("http://ontology.neuinfo.org/anon/-1761792206").get();
    assertThat(graph.getProperty(complement, "type", String.class).get(),
        is(equalTo(OWLObjectComplementOf.class.getSimpleName())));
    assertThat(graph.hasRelationship(complement, parent, EdgeType.REL), is(true));
  }

  @Test
  public void testOntologyProperty() {
    Node parent = graph.getNode(ROOT + "/Parent").get();
    assertThat(graph.getProperty(parent, NodeProperties.ONTOLOGY, String.class).get(),
        is(equalTo(ROOT)));
  }

  @Test
  public void testParentOntologyProperty() {
    Node parent = graph.getNode(ROOT + "/Parent").get();
    assertThat(graph.getProperty(parent, NodeProperties.PARENT_ONTOLOGY, String.class).get(),
        is(equalTo(ROOT)));
  }

  @Test
  public void testSubPropeties() {
    Node hasWife = graph.getNode(ROOT + "/hasWife").get();
    Node hasSpouse = graph.getNode(ROOT + "/hasSpouse").get();
    assertThat(graph.hasRelationship(hasWife, hasSpouse, EdgeType.SUB_OBJECT_PROPETY_OF), is(true));
    assertThat(graph.hasRelationship(hasSpouse, hasWife, EdgeType.SUPER_OBJECT_PROPETY_OF),
        is(true));
  }

  @Test
  public void testChainedObjectProperties() {
    Node chain = graph.getNode(ROOT + "/hasUncle").get();
    Node father = graph.getNode(ROOT + "/hasFather").get();
    Node brother = graph.getNode(ROOT + "/hasBrother").get();
    assertThat(graph.hasRelationship(chain, father, EdgeType.REL), is(true));
    assertThat(graph.hasRelationship(chain, brother, EdgeType.REL), is(true));
    Relationship firstLink = graph.getOrCreateRelationship(chain, father, EdgeType.REL);
    Relationship secondLink = graph.getOrCreateRelationship(chain, brother, EdgeType.REL);
    assertThat(graph.getProperty(firstLink, "order", Integer.class).get(), is(0));
    assertThat(graph.getProperty(secondLink, "order", Integer.class).get(), is(1));
  }

  @Test
  public void testCardinalityRestrction() {
    Node restriction = graph.getNode("http://ontology.neuinfo.org/anon/-583677237").get();
    Node hasChild = graph.getNode(ROOT + "/hasChild").get();
    Node parent = graph.getNode(ROOT + "/Parent").get();
    assertThat(graph.getProperty(restriction, "cardinality", Integer.class).get(), is(2));
    assertThat(graph.hasRelationship(restriction, hasChild, EdgeType.PROPERTY), is(true));
    assertThat(graph.hasRelationship(restriction, parent, EdgeType.CLASS), is(true));
  }

  @Test
  public void testSomeValuesFrom() {
    Node svf = graph.getNode("http://ontology.neuinfo.org/anon/-1615296904").get();
    Node hasChild = graph.getNode(ROOT + "/hasChild").get();
    Node happyPerson = graph.getOrCreateNode(ROOT + "/HappyPerson");
    assertThat(graph.hasRelationship(svf, hasChild, EdgeType.PROPERTY), is(true));
    assertThat(graph.hasRelationship(svf, happyPerson, EdgeType.CLASS), is(true));
  }

  @Test
  public void testAllValuesFrom() {
    Node avf = graph.getNode("http://ontology.neuinfo.org/anon/-1615359878").get();
    Node hasChild = graph.getNode(ROOT + "/hasChild").get();
    Node happyPerson = graph.getNode(ROOT + "/HappyPerson").get();
    assertThat(graph.hasRelationship(avf, hasChild, EdgeType.PROPERTY), is(true));
    assertThat(graph.hasRelationship(avf, happyPerson, EdgeType.CLASS), is(true));
  }

  @Test
  public void testSomeValuesFromPostProcess() {
    Node woman = graph.getNode(ROOT + "/Woman").get();
    Node happyPerson = graph.getNode(ROOT + "/HappyPerson").get();
    assertThat(graph.hasRelationship(woman, happyPerson,
        DynamicRelationshipType.withName("hasChild"), ROOT + "/hasChild"), is(true));
  }

  @Test
  @Ignore
  public void testEquivalencePostProcess() {
    Node adult = graph.getNode(ROOT + "/Adult").get();
    Node grownUp = graph.getNode(OTHER_ROOT + "/Grownup").get();
    Node nonchild = graph.getNode(OTHER_ROOT + "/Nonchild").get();
    assertThat(graph.hasRelationship(adult, grownUp, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(grownUp, adult, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(adult, nonchild, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(nonchild, adult, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(nonchild, grownUp, EdgeType.EQUIVALENT_TO), is(true));
    assertThat(graph.hasRelationship(grownUp, nonchild, EdgeType.EQUIVALENT_TO), is(true));
  }

  /*** http://www.w3.org/TR/owl2-new-features/#F12:_Punning */
  @Test
  public void testPunning() {
    Node eagle = graph.getOrCreateNode(ROOT + "/Eagle");
    assertThat(graph.getProperties(eagle, NodeProperties.TYPE, String.class),
        containsInAnyOrder("OWLClass", "OWLIndividual"));
  }

  @Test
  public void testDataProperties() {
    assertThat(graph.nodeExists(ROOT + "/hasAge"), is(true));
    assertThat(graph.getFramedNode(ROOT + "/hasAge").get().getTypes(), contains("OWLDataProperty"));
  }

  @Test
  public void testObjectProperties() {
    assertThat(graph.nodeExists(ROOT + "/hasParent"), is(true));
    assertThat(graph.getFramedNode(ROOT + "/hasParent").get().getTypes(),
        contains("OWLObjectProperty"));
  }

}