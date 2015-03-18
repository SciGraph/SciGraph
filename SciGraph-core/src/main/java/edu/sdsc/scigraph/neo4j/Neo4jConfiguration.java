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
package edu.sdsc.scigraph.neo4j;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Neo4jConfiguration {

  @NotEmpty
  @JsonProperty
  private String graphLocation;

  private Map<String, String> curies = new HashMap<>();

  private Map<String, String> neo4jConfig = new HashMap<>();

  public String getGraphLocation() {
    return graphLocation;
  }

  public void setGraphLocation(String graphLocation) {
    this.graphLocation = graphLocation;
  }

  public Map<String, String> getCuries() {
    return curies;
  }

  public Map<String, String> getNeo4jConfig() {
    return neo4jConfig;
  }

}
