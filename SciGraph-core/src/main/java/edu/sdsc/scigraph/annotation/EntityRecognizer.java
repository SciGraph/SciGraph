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
