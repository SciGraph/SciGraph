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
package io.scigraph.services.refine;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Sets.newHashSet;
import io.dropwizard.jackson.Jackson;
import io.scigraph.frames.Concept;
import io.scigraph.vocabulary.Vocabulary;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;

public class RefineUtil {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  public static RefineQueries getQueries(String queries) throws IOException {
    return MAPPER.readValue(queries, RefineQueries.class);
  }

  private static boolean isProbableJSON(String text) {
    return text.startsWith("[") && text.endsWith("]") ||
           text.startsWith("{") && text.endsWith("}");
  }

  public static RefineQuery getQuery(String query) throws IOException {
    RefineQuery refineQuery = new RefineQuery();
    if (isProbableJSON(query)) {
      refineQuery = MAPPER.readValue(query, RefineQuery.class);
    } else {
      refineQuery.setQuery(query);
    }
    return refineQuery;
  }

  public static Vocabulary.Query getVocabularyQuery(RefineQuery refineQuery) {
    Vocabulary.Query.Builder builder = new Vocabulary.Query.Builder(refineQuery.getQuery());
    if (refineQuery.getLimit().isPresent()) {
      builder.limit(refineQuery.getLimit().get());
    }
    if (refineQuery.getType().isPresent()) {
      builder.categories(Splitter.on(',').splitToList(refineQuery.getType().get()));
    }
    return builder.build();
  }

  // TODO: Can this be done with Dozer?
  public static RefineResults conceptsToRefineResults(List<Concept> concepts) {
    RefineResults results = new RefineResults();
    for (Concept concept: concepts) {
      RefineResult result = new RefineResult();
      result.setId(concept.getIri());
      result.setName(getFirst(concept.getLabels(), ""));
      result.setScore(1.0);
      result.setType(newHashSet(concept.getCategories()));
      result.setMatch(true);
      results.addResult(result);
    }
    return results;
  }
}
