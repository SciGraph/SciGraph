package edu.sdsc.scigraph.services.refine;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Sets.newHashSet;
import io.dropwizard.jackson.Jackson;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

public class RefineUtil {

  private static final ObjectMapper MAPPER = Jackson.newObjectMapper();

  public static RefineQueries getQueries(String queries) throws IOException {
    return MAPPER.readValue(queries, RefineQueries.class);
  }

  public static RefineQuery getQuery(String query) throws IOException {
    RefineQuery refineQuery = new RefineQuery();
    if ((query.startsWith("[") && query.endsWith("]")) ||
        (query.startsWith("{") && query.endsWith("}"))) {
      // Is probable JSON string
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
      // TODO: Convert this to categories
    }
    return builder.build();
  }

  public static RefineResults conceptsToRefineResults(List<Concept> concepts) {
    RefineResults results = new RefineResults();
    for (Concept concept: concepts) {
      RefineResult result = new RefineResult();
      result.setId(concept.getUri());
      result.setName(getFirst(concept.getLabels(), ""));
      result.setScore(1.0);
      result.setType(newHashSet(concept.getCategories()));
      result.setMatch(true);
      results.addResult(result);
    }
    return results;
  }
}
