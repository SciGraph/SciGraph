/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.util.GraphTestBase;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.VocabularyNeo4jImpl;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

/***
 * TODO: Some of these tests should be moved directly to the analyzer
 */
public class VocabularyNeo4jImplTest extends GraphTestBase {

  VocabularyNeo4jImpl<Concept> vocabulary;
  Graph<Concept> graph;

  Concept hippocampus;
  Concept hippocampusStructure;
  Concept structureOfHippocampus;
  Concept cerebellum;
  Concept hippocampalFormation;
  Concept specialChars;
  Concept parkinsons;

  Concept buildConcept(String uri, String label, String curie, String ... categories) {
    Concept concept = graph.getOrCreateFramedNode(uri);
    concept.setLabel(label);
    concept.setCurie(curie);
    concept.asVertex().setProperty(NodeProperties.LABEL + LuceneUtils.EXACT_SUFFIX, label);
    for (String category: categories) {
      concept.addCategory(category);
    }
    return concept;
  }

  @Before
  public void setupGraph() throws IOException {
    graph = new Graph<Concept>(graphDb, Concept.class);
    hippocampalFormation = buildConcept("http://example.org/#birnlex5", "Hippocampal formation", "BL:5");
    hippocampus = buildConcept("http://example.org/#hippocampus", "Hippocampus", "HP:0008", "foo", "fizz");
    hippocampus.setOntology("http://foo.org");
    hippocampus.addSynonym("cornu ammonis");
    hippocampus.asVertex().setProperty(Concept.SYNONYM + LuceneUtils.EXACT_SUFFIX, "cornu ammonis");
    hippocampusStructure = buildConcept("http://example.org/#hippocampusStructure", "Hippocampus structure", "HP", "baz");
    hippocampusStructure.setOntology("http://baz.org");
    structureOfHippocampus = buildConcept("http://example.org/#structureOfHippocampus", "Structure of hippocampus", "baz");
    cerebellum = buildConcept("http://example.org/#cerebellum", "Cerebellum", "baz", "foo");
    cerebellum.setOntology("http://bar.org");
    specialChars = buildConcept("http://example.org/#specialChars", "(-)-protein alpha", "baz", "foo bar");
    parkinsons = buildConcept("http://example.org/parkinsons", "Parkinson's Disease", "baz");

    vocabulary = new VocabularyNeo4jImpl<Concept>(graph, null);
  }

  @Test
  public void testGetByUri() {
    Optional<Concept> concept = vocabulary.getConceptFromUri("http://example.org/#hippocampus");
    assertThat(concept.get(), is(equalTo(hippocampus)));
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
    assertThat(vocabulary.searchConcepts(query), contains(hippocampus, structureOfHippocampus, hippocampusStructure, hippocampalFormation));
  }

  @Test
  public void testSearchConceptsWithLimit() {
    Query query = new Vocabulary.Query.Builder("hippocampus").limit(1).build();
    assertThat(vocabulary.searchConcepts(query), contains(hippocampus));
  }

  @Test
  public void testSearchConceptsWithCategory() {
    Query query = new Vocabulary.Query.Builder("hippocampus").categories(newArrayList("foo")).build();
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
    Query query = new Vocabulary.Query.Builder("hippocampus").categories(newArrayList("doesntExist")).build();
    assertThat(vocabulary.searchConcepts(query), is(empty()));
  }

  @Test
  public void testGetConceptsFromPrefix() {
    Query query = new Vocabulary.Query.Builder("hip").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus, hippocampusStructure, hippocampalFormation));
  }

  @Test
  public void testGetConceptsFromPrefixWithSpace() {
    Query query = new Vocabulary.Query.Builder("hippocampus str").build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithCaregory() {
    Query query = new Vocabulary.Query.Builder("hip").categories(newArrayList("baz")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampusStructure));
  }

  @Test
  public void testGetConceptsFromPrefixWithMultipleCategories() {
    Query query = new Vocabulary.Query.Builder("hip").categories(newArrayList("baz", "foo")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampusStructure, hippocampus));
  }

  @Test
  public void testGetConceptsFromPrefixWithOntology() {
    Query query = new Vocabulary.Query.Builder("hip").ontologies(newHashSet("http://foo.org")).build();
    assertThat(vocabulary.getConceptsFromPrefix(query), contains(hippocampus));
  }

  @Test
  public void testGetConceptsFromPrefixWithMultipleOntologies() {
    Query query = new Vocabulary.Query.Builder("hip").ontologies(newHashSet("http://foo.org", "http://baz.org")).build();
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
    assertThat(ontologies, hasItems("http://foo.org", "http://bar.org"));
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

}
