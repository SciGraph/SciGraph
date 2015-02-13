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
package edu.sdsc.scigraph.services.configuration;

import java.util.Collections;
import java.util.List;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import edu.sdsc.scigraph.neo4j.OntologyConfiguration;
import edu.sdsc.scigraph.services.jersey.dynamic.CypherResourceConfig;
import edu.sdsc.scigraph.services.refine.ServiceMetadata;

public class ApplicationConfiguration extends Configuration {

  @Valid
  @JsonProperty
  private String applicationContextPath;

  @Valid
  @NotNull
  @JsonProperty
  private OntologyConfiguration graphConfiguration = new OntologyConfiguration();

  @Valid
  @JsonProperty(required=false)
  private Optional<ApiConfiguration> apiConfiguration = Optional.absent();

  @Valid
  @JsonProperty(required=false)
  private Optional<ServiceMetadata> serviceMetadata = Optional.absent();

  @Valid
  @JsonProperty(required=false)
  private List<CypherResourceConfig> cypherResources = Collections.emptyList();
  
  public String getApplicationContextPath() {
    return applicationContextPath;
  }

  public OntologyConfiguration getGraphConfiguration() {
    return graphConfiguration;
  }

  public Optional<ApiConfiguration> getApiConfiguration() {
    return apiConfiguration;
  }

  public Optional<ServiceMetadata> getServiceMetadata() {
    return serviceMetadata;
  }
  
  public List<CypherResourceConfig> getCypherResources() {
    return cypherResources;
  }

}
