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
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.visualization.graphviz.GraphvizWriter;
import org.neo4j.walk.Walker;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;

import com.google.common.io.Resources;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.neo4j.BatchGraph;
import edu.sdsc.scigraph.neo4j.EdgeType;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.neo4j.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlLoadConfiguration.MappedProperty;

public class BatchOwlVisitorTest {

  static GraphDatabaseService graphDb;
  static ReadableIndex<Node> nodeIndex;
  static BatchGraph batchGraph;
  static Path path;
  static final String ROOT = "http://example.com/owl/families";
  static final String OTHER_ROOT = "http://example.org/otherOntologies/families";

  @BeforeClass
  public static void loadOwl() throws Exception {
    path = Files.createTempDirectory("SciGraph-BatchTest");
    System.out.println(path.toAbsolutePath().toString());
    BatchInserter inserter = BatchInserters.inserter(path.toFile().getAbsolutePath());
    batchGraph = new BatchGraph(inserter, CommonProperties.URI, newHashSet(CommonProperties.URI,
        NodeProperties.LABEL, NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX,
        CommonProperties.CURIE, CommonProperties.ONTOLOGY, CommonProperties.FRAGMENT,
        Concept.CATEGORY, Concept.SYNONYM, Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX),
        newHashSet(""));
    String uri = Resources.getResource("ontologies/family.owl").toURI().toString();
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    IRI iri = IRI.create(uri);
    manager.loadOntologyFromOntologyDocument(iri);
    OWLOntologyWalker walker = new OWLOntologyWalker(manager.getOntologies());
    Map<String, String> curieMap = new HashMap<>();
    curieMap.put("http://example.org/otherOntologies/families/", "otherOnt");
    List<MappedProperty> propertyMap = new ArrayList<>();
    MappedProperty age = mock(MappedProperty.class);
    when(age.getName()).thenReturn("isAged");
    when(age.getProperties()).thenReturn(newArrayList(ROOT + "/hasAge"));
    propertyMap.add(age);

    BatchOwlVisitor visitor = new BatchOwlVisitor(walker, batchGraph, curieMap, propertyMap);
    walker.walkStructure(visitor);

    batchGraph.shutdown();
    graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path.toString());
    nodeIndex = graphDb.index().getNodeAutoIndexer().getAutoIndex();
    graphDb.beginTx();
  }

  @AfterClass
  public static void teardown() throws IOException {
    FileUtils.deleteDirectory(path.toFile());
  }

  Node getNode(String uri) {
    return nodeIndex.get(CommonProperties.URI, uri).getSingle();
  }

  @Test
  public void testConcreteClassCreation() {
    Node mother = getNode(ROOT + "/Mother");
    assertThat(GraphUtil.getProperty(mother, CommonProperties.URI, String.class).get(), is(ROOT
        + "/Mother"));
  }

  @Test
  public void testClassLabel() {
    Node mother = getNode(ROOT + "/Mother");
    assertThat(mother.hasLabel(OwlLabels.OWL_CLASS), is(true));
  }

  @Test
  public void testFragment() {
    Node mother = getNode(ROOT + "/Mother");
    assertThat(GraphUtil.getProperty(mother, CommonProperties.FRAGMENT, String.class).get(),
        is("Mother"));
  }

  @Test
  public void testConcreteSubclass() {
    Node mother = getNode(ROOT + "/Mother");
    Node woman = getNode(ROOT + "/Woman");
    assertThat(mother.hasRelationship(Direction.OUTGOING, OwlLabels.RDF_SUBCLASS_OF), is(true));
    Relationship relationship = getOnlyElement(mother.getRelationships(Direction.OUTGOING,
        OwlLabels.RDF_SUBCLASS_OF));
    assertThat(relationship.getEndNode(), is(woman));
  }

  @Test
  public void testAnnotationAssertionAxiom() {
    Node person = getNode(ROOT + "/Person");
    assertThat(
        GraphUtil.getProperty(person, "http://www.w3.org/2000/01/rdf-schema#comment", String.class)
            .get(), is(equalTo("Represents the set of all people.")));
  }

  @Test
  public void testMultiLanguageAnnotationAssertionAxiom() {
    Node person = getNode(ROOT + "/Person");
    assertThat(
        GraphUtil.getProperty(person, "http://www.w3.org/2000/01/rdf-schema#label", String.class)
            .get(), is(equalTo("Person")));
  }

  @Test
  public void testNonLiteralAnnotationAssertionAxiom() {
    Node person = getNode(ROOT + "/Person");
    Node fazz = getNode(ROOT + "/Fazz");
    Relationship assertion = getOnlyElement(GraphUtil.getRelationships(person, fazz,
        OwlLabels.OWL_ANNOTATION));
    assertThat(GraphUtil.getProperty(assertion, CommonProperties.URI, String.class).get(),
        is(equalTo(ROOT + "/fizz")));
  }

  @Test
  public void testNamedIndividualTypes() {
    Node john = getNode(ROOT + "/John");
    Node father = getNode(ROOT + "/Father");
    assertThat(john.hasLabel(OwlLabels.OWL_NAMED_INDIVIDUAL), is(true));
    assertThat(size(GraphUtil.getRelationships(john, father, OwlLabels.RDF_TYPE)), is(1));
  }

  @Test
  public void testSameIndividual() {
    Node james = getNode(ROOT + "/James");
    Node jim = getNode(ROOT + "/Jim");
    Relationship r = getOnlyElement(james.getRelationships(OwlLabels.OWL_SAME_AS));
    assertThat(r.getEndNode(), is(equalTo(jim)));
  }

  @Test
  public void testDifferentIndividual() {
    Node john = getNode(ROOT + "/John");
    Node bill = getNode(ROOT + "/Bill");
    Relationship r = getOnlyElement(john.getRelationships(OwlLabels.OWL_DIFFERENT_FROM));
    assertThat(r.getStartNode(), is(equalTo(bill)));
  }

  @Test
  public void testDataPropertyAssertions() {
    Node john = getNode(ROOT + "/John");
    assertThat((Integer) john.getProperty(ROOT + "/hasAge"), is(equalTo(51)));
  }

  @Test
  public void testMappedDataPropertyAssertion() {
    Node john = getNode(ROOT + "/John");
    assertThat((Integer) john.getProperty("isAged"), is(equalTo(51)));
  }

  @Test
  public void testObjectPropertyAssertions() {
    Node susan = getNode(ROOT + "/Susan");
    Node meg = getNode(ROOT + "/Meg");
    getOnlyElement(GraphUtil.getRelationships(susan, meg,
        DynamicRelationshipType.withName("hasAncestor")));
  }

  @Test
  public void testClassEquivalenceRelationships() {
    Node adult = getNode(ROOT + "/Adult");
    Node grownup = getNode(OTHER_ROOT + "/Grownup");
    assertThat(size(GraphUtil.getRelationships(adult, grownup, OwlLabels.OWL_EQUIVALENT_CLASS)),
        is(equalTo(1)));
  }

  @Test
  public void testDisjointClasses() {
    Node man = getNode(ROOT + "/Man");
    Node woman = getNode(ROOT + "/Woman");
    assertThat(size(GraphUtil.getRelationships(man, woman, OwlLabels.OWL_DISJOINT_WITH)),
        is(equalTo(1)));
  }

  @Test
  public void testObjectUnionOf() {
    Node parent = getNode(ROOT + "/Parent");
    Node intersection = getNode("http://ontology.neuinfo.org/anon/412251922");
    assertThat(
        size(GraphUtil.getRelationships(parent, intersection, OwlLabels.OWL_EQUIVALENT_CLASS)),
        is(equalTo(1)));
    assertThat(intersection.hasLabel(OwlLabels.OWL_UNION_OF), is(true));
    assertThat(GraphUtil.getProperty(intersection, NodeProperties.ANONYMOUS, Boolean.class).get(),
        is(true));
    Node mother = getNode(ROOT + "/Mother");
    Node father = getNode(ROOT + "/Father");
    assertThat(size(GraphUtil.getRelationships(intersection, mother, EdgeType.REL)), is(equalTo(1)));
    assertThat(size(GraphUtil.getRelationships(intersection, father, EdgeType.REL)), is(equalTo(1)));
  }

  @Test
  public void testObjectIntersectionOf() {
    Node parent = getNode(ROOT + "/HappyPerson");
    Node intersection = getNode("http://ontology.neuinfo.org/anon/1064318321");
    assertThat(
        size(GraphUtil.getRelationships(parent, intersection, OwlLabels.OWL_EQUIVALENT_CLASS)),
        is(equalTo(1)));
    assertThat(intersection.hasLabel(OwlLabels.OWL_INTERSECTION_OF), is(true));
    Node mother = getNode("http://ontology.neuinfo.org/anon/-1615296904");
    Node father = getNode("http://ontology.neuinfo.org/anon/-1615359878");
    assertThat(size(GraphUtil.getRelationships(intersection, mother, EdgeType.REL)), is(equalTo(1)));
    assertThat(size(GraphUtil.getRelationships(intersection, father, EdgeType.REL)), is(equalTo(1)));
  }

  @Test
  public void testObjectComplementOf() {
    Node parent = getNode(ROOT + "/Parent");
    Node complement = getNode("http://ontology.neuinfo.org/anon/-1761792206");
    assertThat(complement.hasLabel(OwlLabels.OWL_COMPLEMENT_OF), is(true));
    assertThat(size(GraphUtil.getRelationships(complement, parent, EdgeType.REL)), is(equalTo(1)));
  }

  /*
   * @Test public void testOntologyProperty() { Node parent = graph.getNode(ROOT + "/Parent").get();
   * assertThat(graph.getProperty(parent, NodeProperties.ONTOLOGY, String.class).get(),
   * is(equalTo(ROOT))); }
   * 
   * @Test public void testParentOntologyProperty() { Node parent = graph.getNode(ROOT +
   * "/Parent").get(); assertThat(graph.getProperty(parent, NodeProperties.PARENT_ONTOLOGY,
   * String.class).get(), is(equalTo(ROOT))); }
   */
  @Test
  public void testSubProperties() {
    Node hasWife = getNode(ROOT + "/hasWife");
    Node hasSpouse = getNode(ROOT + "/hasSpouse");
    assertThat(
        size(GraphUtil.getRelationships(hasWife, hasSpouse, OwlLabels.RDFS_SUB_PROPERTY_OF)),
        is(equalTo(1)));
  }

  @Test
  public void testChainedObjectProperties() {
    Node chain = getNode(ROOT + "/hasUncle");
    Node father = getNode(ROOT + "/hasFather");
    Node brother = getNode(ROOT + "/hasBrother");
    Relationship firstLink = getOnlyElement(GraphUtil.getRelationships(chain, father,
        OwlLabels.OWL_PROPERTY_CHAIN_AXIOM));
    Relationship secondLink = getOnlyElement(GraphUtil.getRelationships(chain, brother,
        OwlLabels.OWL_PROPERTY_CHAIN_AXIOM));
    assertThat(GraphUtil.getProperty(firstLink, "order", Integer.class).get(), is(equalTo(0)));
    assertThat(GraphUtil.getProperty(secondLink, "order", Integer.class).get(), is(equalTo(1)));
  }

  @Test
  public void testCardinalityRestriction() {
    Node restriction = getNode("http://ontology.neuinfo.org/anon/-583677237");
    Node hasChild = getNode(ROOT + "/hasChild");
    Node parent = getNode(ROOT + "/Parent");
    assertThat(restriction.hasLabel(OwlLabels.OWL_MIN_CARDINALITY), is(true));
    assertThat(GraphUtil.getProperty(restriction, "cardinality", Integer.class).get(),
        is(equalTo(2)));
    assertThat(size(GraphUtil.getRelationships(restriction, hasChild, EdgeType.PROPERTY)),
        is(equalTo(1)));
    assertThat(size(GraphUtil.getRelationships(restriction, parent, EdgeType.CLASS)),
        is(equalTo(1)));
  }

  @Test
  public void testSomeValuesFrom() {
    Node svf = getNode("http://ontology.neuinfo.org/anon/-1615296904");
    assertThat(svf.hasLabel(OwlLabels.OWL_SOME_VALUES_FROM), is(true));
    Node hasChild = getNode(ROOT + "/hasChild");
    Node happyPerson = getNode(ROOT + "/HappyPerson");
    assertThat(size(GraphUtil.getRelationships(svf, hasChild, EdgeType.PROPERTY)), is(equalTo(1)));
    assertThat(size(GraphUtil.getRelationships(svf, happyPerson, EdgeType.FILLER)), is(equalTo(1)));
  }

  @Test
  public void testAllValuesFrom() {
    Node avf = getNode("http://ontology.neuinfo.org/anon/-1615359878");
    assertThat(avf.hasLabel(OwlLabels.OWL_ALL_VALUES_FROM), is(true));
    Node hasChild = getNode(ROOT + "/hasChild");
    Node happyPerson = getNode(ROOT + "/HappyPerson");
    assertThat(size(GraphUtil.getRelationships(avf, hasChild, EdgeType.PROPERTY)), is(equalTo(1)));
    assertThat(size(GraphUtil.getRelationships(avf, happyPerson, EdgeType.FILLER)), is(equalTo(1)));
  }

  /*
   * @Test public void testSomeValuesFromPostProcess() { Node woman = graph.getNode(ROOT +
   * "/Woman").get(); Node happyPerson = graph.getNode(ROOT + "/HappyPerson").get();
   * assertThat(graph.hasRelationship(woman, happyPerson,
   * DynamicRelationshipType.withName("hasChild"), ROOT + "/hasChild"), is(true)); }
   * 
   * @Test
   * 
   * @Ignore public void testEquivalencePostProcess() { Node adult = graph.getNode(ROOT +
   * "/Adult").get(); Node grownUp = graph.getNode(OTHER_ROOT + "/Grownup").get(); Node nonchild =
   * graph.getNode(OTHER_ROOT + "/Nonchild").get(); assertThat(graph.hasRelationship(adult, grownUp,
   * EdgeType.EQUIVALENT_TO), is(true)); assertThat(graph.hasRelationship(grownUp, adult,
   * EdgeType.EQUIVALENT_TO), is(true)); assertThat(graph.hasRelationship(adult, nonchild,
   * EdgeType.EQUIVALENT_TO), is(true)); assertThat(graph.hasRelationship(nonchild, adult,
   * EdgeType.EQUIVALENT_TO), is(true)); assertThat(graph.hasRelationship(nonchild, grownUp,
   * EdgeType.EQUIVALENT_TO), is(true)); assertThat(graph.hasRelationship(grownUp, nonchild,
   * EdgeType.EQUIVALENT_TO), is(true)); }
   * 
   * @Test public void testCuries() { Node grownup = graph.getOrCreateNode(OTHER_ROOT + "/Grownup");
   * String curie = graph.getProperty(grownup, CommonProperties.CURIE, String.class).get();
   * assertThat(curie, is(equalTo("otherOnt:Grownup"))); }
   */

  /***
   * http://www.w3.org/TR/owl2-new-features/#F12:_Punning
   * 
   * @throws IOException
   */
  @Test
  public void testPunning() {
    Node eagle = getNode(ROOT + "/Eagle");
    assertThat(eagle.getLabels(),
        containsInAnyOrder(OwlLabels.OWL_NAMED_INDIVIDUAL, OwlLabels.OWL_CLASS));
  }

  @Test
  public void testDataProperties() {
    Node hasAge = getNode(ROOT + "/hasAge");
    assertThat(hasAge.hasLabel(OwlLabels.OWL_DATA_PROPERTY), is(true));
  }

  @Test
  public void testObjectProperties() {
    Node hasAge = getNode(ROOT + "/hasParent");
    assertThat(hasAge.hasLabel(OwlLabels.OWL_OBJECT_PROPERTY), is(true));
  }

  @Test
  public void testDrawGraph() throws IOException {
    GraphvizWriter writer = new GraphvizWriter();
    Walker walker = Walker.fullGraph(graphDb);
    writer.emit(new File("/temp/ont.dot"), walker);
  }

}