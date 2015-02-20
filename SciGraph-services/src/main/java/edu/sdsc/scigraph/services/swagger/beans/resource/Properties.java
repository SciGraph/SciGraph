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
package edu.sdsc.scigraph.services.swagger.beans.resource;

public class Properties {
  private Abbreviations abbreviations;
  private Acronyms acronyms;
  private Anonymous anonymous;
  private Categories categories;
  private Definitions definitions;
  private Deprecated deprecated;
  private EquivalentClasses equivalentClasses;
  private Fragment fragment;
  private Id id;
  private Labels labels;
  private Synonyms synonyms;
  private Types types;
  private Uri uri;

  public Abbreviations getAbbreviations() {
    return this.abbreviations;
  }

  public void setAbbreviations(Abbreviations abbreviations) {
    this.abbreviations = abbreviations;
  }

  public Acronyms getAcronyms() {
    return this.acronyms;
  }

  public void setAcronyms(Acronyms acronyms) {
    this.acronyms = acronyms;
  }

  public Anonymous getAnonymous() {
    return this.anonymous;
  }

  public void setAnonymous(Anonymous anonymous) {
    this.anonymous = anonymous;
  }

  public Categories getCategories() {
    return this.categories;
  }

  public void setCategories(Categories categories) {
    this.categories = categories;
  }

  public Definitions getDefinitions() {
    return this.definitions;
  }

  public void setDefinitions(Definitions definitions) {
    this.definitions = definitions;
  }

  public Deprecated getDeprecated() {
    return this.deprecated;
  }

  public void setDeprecated(Deprecated deprecated) {
    this.deprecated = deprecated;
  }

  public EquivalentClasses getEquivalentClasses() {
    return this.equivalentClasses;
  }

  public void setEquivalentClasses(EquivalentClasses equivalentClasses) {
    this.equivalentClasses = equivalentClasses;
  }

  public Fragment getFragment() {
    return this.fragment;
  }

  public void setFragment(Fragment fragment) {
    this.fragment = fragment;
  }

  public Id getId() {
    return this.id;
  }

  public void setId(Id id) {
    this.id = id;
  }

  public Labels getLabels() {
    return this.labels;
  }

  public void setLabels(Labels labels) {
    this.labels = labels;
  }

  public Synonyms getSynonyms() {
    return this.synonyms;
  }

  public void setSynonyms(Synonyms synonyms) {
    this.synonyms = synonyms;
  }

  public Types getTypes() {
    return this.types;
  }

  public void setTypes(Types types) {
    this.types = types;
  }

  public Uri getUri() {
    return this.uri;
  }

  public void setUri(Uri uri) {
    this.uri = uri;
  }
}
