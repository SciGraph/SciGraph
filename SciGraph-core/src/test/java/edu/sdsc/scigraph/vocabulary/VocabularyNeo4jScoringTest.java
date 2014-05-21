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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.frames.NodeProperties;
import edu.sdsc.scigraph.lucene.LuceneUtils;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.util.GraphTestBase;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

/***
 * TODO: Some of these tests should be moved directly to the analyzer
 */
public class VocabularyNeo4jScoringTest extends GraphTestBase {

  VocabularyNeo4jImpl<Concept> vocabulary;
  Graph<Concept> graph;

  Concept cell;
  Concept onCell;

  Concept buildConcept(String uri, String label, String curie, String ... categories) {
    Concept concept = graph.getOrCreateFramedNode(uri);
    concept.addLabel(label);
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
    cell = buildConcept("http://example.org/#birnlex5", "Cell", "BL:5");
    onCell = buildConcept("http://example.org/#birnlex6", "Something on cell", "HP:0008");
    onCell.addSynonym("on cell");
    vocabulary = new VocabularyNeo4jImpl<Concept>(graph, null);
  }

  @Test
  public void testGetConceptsFromTerm() {
    Query query = new Vocabulary.Query.Builder("cell").build();
    assertThat(vocabulary.getConceptsFromTerm(query), contains(cell, onCell));
  }

}
