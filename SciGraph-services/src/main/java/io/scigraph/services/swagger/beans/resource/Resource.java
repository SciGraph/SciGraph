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

import java.util.ArrayList;
import java.util.List;

public class Resource {
  private String apiVersion;
  private List<Apis> apis = new ArrayList<>();
  private String basePath;
  private List<String> models = new ArrayList<>();
  private List<String> produces;
  private String resourcePath;
  private String swaggerVersion;

  public String getApiVersion() {
    return this.apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public List<Apis> getApis() {
    return this.apis;
  }

  public void setApis(List<Apis> apis) {
    this.apis = apis;
  }

  public String getBasePath() {
    return this.basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public List<String> getModels() {
    return this.models;
  }

  public void setModels(List<String> models) {
    this.models = models;
  }

  public List<String> getProduces() {
    return this.produces;
  }

  public void setProduces(List<String> produces) {
    this.produces = produces;
  }

  public String getResourcePath() {
    return this.resourcePath;
  }

  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public String getSwaggerVersion() {
    return this.swaggerVersion;
  }

  public void setSwaggerVersion(String swaggerVersion) {
    this.swaggerVersion = swaggerVersion;
  }
}
