package edu.sdsc.scigraph.annotation;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.disjoint;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Sets;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

public class EntityRecognizer {

  private final Vocabulary<Concept> vocabulary;

  @Inject
  EntityRecognizer(Vocabulary<Concept> vocabulary) throws IOException {
    this.vocabulary = vocabulary;
  }

  public String getCssClass() {
    return "sciCrunchAnnotation";
  }

  boolean shouldAnnotate(Concept concept, EntityFormatConfiguration config) {
    Collection<String> conceptCategories = newHashSet(concept.getCategories());

    if (!disjoint(config.getExcludeCategories(), conceptCategories)) {
      return false;
    }
    if (concept.getLabel().length() < config.getMinLength()) {
      return false;
    }
    if (!config.isIncludeNumbers() && concept.getLabel().matches("(\\d|\\-|_)+")) {
      return false;
    }

    if (!config.getIncludeCategories().isEmpty()
        && disjoint(config.getIncludeCategories(), conceptCategories)) {
      return false;
    }

    return true;
  }

  public Collection<Entity> getEntities(String token, EntityFormatConfiguration config) {
    Query query = new Vocabulary.Query.Builder(token).build();
    List<Concept> terms = vocabulary.getConceptsFromTerm(query);

    Set<Entity> entities = Sets.newHashSet();
    for (Concept term : terms) {
      if (shouldAnnotate(term, config)) {
        entities.add(new Entity(term));
      }
    }

    return entities;
  }

}
