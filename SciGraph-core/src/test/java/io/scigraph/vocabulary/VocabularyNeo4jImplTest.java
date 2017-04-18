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
package io.scigraph.vocabulary;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.prefixcommons.CurieUtil;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import io.scigraph.frames.Concept;
import io.scigraph.frames.NodeProperties;
import io.scigraph.lucene.LuceneUtils;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.neo4j.NodeTransformer;
import io.scigraph.util.GraphTestBase;
import io.scigraph.vocabulary.Vocabulary.Query;


/***
 * TODO: Some of these tests should be moved directly to the analyzer
 */
public class VocabularyNeo4jImplTest extends GraphTestBase {

  VocabularyNeo4jImpl vocabulary;

  Concept hippocampus;
  Concept hippocampusStructure;
  Concept structureOfHippocampus;
  Concept cerebellum;
  Concept hippocampalFormation;
  Concept specialChars;
  Concept parkinsons;
  Concept als;
  Concept deprecated;

  NodeTransformer transformer = new NodeTransformer();

  Concept buildConcept(String iri, String label, String... categories) {
    Node concept = createNode(iri);
    GraphUtil.addProperty(concept, Concept.LABEL, label);
    GraphUtil.addProperty(concept, NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX, label);
    for (String category : categories) {
      GraphUtil.addProperty(concept, Concept.CATEGORY, category);
    }
    return transformer.apply(concept);
  }

  @Before
  public void setupGraph() throws IOException {
    try (Transaction tx = graphDb.beginTx()) {
      hippocampalFormation = buildConcept("http://example.org/#birnlex5", "Hippocampal formation");
      hippocampus = buildConcept("http://example.org/#hippocampus", "Hippocampus", "foo", "fizz");
      GraphUtil.addProperty(graphDb.getNodeById(hippocampus.getId()), Concept.SYNONYM,
          "cornu ammonis");
      GraphUtil.addProperty(graphDb.getNodeById(hippocampus.getId()), Concept.SYNONYM
          + LuceneUtils.EXACT_SUFFIX, "cornu ammonis");
      GraphUtil.addProperty(graphDb.getNodeById(hippocampus.getId()), Concept.ABREVIATION, "hpcs");
      GraphUtil.addProperty(graphDb.getNodeById(hippocampus.getId()), Concept.ABREVIATION
          + LuceneUtils.EXACT_SUFFIX, "hpcs");
      hippocampusStructure =
          buildConcept("http://example.org/#hippocampusStructure", "Hippocampus structure", "baz");
      structureOfHippocampus =
          buildConcept("http://example.org/#structureOfHippocampus", "Structure of hippocampus",
              "baz");
      cerebellum = buildConcept("http://example.org/#cerebellum", "Cerebellum", "baz", "foo");
      specialChars =
          buildConcept("http://example.org/#specialChars", "(-)-protein alpha", "baz", "foo bar");
      parkinsons = buildConcept("http://example.org/#parkinsons", "Parkinson's Disease", "baz");
      GraphUtil.addProperty(graphDb.getNodeById(parkinsons.getId()), Concept.SYNONYM, "the");
      GraphUtil.addProperty(graphDb.getNodeById(parkinsons.getId()), Concept.ACRONYM, "PD");
      GraphUtil.addProperty(graphDb.getNodeById(parkinsons.getId()), Concept.ACRONYM
          + LuceneUtils.EXACT_SUFFIX, "PD");
      als = buildConcept("http://example.org/#als", "amyotrophic lateral sclerosis");
      GraphUtil.addProperty(graphDb.getNodeById(als.getId()), Concept.SYNONYM, "Lou Gehrig's");
      GraphUtil.addProperty(graphDb.getNodeById(als.getId()), Concept.SYNONYM
          + LuceneUtils.EXACT_SUFFIX, "Lou Gehrig's");
      GraphUtil.addProperty(graphDb.getNodeById(als.getId()), Concept.SYNONYM,
          "motor neuron disease, bulbar");
      GraphUtil.addProperty(graphDb.getNodeById(als.getId()), Concept.SYNONYM
          + LuceneUtils.EXACT_SUFFIX, "motor neuron disease, bulbar");
      GraphUtil.addProperty(graphDb.getNodeById(als.getId()), Concept.ACRONYM, "ALS");
      GraphUtil.addProperty(graphDb.getNodeById(als.getId()), Concept.ACRONYM
          + LuceneUtils.EXACT_SUFFIX, "ALS");
      deprecated = buildConcept("http://example.org/#cerebellum2", "Cerebellum", "baz", "foo");
      GraphUtil.addProperty(graphDb.getNodeById(deprecated.getId()),
          OWLRDFVocabulary.OWL_DEPRECATED.toString(), "true");
      tx.success();
    }

    CurieUtil curieUtil = mock(CurieUtil.class);
    when(curieUtil.getPrefixes()).thenReturn(newHashSet("H", "S"));
    when(curieUtil.getExpansion("H")).thenReturn("http://example.org/#h");
    when(curieUtil.getExpansion("S")).thenReturn("http://example.org/#s");
    when(curieUtil.getIri(anyString())).thenReturn(Optional.<String>empty());
    when(curieUtil.getIri("HP:0008")).thenReturn(Optional.of("http://example.org/#hippocampus"));
    vocabulary = new VocabularyNeo4jImpl(graphDb, null, curieUtil, new NodeTransformer());
  }

  @Test
  public void testGetByUri() {
    Query query = new Vocabulary.Query.Builder("http://example.org/#hippocampus").build();
    assertThat(vocabulary.getConceptFromId(query).get(), is(hippocampus));
  }

  @Test
  public void testAbsentUri() {
    Query query = new Vocabulary.Query.Builder("http://example.org/absent").build();
    assertThat(vocabulary.getConceptFromId(query).isPresent(), is(false));
  }

  @Test
  public void testGetByCurie() {
    Query query = new Vocabulary.Query.Builder("HP:0008").build();
    assertThat(vocabulary.getConceptFromId(query).get(), is(hippocampus));
  }

  @Test
  public void testIdWithSpacesNoException() {
    Query query = new Vocabulary.Query.Builder("with space").build();
    assertThat(vocabulary.getConceptFromId(query).isPresent(), is(false));
  }

  @Test
  public void testSearchConcepts() {
    Query query = new Vocabulary.Query.Builder("hippocampus").build();
    assertThat(
        vocabulary.searchConcepts(query),
        containsInAnyOrder(hippocampus, structureOfHippocampus, hippocampusStructure,
            hippocampalFormation));
  }

  @Test
  public void testSearchConceptsWithLimit() {
    Query query = new Vocabulary.Query.Builder("hippocampus").limit(1).build();
    assertThat(vocabulary.searchConcepts(query).size(), is(1));
  }

  @Test
  public void testSearchConceptsWithCategory() {
    Query query =
        new Vocabulary.Query.Builder("hippocampus").categories(newArrayList("foo")).build();
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
    Query query =
        new Vocabulary.Query.Builder("hippocampus").categories(newArrayList("doesntExist")).build();
    assertThat(vocabulary.searchConcepts(query), is(empty()));
  }

  @Test
  public void testGetConceptsFromPrefix() {
    Query query = new Vocabulary.Query.Builder("hip").build();
    assertThat(vocabulary.getConceptsFromPrefix(query),
        containsInAnyOrder(hippocampus, hippocampusStructure, hippocampalFormation));
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
    assertThat(vocabulary.getConceptsFromPrefix(query),
        containsInAnyOrder(hippocampus, hippocampusStructure));
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
    Query query =
        new Vocabulary.Query.Builder("hip").categories(newArrayList("baz", "foo")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), containsInAnyOrder(hippocampus, hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithCuriePrefix() {
    Query query = new Vocabulary.Query.Builder("hip").prefixes(newHashSet("H")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query),
        containsInAnyOrder(hippocampus, hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithMultipleCuriePrefixes() {
    Query query = new Vocabulary.Query.Builder("hip").prefixes(newHashSet("H", "S")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query),
        containsInAnyOrder(hippocampus, hippocampusStructure));
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
  public void testGetAllPrefixes() {
    Collection<String> prefixes = vocabulary.getAllCuriePrefixes();
    assertThat(prefixes, hasItems("H", "S"));
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
    System.out.println(vocabulary.getConceptsFromTerm(query));
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
    assertThat(vocabulary.getConceptFromId(query).get(), is(hippocampus));
  }

  @Test
  public void deprecatedClassesReturned() {
    Query query = new Vocabulary.Query.Builder("Cerebellum").build();
    assertThat(vocabulary.getConceptsFromTerm(query), containsInAnyOrder(cerebellum, deprecated));
  }

  @Test
  public void deprecatedClassesNotReturned_whenRequested() {
    Query query = new Vocabulary.Query.Builder("Cerebellum").includeDeprecated(false).build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(cerebellum));
  }

  @Test
  @Ignore
  public void testStopWordOnlyQuery() {
    // TODO: Make sure that stopwords don't return...
    Query query = new Vocabulary.Query.Builder("a").build();
    assertThat(vocabulary.getConceptsFromTerm(query), is(empty()));
  }

  @Test
  public void abbreviationsAreCompleted() {
    Query query = new Vocabulary.Query.Builder("hpc").includeAbbreviations(true).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus));
  }

  @Test
  public void acronymsAreCompleted() {
    Query query = new Vocabulary.Query.Builder("al").includeAcronyms(true).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(als));
  }

  @Test
  public void abbreviationsAreSearched() {
    Query query = new Vocabulary.Query.Builder("hpcs").includeAbbreviations(true).build();
    assertThat(vocabulary.searchConcepts(query), contains(hippocampus));
  }

  @Test
  public void acronymsAreSearched() {
    Query query = new Vocabulary.Query.Builder("als").includeAcronyms(true).build();
    assertThat(vocabulary.searchConcepts(query), contains(als));
  }

  @Test
  public void abbreviationsAreResolved() {
    Query query = new Vocabulary.Query.Builder("hpcs").includeAbbreviations(true).build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(hippocampus));
  }

  @Test
  public void acronymsAreResolved() {
    Query query = new Vocabulary.Query.Builder("als").includeAcronyms(true).build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(als));
  }

  @Test
  public void specialCharactersAreEscaped() {
    Query query =
        new Vocabulary.Query.Builder("HP:0008").includeSynonyms(true).categories(newHashSet("foo"))
            .build();
    assertThat(vocabulary.getConceptsFromTerm(query), is(empty()));
    query =
        new Vocabulary.Query.Builder("HP:0008").includeSynonyms(true).categories(newHashSet("foo"))
            .build();
    assertThat(vocabulary.searchConcepts(query), is(empty()));
  }

}
