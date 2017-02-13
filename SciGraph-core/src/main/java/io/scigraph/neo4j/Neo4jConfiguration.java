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
package io.scigraph.neo4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Neo4jConfiguration {

  @NotEmpty
  @JsonProperty
  private String location;

  private Map<String, String> curies = new HashMap<>();
  private Map<String, String> neo4jConfig = new HashMap<>();

  private Set<String> indexedNodeProperties = new HashSet<>();
  private Set<String> exactNodeProperties = new HashSet<>();
  
  private Map<String, Set<String>> schemaIndexes = new HashMap<>();

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Map<String, String> getCuries() {
    return curies;
  }

  public Map<String, String> getNeo4jConfig() {
    return neo4jConfig;
  }

  public Set<String> getIndexedNodeProperties() {
    return indexedNodeProperties;
  }

  public Set<String> getExactNodeProperties() {
    return exactNodeProperties;
  }
  
  public void setSchemaIndexes(Map<String, Set<String>> schemaIndexes) {
    this.schemaIndexes = schemaIndexes;
  }
  
  public Map<String, Set<String>> getSchemaIndexes() {
    return schemaIndexes;
  }

}
