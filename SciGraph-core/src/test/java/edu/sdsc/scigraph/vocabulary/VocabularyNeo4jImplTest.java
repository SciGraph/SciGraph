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
package edu.sdsc.scigraph.vocabulary;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.owlapi.*;
import edu.sdsc.scigraph.util.GraphTestBase;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

/***
 * TODO: Some of these tests should be moved directly to the analyzer
 */
public class VocabularyNeo4jImplTest extends GraphTestBase {

  VocabularyNeo4jImpl vocabulary;
  Graph graph;

  Concept hippocampus;
  Concept hippocampusStructure;
  Concept structureOfHippocampus;
  Concept cerebellum;
  Concept hippocampalFormation;
  Concept specialChars;
  Concept parkinsons;
  Concept als;

  Concept buildConcept(String uri, String label, String... categories) {
    Node concept = graph.getOrCreateNode(uri);
    graph.addProperty(concept, Concept.LABEL, label);
    graph.setProperty(concept, NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX, label);
    for (String category : categories) {
      graph.addProperty(concept, Concept.CATEGORY, category);
    }
    return graph.getOrCreateFramedNode(concept);
  }

  @Before
  public void setupGraph() throws IOException {
    graph = new Graph(graphDb);
    try (Transaction tx = graphDb.beginTx()) {
      hippocampalFormation = buildConcept("http://example.org/#birnlex5", "Hippocampal formation");
      hippocampus = buildConcept("http://example.org/#hippocampus", "Hippocampus", "foo", "fizz");
      graph.setProperty(graph.getNode(hippocampus), Concept.ONTOLOGY, "http://foo.org");
      graph.addProperty(graph.getNode(hippocampus), Concept.SYNONYM, "cornu ammonis");
      graph.addProperty(graph.getNode(hippocampus), Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX,
          "cornu ammonis");
      hippocampusStructure = buildConcept("http://example.org/#hippocampusStructure",
          "Hippocampus structure", "baz");
      graph.setProperty(graph.getNode(hippocampusStructure), Concept.ONTOLOGY, "http://baz.org");
      structureOfHippocampus = buildConcept("http://example.org/#structureOfHippocampus",
          "Structure of hippocampus", "baz");
      cerebellum = buildConcept("http://example.org/#cerebellum", "Cerebellum", "baz", "foo");
      graph.setProperty(graph.getNode(cerebellum), Concept.ONTOLOGY, "http://baz.org");
      specialChars = buildConcept("http://example.org/#specialChars", "(-)-protein alpha", "baz",
          "foo bar");
      parkinsons = buildConcept("http://example.org/parkinsons", "Parkinson's Disease", "baz");
      graph.addProperty(graph.getNode(parkinsons), Concept.SYNONYM, "the");
      als = buildConcept("http://example.org/als", "amyotrophic lateral sclerosis");
      graph.addProperty(graph.getNode(als), Concept.SYNONYM, "Lou Gehrig's");
      graph.addProperty(graph.getNode(als), Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX,
          "Lou Gehrig's");
      graph.addProperty(graph.getNode(als), Concept.SYNONYM, "motor neuron disease, bulbar");
      graph.addProperty(graph.getNode(als), Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX,
          "motor neuron disease, bulbar");
      tx.success();
    }

    CurieUtil curieUtil = mock(CurieUtil.class);
    when(curieUtil.getPrefixes()).thenReturn(newHashSet("H", "S"));
    when(curieUtil.getAllExpansions("H")).thenReturn(newHashSet("http://example.org/#h"));
    when(curieUtil.getAllExpansions("S")).thenReturn(newHashSet("http://example.org/#s"));
    when(curieUtil.getFullUri(anyString())).thenReturn(Collections.<String>emptySet());
    when(curieUtil.getFullUri("HP:0008")).thenReturn(newHashSet("http://example.org/#hippocampus"));
    vocabulary = new VocabularyNeo4jImpl(graph, null, curieUtil);
  }

  @Test
  public void testGetByUri() {
    Optional<Concept> concept = vocabulary.getConceptFromUri("http://example.org/#hippocampus");
    assertThat(concept.get(), is(hippocampus));
  }

  @Test
  public void testAbsentUri() {
    Optional<Concept> concept = vocabulary.getConceptFromUri("http://example.org/absent");
    assertThat(concept.isPresent(), is(false));
  }

  @Test
  public void testGetByCurie() {
    Query query = new Vocabulary.Query.Builder("HP:0008").build();
    assertThat(vocabulary.getConceptFromId(query), contains(hippocampus));
  }

  @Test
  public void testGetByFragment() {
    Query query = new Vocabulary.Query.Builder("cerebellum").build();
    assertThat(vocabulary.getConceptFromId(query), contains(cerebellum));
  }

  @Test
  public void testNonexistantFragment() {
    Query query = new Vocabulary.Query.Builder("absent").build();
    assertThat(vocabulary.getConceptFromId(query), is(empty()));
  }

  @Test
  public void testIdWithSpacesNoException() {
    Query query = new Vocabulary.Query.Builder("with space").build();
    assertThat(vocabulary.getConceptFromId(query), is(empty()));
  }

  @Test
  public void testSearchConcepts() {
    Query query = new Vocabulary.Query.Builder("hippocampus").build();
    assertThat(vocabulary.searchConcepts(query),
        contains(hippocampus, structureOfHippocampus, hippocampusStructure, hippocampalFormation));
  }

  @Test
  public void testSearchConceptsWithLimit() {
    Query query = new Vocabulary.Query.Builder("hippocampus").limit(1).build();
    assertThat(vocabulary.searchConcepts(query), contains(hippocampus));
  }

  @Test
  public void testSearchConceptsWithCategory() {
    Query query = new Vocabulary.Query.Builder("hippocampus").categories(newArrayList("foo"))
        .build();
    assertThat(vocabulary.searchConcepts(query), contains(hippocampus));
  }

  @Test
  public void testSearchConceptsWithCategoryWithWhitespace() {
    Query query = new Vocabulary.Query.Builder("alpha").categories(newArrayList("foo bar")).build();
    assertThat(vocabulary.searchConcepts(query), contains(specialChars));
  }

  @Test
  public void testGetConceptsFromTerm() {
    Query query = new Vocabulary.Query.Builder("hippocampus").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(hippocampus));
  }

  @Test
  public void testGetConceptsFromTermWithSpaces() {
    Query query = new Vocabulary.Query.Builder("hippocampus Formation").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(hippocampalFormation));
  }

  @Test
  public void testGetConceptsFromTermWithSpecialCharacters() {
    Query query = new Vocabulary.Query.Builder("(-)-protein alpha").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(specialChars));
  }

  @Test
  public void testSearchconceptsWithNonexistantCategory() {
    Query query = new Vocabulary.Query.Builder("hippocampus").categories(
        newArrayList("doesntExist")).build();
    assertThat(vocabulary.searchConcepts(query), is(empty()));
  }

  @Test
  public void testGetConceptsFromPrefix() {
    Query query = new Vocabulary.Query.Builder("hip").build();
    assertThat(vocabulary.getConceptsFromPrefix(query),
        contains(hippocampus, hippocampusStructure, hippocampalFormation));
  }

  @Test
  public void testGetConceptsFromPrefixWithApos() {
    Query query = new Vocabulary.Query.Builder("parkinson").includeSynonyms(false).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(parkinsons));
    query = new Vocabulary.Query.Builder("parkinsons").includeSynonyms(false).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(parkinsons));
    query = new Vocabulary.Query.Builder("parkinson's").includeSynonyms(false).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(parkinsons));
    query = new Vocabulary.Query.Builder("parkinsons disease").includeSynonyms(false).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(parkinsons));
    query = new Vocabulary.Query.Builder("parkinson's disease").includeSynonyms(false).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(parkinsons));
  }

  @Test
  public void testGetConceptFromCuriePrefix() {
    Query query = new Vocabulary.Query.Builder("HP:0008").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus, hippocampusStructure));
  }

  @Test
  public void testGetConceptFromFragmentPrefix() {
    Query query = new Vocabulary.Query.Builder("birnlex").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampalFormation));
  }

  @Test
  public void testGetConceptsFromPrefixWithSpace() {
    Query query = new Vocabulary.Query.Builder("hippocampus str").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithCategory() {
    Query query = new Vocabulary.Query.Builder("hip").categories(newArrayList("baz")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithMultipleCategories() {
    Query query = new Vocabulary.Query.Builder("hip").categories(newArrayList("baz", "foo"))
        .build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus, hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithCurie() {
    Query query = new Vocabulary.Query.Builder("hip").curies(newHashSet("H")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus, hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithMultipleCuries() {
    Query query = new Vocabulary.Query.Builder("hip").curies(newHashSet("H", "S")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus, hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithSpecialCharacters() {
    Query query = new Vocabulary.Query.Builder("(-)-pro").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(specialChars));
  }

  @Test
  public void testGetConceptsFrompPrefixWithSynonyms() {
    Query query = new Vocabulary.Query.Builder("Co").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus));
  }

  @Test
  public void testSearchConceptsWithSynonyms() {
    Query query = new Vocabulary.Query.Builder("ammonis").build();
    assertThat(vocabulary.searchConcepts(query), contains(hippocampus));
  }

  @Test
  public void testSearchConceptsWithNoSynonyms() {
    Query query = new Vocabulary.Query.Builder("ammonis").includeSynonyms(false).build();
    assertThat(vocabulary.searchConcepts(query), is(empty()));
  }

  @Test
  public void testGetConceptsFromTermWithSynonym() {
    Query query = new Vocabulary.Query.Builder("cornu ammonis").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(hippocampus));
  }

  @Test
  public void testGetAllOntologies() {
    Set<String> ontologies = vocabulary.getAllOntologies();
    assertThat(ontologies, hasItems("http://foo.org", "http://baz.org"));
  }

  @Test
  public void testGetAllCategories() {
    Set<String> categories = vocabulary.getAllCategories();
    assertThat(categories, hasItems("foo", "fizz", "baz"));
  }

  @Test
  public void testPossessives() {
    Query query = new Vocabulary.Query.Builder("parkinsons disease").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(parkinsons));
  }

  @Test
  public void testLeadingAndTrailingPunctuation() {
    Query query = new Vocabulary.Query.Builder("hippocampus,").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(hippocampus));
    query = new Vocabulary.Query.Builder(",hippocampal formations,").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(hippocampalFormation));
  }

  @Test
  public void testLouPrefix() {
    Query query = new Vocabulary.Query.Builder("lou geh").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(als));
  }

  @Test
  public void testQuotedIdQuery() {
    Query query = new Vocabulary.Query.Builder("\"HP:0008\"").build();
    assertThat(vocabulary.getConceptFromId(query), contains(hippocampus));
  }

  @Test
  @Ignore
  public void testStopWordOnlyQuery() {
    // TODO: Make sure that stopwords don't return...
    Query query = new Vocabulary.Query.Builder("a").build();
    assertThat(vocabulary.getConceptsFromTerm(query), is(empty()));
  }

}
