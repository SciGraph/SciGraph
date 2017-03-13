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

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import io.scigraph.frames.Concept;
import io.scigraph.frames.NodeProperties;
import io.scigraph.lucene.LuceneUtils;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.neo4j.NodeTransformer;
import io.scigraph.util.GraphTestBase;
import io.scigraph.vocabulary.Vocabulary.Query;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.prefixcommons.CurieUtil;

/***
 * TODO: Some of these tests should be moved directly to the analyzer
 */
public class VocabularyNeo4jScoringTest extends GraphTestBase {

  VocabularyNeo4jImpl vocabulary;

  Concept cell;
  Concept onCell;

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
    cell = buildConcept("http://x.org/#birnlex5", "Cell cell", "BL:5");
    onCell = buildConcept("http://x.org/#birnlex6", "Something on cell", "HP:0008");
    vocabulary =
        new VocabularyNeo4jImpl(graphDb, null, mock(CurieUtil.class), new NodeTransformer());
  }

  @Test
  public void testGetConceptsFromTerm() {
    Query query = new Vocabulary.Query.Builder("cell").build();
    List<Concept> results = vocabulary.searchConcepts(query);
    assertThat(results, containsInAnyOrder(cell, onCell));
  }

}
