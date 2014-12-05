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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/***
 * Properties common to both nodes and edges.
 */
public class CommonProperties {

  public static final String URI = "uri";
  public static final String FRAGMENT = "fragment";
  public static final String NEGATED = "negated";
  public static final String PARENT_ONTOLOGY = "parentOntology";
  public static final String ONTOLOGY = "ontology";
  public static final String ONTOLOGY_VERSION = "ontologyVersion";
  public static final String CONVENIENCE = "convenience";
  public static final String OWL_TYPE = "owlType";

  private long id;
  private String uri;
  private String fragment;
  private boolean negated;
  private String parentOntology;
  private String ontology;
  private String ontologyVersion;
  private Set<String> types = new HashSet<>();

  public void setId(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getUri() {
    return uri;
  }

  public void setFragment(String fragment) {
    this.fragment = fragment;
  }

  public String getFragment() {
    return fragment;
  }

  public void setNegated(boolean negated) {
    this.negated = negated;
  }

  public boolean isNegated() {
    return negated;
  }

  public void setParentOntology(String ontology) {
    this.parentOntology = ontology;
  }

  public String getParentOntology() {
    return parentOntology;
  }
  
  public void setOntology(String ontology) {
    this.ontology = ontology;
  }

  public String getOntology() {
    return ontology;
  }

  public void setOntologyVersion(String version) {
    this.ontologyVersion = version;
  }

  public String getOntologyVersion() {
    return ontologyVersion;
  }

  public void addType(String type) {
    types.add(type);
  }

  public Collection<String> getTypes() {
    return types;
  }

}
