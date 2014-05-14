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
package edu.sdsc.scigraph.services.api.graph;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement(name="class")
@JsonInclude(Include.NON_DEFAULT)
public class ConceptDTOLite {

  private String uri;
  private String label;
  private String fragment;
  private String curie;
  private Collection<String> categories;
  private Collection<String> synonyms;
  private Collection<String> acronyms;
  private Collection<String> abbreviations;

  @XmlAttribute
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @XmlAttribute
  public String getFragment() {
    return fragment;
  }

  public void setFragment(String fragment) {
    this.fragment = fragment;
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

}
