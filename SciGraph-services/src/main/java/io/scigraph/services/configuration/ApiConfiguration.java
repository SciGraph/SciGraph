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
package io.scigraph.services.configuration;

import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilderSpec;

public class ApiConfiguration {

  @Valid
  @NotNull
  @JsonProperty
  private String apikeyParameter;

  @Valid
  @JsonProperty
  private String defaultApikey;

  @Valid
  @NotNull
  @JsonProperty
  CacheBuilderSpec authenticationCachePolicy;

  @Valid
  @NotNull
  @JsonProperty
  String authenticationQuery;

  @Valid
  @NotNull
  @JsonProperty
  String roleQuery;

  @Valid
  @NotNull
  @JsonProperty
  String permissionQuery;

  @Valid
  @NotNull
  @JsonProperty
  private DataSourceFactory authDataSourceFactory;

  public String getApikeyParameter() {
    return apikeyParameter;
  }

  public String getDefaultApikey() {
    return defaultApikey;
  }

  public CacheBuilderSpec getAuthenticationCachePolicy() {
    return authenticationCachePolicy;
  }

  public String getAuthenticationQuery() {
    return authenticationQuery;
  }

  public String getRoleQuery() {
    return roleQuery;
  }

  public String getPermissionQuery() {
    return permissionQuery;
  }

  public DataSourceFactory getAuthDataSourceFactory() {
    return authDataSourceFactory;
  }

}
