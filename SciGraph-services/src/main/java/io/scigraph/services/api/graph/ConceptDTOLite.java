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
package io.scigraph.services.api.graph;

import java.util.Collection;
import java.util.HashSet;

public class ConceptDTOLite {

  private String iri;
  private Collection<String> labels = new HashSet<>();
  private String curie;
  private Collection<String> categories = new HashSet<>();
  private Collection<String> synonyms = new HashSet<>();
  private Collection<String> acronyms = new HashSet<>();
  private Collection<String> abbreviations = new HashSet<>();
  private boolean deprecated;

  public String getIri() {
    return iri;
  }

  public void setIri(String iri) {
    this.iri = iri;
  }

  public Collection<String> getLabels() {
    return labels;
  }

  public void setLabels(Collection<String> labels) {
    this.labels = labels;
  }

  public String getCurie() {
    return curie;
  }

  public void setCurie(String curie) {
    this.curie = curie;
  }

  public Collection<String> getCategories() {
    return categories;
  }

  public void setCategories(Collection<String> categories) {
    this.categories = categories;
  }

  public Collection<String> getSynonyms() {
    return synonyms;
  }

  public void setSynonyms(Collection<String> synonyms) {
    this.synonyms = synonyms;
  }

  public Collection<String> getAcronyms() {
    return acronyms;
  }

  public void setAcronyms(Collection<String> acronyms) {
    this.acronyms = acronyms;
  }

  public Collection<String> getAbbreviations() {
    return abbreviations;
  }

  public void setAbbreviations(Collection<String> abbreviations) {
    this.abbreviations = abbreviations;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

}
