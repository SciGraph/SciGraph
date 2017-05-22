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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.scigraph.frames.CommonProperties;
import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.owlapi.GraphOwlVisitor;
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationAssertionAxiomImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

public abstract class GraphOwlVisitorTestBase<T extends Graph> {

  static final String ROOT = "http://example.com/owl/families";
  static final String OWL = "http://www.w3.org/2002/07/owl#";
  static final String VERSION_IRI = "http://example.com/owl/families/2017-10-05/family.owl";
  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";

  @ClassRule
  public static TemporaryFolder folder = new TemporaryFolder();

  private T graph;

  static String path;

  static boolean builtGraph = false;

  static GraphDatabaseService graphDb;
  static ReadableIndex<Node> nodeIndex;

  static {
    try {
      folder.create();
      path = folder.newFolder().getAbsolutePath();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

  protected abstract T createInstance() throws Exception;

  static Transaction tx;

  @Before
  public void setup() throws Exception {
    if (builtGraph) {
      // TODO: UGH - need a better pattern for this
      return;
    }
    graph = createInstance();

    String uri = Resources.getResource("ontologies/family.owl").toURI().toString();
    manager.loadOntologyFromOntologyDocument(IRI.create(uri));

    List<MappedProperty> propertyMap = new ArrayList<>();
    MappedProperty age = mock(MappedProperty.class);
    when(age.getName()).thenReturn("isAged");
    when(age.getProperties()).thenReturn(newArrayList(ROOT + "/hasAge"));
    propertyMap.add(age);
    MappedProperty versionInfo = mock(MappedProperty.class);
    when(versionInfo.getName()).thenReturn("versionInfo");
    when(versionInfo.getProperties()).thenReturn(newArrayList(OWL + "versionInfo"));
    propertyMap.add(versionInfo);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
    GraphOwlVisitor visitor = new GraphOwlVisitor(walker, graph, propertyMap);
    walker.walkStructure(visitor);
    
    for (OWLOntology ontology : manager.getOntologies()){
      String ontologyIri = OwlApiUtils.getIri(ontology);
      for (OWLAnnotation annotation : ontology.getAnnotations()) { // Get annotations on ontology iri
        OWLAnnotationSubject ontologySubject = IRI.create(ontologyIri);
        OWLAnnotationAssertionAxiom object = 
            new OWLAnnotationAssertionAxiomImpl(ontologySubject,
                annotation.getProperty(), annotation.getValue(),
                new ArrayList<OWLAnnotation>());
        visitor.visit(object);
      }
    }
    graph.shutdown();
    graphDb = new TestGraphDatabaseFactory().newEmbeddedDatabase(new File(path));
    tx = graphDb.beginTx();
    nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
    builtGraph = true;
  }

  Node getNode(String uri) {
    return nodeIndex.get(CommonProperties.IRI, uri).getSingle();
  }

  static boolean hasRelationship(Node node1, Node node2, RelationshipType type) {
    Iterable<Relationship> relationships = GraphUtil.getRelationships(node1, node2, type);
    return size(relationships) == 1;
  }

  static boolean hasDirectedRelationship(Node node1, Node node2, RelationshipType type) {
    Iterable<Relationship> relationships = GraphUtil.getRelationships(node1, node2, type, true);
    return size(relationships) == 1;
  }

  static Node getLabeledOtherNode(Node node, RelationshipType type, Label label) {
    Iterable<Relationship> relationships = node.getRelationships(type);
    for (Relationship relationship: relationships) {
      if (relationship.getOtherNode(node).hasLabel(label)) {
        return relationship.getOtherNode(node);
      }
    }
    return null;
  }

  @Test
  public void uriIndexesAreCreated() {
    Node mother = getNode(ROOT + "/Mother");
    assertThat(GraphUtil.getProperty(mother, CommonProperties.IRI, String.class).get(), is(ROOT
        + "/Mother"));
  }

  @Test
  public void ontologyNodesIsCreated() {
    Node mother = getNode(ROOT);
    assertThat(mother.getLabels(), hasItem(OwlLabels.OWL_ONTOLOGY));
  }

  @Test
  public void nodesHaveDefinedBy() {
    Node mother = getNode(ROOT + "/Mother");
    Node ontology = getNode(ROOT);
    assertThat(hasRelationship(mother, ontology, OwlRelationships.RDFS_IS_DEFINED_BY), is(true));
  }

  @Test
  public void relationshipsHaveDefinedBy() {
    Node mother = getNode(ROOT + "/Mother");
    Relationship r = getOnlyElement(mother.getRelationships(OwlRelationships.RDFS_SUBCLASS_OF, Direction.OUTGOING));
    assertThat(GraphUtil.getProperties(r, OwlRelationships.RDFS_IS_DEFINED_BY.name(), String.class), contains(ROOT));
  }
  
  @Test
  public void classesAreCreated() {
    Node mother = getNode(ROOT + "/Mother");
    assertThat(mother.getLabels(), hasItem(OwlLabels.OWL_CLASS));
    assertThat(GraphUtil.getProperty(mother, CommonProperties.IRI, String.class).get(), is(ROOT
        + "/Mother"));
  }

  @Test
  public void anonymousClassesAreCreated() {
    Iterator<Node> nodes = graphDb.findNodes(OwlLabels.OWL_ANONYMOUS);
    assertThat(nodes.hasNext(), is(true));
    while (nodes.hasNext()) {
      Node node = nodes.next();
      assertThat(GraphUtil.getProperty(node, CommonProperties.IRI, String.class).get(), startsWith("_:"));
    }
      
    
    //assertThat(complement.getLabels(), hasItem(OwlLabels.OWL_ANONYMOUS));
  }

  @Test
  public void subclassesAreCreated() {
    Node mother = getNode(ROOT + "/Mother");
    Node woman = getNode(ROOT + "/Woman");
    assertThat(hasDirectedRelationship(mother, woman, OwlRelationships.RDFS_SUBCLASS_OF), is(true));
  }

  @Test
  public void annotationAssertionAxiom() {
    Node person = getNode(ROOT + "/Person");
    assertThat(
        GraphUtil.getProperty(person, "http://www.w3.org/2000/01/rdf-schema#comment", String.class).get(),
        is("Represents the set of all people."));
  }

  @Test
  public void multiLanguageAnnotationAssertionAxiom() {
    Node person = getNode(ROOT + "/Person");
    assertThat(
        GraphUtil.getProperty(person, "http://www.w3.org/2000/01/rdf-schema#label", String.class).get(),
        is("Person"));
  }

  @Test
  public void nonLiteralAnnotationAssertionAxiom() {
    Node person = getNode(ROOT + "/Person");
    Node fazz = getNode(ROOT + "/Fazz");
    Relationship relationship = getOnlyElement(GraphUtil.getRelationships(person, fazz, RelationshipType.withName(ROOT + "/fizz"), true));
    assertThat(GraphUtil.getProperty(relationship, CommonProperties.IRI, String.class).get(),
        is(ROOT + "/fizz"));
  }

  @Test
  public void namedIndividualTypes() {
    Node john = getNode(ROOT + "/John");
    Node father = getNode(ROOT + "/Father");
    assertThat(john.getLabels(), contains(OwlLabels.OWL_NAMED_INDIVIDUAL));
    assertThat(hasDirectedRelationship(john, father, OwlRelationships.RDF_TYPE), is(true));
  }

  @Test
  public void sameIndividual() {
    Node james = getNode(ROOT + "/James");
    Node jim = getNode(ROOT + "/Jim");
    assertThat(hasRelationship(james, jim, OwlRelationships.OWL_SAME_AS), is(true));
  }

  @Test
  public void differentFrom() {
    Node john = getNode(ROOT + "/John");
    Node bill = getNode(ROOT + "/Bill");
    assertThat(hasRelationship(bill, john, OwlRelationships.OWL_DIFFERENT_FROM), is(true));
  }

  @Test
  public void dataPropertyAssertions() {
    Node john = getNode(ROOT + "/John");
    assertThat(GraphUtil.getProperty(john, ROOT + "/hasAge", Integer.class).get(), is(51));
  }

  @Test
  public void mappedDataPropertyAssertion() {
    Node john = getNode(ROOT + "/John");
    assertThat(GraphUtil.getProperty(john, "isAged", Integer.class).get(), is(51));
  }
  
  @Test
  public void mappedAnnotationPropertyOnOntologyAssertion() {
    Node ontology = getNode(ROOT);
    assertThat(GraphUtil.getProperty(ontology, "versionInfo", String.class).get(), is("version 4.0"));
  }

  @Test
  public void objectPropertyAssertions() {
    Node susan = getNode(ROOT + "/Susan");
    Node meg = getNode(ROOT + "/Meg");
    assertThat(hasDirectedRelationship(susan, meg, RelationshipType.withName(ROOT + "/#hasAncestor")), is(true));
  }

  @Test
  public void classEquivalenceRelationships() {
    Node adult = getNode(ROOT + "/Adult");
    Node grownup = getNode(OTHER_ROOT + "/Grownup");
    assertThat(hasRelationship(adult, grownup, OwlRelationships.OWL_EQUIVALENT_CLASS), is(true));
  }

  @Test
  public void disjointClasses() {
    Node man = getNode(ROOT + "/Man");
    Node woman = getNode(ROOT + "/Woman");
    assertThat(hasRelationship(man, woman, OwlRelationships.OWL_DISJOINT_WITH), is(true));
  }

  @Test
  public void objectUnionOf() {
    Node parent = getNode(ROOT + "/Parent");
    Node union = getLabeledOtherNode(parent, OwlRelationships.OWL_EQUIVALENT_CLASS, OwlLabels.OWL_UNION_OF);
    assertThat(union.hasLabel(OwlLabels.OWL_ANONYMOUS), is(true));
    Node mother = getNode(ROOT + "/Mother");
    Node father = getNode(ROOT + "/Father");
    assertThat(hasDirectedRelationship(union, mother, OwlRelationships.OPERAND), is(true));
    assertThat(hasDirectedRelationship(union, father, OwlRelationships.OPERAND), is(true));
  }

  @Test
  public void objectComplementOf() {
    Node parent = getNode(ROOT + "/Parent");
    Node complement = getLabeledOtherNode(parent, OwlRelationships.OPERAND, OwlLabels.OWL_COMPLEMENT_OF);
    assertThat(complement.getLabels(), hasItem(OwlLabels.OWL_ANONYMOUS));
  }

  @Test
  public void testSubPropeties() {
    Node hasWife = getNode(ROOT + "/hasWife");
    Node hasSpouse = getNode(ROOT + "/hasSpouse");
    assertThat(hasDirectedRelationship(hasWife, hasSpouse, OwlRelationships.RDFS_SUB_PROPERTY_OF), is(true));
  }

  @Test
  public void testEquivalentProperties() {
    Node hasChild1 = getNode(ROOT + "/hasChild");
    Node hasChild2 = getNode(OTHER_ROOT + "/child");
    assertThat(hasRelationship(hasChild1, hasChild2, OwlRelationships.OWL_EQUIVALENT_OBJECT_PROPERTY), is(true));
  }

  @Test
  public void chainedObjectProperties() {
    Node chain = getNode(ROOT + "/hasUncle");
    Node father = getNode(ROOT + "/hasFather");
    Node brother = getNode(ROOT + "/hasBrother");
    Relationship firstLink = getOnlyElement(GraphUtil.getRelationships(chain, father, OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM, true));
    Relationship secondLink = getOnlyElement(GraphUtil.getRelationships(chain, brother, OwlRelationships.OWL_PROPERTY_CHAIN_AXIOM, true));
    assertThat(GraphUtil.getProperty(firstLink, "order", Integer.class).get(), is(0));
    assertThat(GraphUtil.getProperty(secondLink, "order", Integer.class).get(), is(1));
  }

  @Test
  public void cardinalityRestrction() {
    Node hasChild = getNode(ROOT + "/hasChild");
    Node parent = getNode(ROOT + "/Parent");
    Node restriction = getLabeledOtherNode(parent, OwlRelationships.CLASS, OwlLabels.OWL_MIN_CARDINALITY);
    assertThat(restriction.getLabels(), hasItem(OwlLabels.OWL_ANONYMOUS));
    assertThat(GraphUtil.getProperty(restriction, "cardinality", Integer.class).get(), is(2));
    assertThat(hasDirectedRelationship(restriction, hasChild, OwlRelationships.PROPERTY), is(true));
    assertThat(hasDirectedRelationship(restriction, parent, OwlRelationships.CLASS), is(true));
  }

  @Test
  public void someValuesFrom() {
    Node hasChild = getNode(ROOT + "/hasChild");
    Node happyPerson = getNode(ROOT + "/HappyPerson");
    Node svf = getLabeledOtherNode(happyPerson, OwlRelationships.FILLER, OwlLabels.OWL_SOME_VALUES_FROM);
    assertThat(svf.getLabels(), hasItem(OwlLabels.OWL_ANONYMOUS));
    assertThat(hasDirectedRelationship(svf, hasChild, OwlRelationships.PROPERTY), is(true));
    assertThat(hasDirectedRelationship(svf, happyPerson, OwlRelationships.FILLER), is(true));
  }

  @Test
  public void allValuesFrom() {
    Node hasChild = getNode(ROOT + "/hasChild");
    Node happyPerson = getNode(ROOT + "/HappyPerson");
    Node avf = getLabeledOtherNode(happyPerson, OwlRelationships.FILLER, OwlLabels.OWL_ALL_VALUES_FROM);
    assertThat(avf.getLabels(), hasItem(OwlLabels.OWL_ANONYMOUS));
    assertThat(hasDirectedRelationship(avf, hasChild, OwlRelationships.PROPERTY), is(true));
    assertThat(hasDirectedRelationship(avf, happyPerson, OwlRelationships.FILLER), is(true));
  }

  /*** http://www.w3.org/TR/owl2-new-features/#F12:_Punning */
  @Test
  public void punning() {
    Node eagle = getNode(ROOT + "/Eagle");
    assertThat(eagle.getLabels(), containsInAnyOrder(OwlLabels.OWL_CLASS, OwlLabels.OWL_NAMED_INDIVIDUAL));
  }

  @Test
  public void dataProperties() {
    Node hasAge = getNode(ROOT + "/hasAge");
    assertThat(hasAge.hasLabel(OwlLabels.OWL_DATA_PROPERTY), is(true));
  }

  @Test
  public void objectProperties() {
    Node hasParent = getNode(ROOT + "/hasParent");
    assertThat(hasParent.hasLabel(OwlLabels.OWL_OBJECT_PROPERTY), is(true));
  }
  
  @Test
  public void versionIRI() {
    Node ontology = getNode(ROOT);
    Node version = getNode(VERSION_IRI);
    assertThat(hasRelationship(ontology, version, OwlRelationships.OWL_VERSION_IRI), is(true));
    
  }


}
