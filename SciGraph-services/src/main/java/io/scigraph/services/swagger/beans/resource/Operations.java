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
package io.scigraph.services.swagger.beans.resource;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Collections2.filter;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Predicate;

public class Operations {
  private String method = "GET";
  private String nickname;
  private String notes;
  private List<Parameters> parameters;
  private String summary;
  private String type;

  @JsonProperty
  public String getMethod() {
    return this.method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  @JsonProperty
  public String getNickname() {
    if (!isNullOrEmpty(nickname)) {
      return nickname;
    } else if (!isNullOrEmpty(summary)) {
      return summary.toLowerCase().replaceAll("\\s+", "_");
    } else {
      return "nickname";
    }
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  @JsonProperty
  public String getNotes() {
    return this.notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @JsonProperty
  public List<Parameters> getParameters() {
    return this.parameters;
  }

  public Collection<Parameters> getParametersOfType(final String type) {
    return filter(parameters, new Predicate<Parameters>() {
      @Override
      public boolean apply(Parameters parameter) {
        return type.equals(parameter.getType());
      }
    });
  }

  public void setParameters(List<Parameters> parameters) {
    this.parameters = parameters;
  }

  @JsonProperty
  public String getSummary() {
    return this.summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
