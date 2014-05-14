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
package edu.sdsc.scigraph.services.configuration;

import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.sdsc.scigraph.neo4j.OntologyConfiguration;

public class ApplicationConfiguration extends Configuration {

  @Valid
  @JsonProperty
  private String applicationContextPath;

  @Valid
  @NotNull
  @JsonProperty
  private FederationConfiguration federationConfiguration = new FederationConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private OntologyConfiguration graphConfiguration = new OntologyConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private ApiConfiguration apiConfiguration = new ApiConfiguration();

  public String getApplicationContextPath() {
    return applicationContextPath;
  }

  public FederationConfiguration getFederationConfiguration() {
    return federationConfiguration;
  }

  public OntologyConfiguration getGraphConfiguration() {
    return graphConfiguration;
  }

  public ApiConfiguration getApiConfiguration() {
    return apiConfiguration;
  }

}
