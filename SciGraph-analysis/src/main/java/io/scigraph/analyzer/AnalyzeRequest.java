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
package io.scigraph.analyzer;

import java.util.Collection;
import java.util.HashSet;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalyzeRequest {

  @NotEmpty
  @NotNull
  @JsonProperty
  private Collection<String> samples = new HashSet<>();

  @NotEmpty
  @NotNull
  @JsonProperty
  private String ontologyClass;

  @NotEmpty
  @NotNull
  @JsonProperty
  private String path;

  public Collection<String> getSamples() {
    return samples;
  }

  public void setSamples(Collection<String> samples) {
    this.samples = samples;
  }

  public String getOntologyClass() {
    return ontologyClass;
  }

  public void setOntologyClass(String ontologyClass) {
    this.ontologyClass = ontologyClass;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

}
