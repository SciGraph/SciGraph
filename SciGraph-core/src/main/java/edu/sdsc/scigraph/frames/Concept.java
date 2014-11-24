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
package edu.sdsc.scigraph.frames;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Iterables;

public class Concept extends NodeProperties {

  public static final String PREFERRED_LABEL = "preferedLabel";
  public static final String SYNONYM = "synonym";
  public static final String ACRONYM = "acronym";
  public static final String ABREVIATION = "abbreviation";
  public static final String DEFINITION = "definition";
  public static final String CATEGORY = "category";

  private String preferredLabel;
  private Set<String> definitions = new HashSet<>();
  private Set<String> categories = new HashSet<>();
  private Set<String> synonyms = new HashSet<>();
  private Set<String> acronyms = new HashSet<>();
  private Set<String> abbreviations = new HashSet<>();

  private Set<String> equivalentClasses = new HashSet<>();

  public void setPreferredLabel(String preferredLabel) {
    this.preferredLabel = preferredLabel;
  }

  public String getPreferredLabel() {
    return preferredLabel;
  }

  public Iterable<String> getSynonyms() {
    return synonyms;
  }

  public void addSynonym(String synonym) {
    synonyms.add(synonym);
  }

  public Iterable<String> getAcronyms() {
    return acronyms;
  }

  public void addAcronym(String acronym) {
    acronyms.add(acronym);
  }

  public Iterable<String> getAbbreviations() {
    return abbreviations;
  }

  public void addAbbreviation(String abbreviation) {
    abbreviations.add(abbreviation);
  }

  public void addDefinition(String definition) {
    this.definitions.add(definition);
  }

  public Iterable<String> getDefinitions() {
    return definitions;
  }

  public void addCategory(String category) {
    categories.add(category);
  }

  public Iterable<String> getCategories() {
    return categories;
  }

  public Set<String> getEquivalentClasses() {
    return equivalentClasses;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Concept) {
      return ((Concept) obj).getId() == getId();
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Long.valueOf(getId()).hashCode();
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", Iterables.toString(getLabels()), this.getFragment());
  }

}
