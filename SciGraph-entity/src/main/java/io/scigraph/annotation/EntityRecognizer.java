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
package io.scigraph.annotation;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.disjoint;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.prefixcommons.CurieUtil;

import io.scigraph.frames.Concept;
import io.scigraph.vocabulary.Vocabulary;
import io.scigraph.vocabulary.Vocabulary.Query;

public class EntityRecognizer {

  private final Vocabulary vocabulary;
  private final CurieUtil curieUtil;

  @Inject
  EntityRecognizer(Vocabulary vocabulary, CurieUtil curieUtil) throws IOException {
    this.vocabulary = vocabulary;
    this.curieUtil = curieUtil;
  }

  public String getCssClass() {
    return "sciCrunchAnnotation";
  }

  boolean shouldAnnotate(Concept concept, EntityFormatConfiguration config) {
    Collection<String> conceptCategories = newHashSet(concept.getCategories());

    if (!disjoint(config.getExcludeCategories(), conceptCategories)) {
      return false;
    }
    if (!config.isIncludeNumbers() && getFirst(concept.getLabels(), "").matches("(\\d|\\-|_)+")) {
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

    Set<Entity> entities = newHashSet();
    for (Concept term : terms) {
      if (shouldAnnotate(term, config)) {
        Optional<String> id = curieUtil.getCurie(term.getIri());
        entities.add(new Entity(term.getLabels(), id.orElse(term.getIri()), term.getCategories()));
      }
    }

    return entities;
  }

}
