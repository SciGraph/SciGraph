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

import com.tinkerpop.frames.Property;

/***
 * Properties common to both nodes and edges.
 */
public interface CommonProperties {

  String URI = "uri";
  String FRAGMENT = "fragment";
  String CURIE = "curie";
  String TYPE = "type"; // TODO: Could be replaced by neo4j 2.0 labels
  String NEGATED = "negated";
  String PARENT_ONTOLOGY = "parentOntology";
  String ONTOLOGY = "ontology";
  String ONTOLOGY_VERSION = "ontologyVersion";

  @Property(URI)
  void setUri(String uri);

  @Property(URI)
  String getUri();

  @Property(FRAGMENT)
  void setFragment(String uri);

  @Property(FRAGMENT)
  String getFragment();

  @Property(CURIE)
  void setCurie(String curie);

  @Property(CURIE)
  String getCurie();

  @Property(NEGATED)
  void setNegated(boolean negated);

  @Property(NEGATED)
  boolean isNegated();

  @Property(PARENT_ONTOLOGY)
  void setParentOntology(String ontology);

  @Property(PARENT_ONTOLOGY)
  String getParentOntology();
  
  @Property(ONTOLOGY)
  void setOntology(String ontology);

  @Property(ONTOLOGY)
  String getOntology();

  @Property(ONTOLOGY_VERSION)
  void setOntologyVersion(String version);

  @Property(ONTOLOGY_VERSION)
  String getOntologyVersion();

  @Property(TYPE)
  void addType(String type);

  @Property(TYPE)
  Iterable<String> getTypes();

}
