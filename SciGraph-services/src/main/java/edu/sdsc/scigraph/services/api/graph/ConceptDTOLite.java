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
package edu.sdsc.scigraph.services.api.graph;

import java.util.Collection;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="class")
public class ConceptDTOLite {

  private String iri;
  private Collection<String> labels = new HashSet<>();
  private String curie;
  private Collection<String> categories = new HashSet<>();
  private Collection<String> synonyms = new HashSet<>();
  private Collection<String> acronyms = new HashSet<>();
  private Collection<String> abbreviations = new HashSet<>();
  private boolean deprecated;

  @XmlAttribute
  public String getUri() {
    return iri;
  }

  public void setUri(String uri) {
    this.iri = uri;
  }

  public Collection<String> getLabels() {
    return labels;
  }

  public void setLabels(Collection<String> labels) {
    this.labels = labels;
  }

  @XmlAttribute
  public String getCurie() {
    return curie;
  }

  public void setCurie(String curie) {
    this.curie = curie;
  }

  @XmlElementWrapper(name="categories")
  @XmlElement(name="category")
  public Collection<String> getCategories() {
    return categories;
  }

  public void setCategories(Collection<String> categories) {
    this.categories = categories;
  }

  @XmlElementWrapper(name="synonyms")
  @XmlElement(name="synonym")
  public Collection<String> getSynonyms() {
    return synonyms;
  }

  public void setSynonyms(Collection<String> synonyms) {
    this.synonyms = synonyms;
  }

  @XmlElementWrapper(name="acronyms")
  @XmlElement(name="acronym")
  public Collection<String> getAcronyms() {
    return acronyms;
  }

  public void setAcronyms(Collection<String> acronyms) {
    this.acronyms = acronyms;
  }

  @XmlElementWrapper(name="abbreviations")
  @XmlElement(name="abbreviation")
  public Collection<String> getAbbreviations() {
    return abbreviations;
  }

  public void setAbbreviations(Collection<String> abbreviations) {
    this.abbreviations = abbreviations;
  }

  @XmlAttribute
  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

}
