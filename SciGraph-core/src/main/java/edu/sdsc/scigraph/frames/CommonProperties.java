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
package edu.sdsc.scigraph.frames;

import com.tinkerpop.frames.Property;

/***
 * Properties common to both nodes and edges.
 */
public interface CommonProperties {

  public final static String URI = "uri";
  public final static String FRAGMENT = "fragment";
  public final static String CURIE = "curie";
  public final static String TYPE = "type"; // TODO: Could be replaced by neo4j 2.0 labels
  public final static String NEGATED = "negated";
  public static final String PARENT_ONTOLOGY = "parentOntology";
  public static final String ONTOLOGY = "ontology";
  public static final String ONTOLOGY_VERSION = "ontologyVersion";

  @Property(URI)
  public void setUri(String uri);

  @Property(URI)
  public String getUri();

  @Property(FRAGMENT)
  public void setFragment(String uri);

  @Property(FRAGMENT)
  public String getFragment();

  @Property(CURIE)
  public void setCurie(String curie);

  @Property(CURIE)
  public String getCurie();

  @Property(NEGATED)
  public void setNegated(boolean negated);

  @Property(NEGATED)
  public boolean isNegated();

  @Property(PARENT_ONTOLOGY)
  public void setParentOntology(String ontology);

  @Property(PARENT_ONTOLOGY)
  public String getParentOntology();
  
  @Property(ONTOLOGY)
  public void setOntology(String ontology);

  @Property(ONTOLOGY)
  public String getOntology();

  @Property(ONTOLOGY_VERSION)
  public void setOntologyVersion(String version);

  @Property(ONTOLOGY_VERSION)
  public String getOntologyVersion();

  @Property(TYPE)
  public void addType(String type);

  @Property(TYPE)
  public Iterable<String> getTypes();

}
