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
package edu.sdsc.scigraph.services.swagger.beans.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.CacheControl;

public class Apis {

  private static final int MAX_AGE = 60 * 60 * 24;

  private List<Operations> operations = new ArrayList<>();
  private String path;
  private String query;
  private CacheControl cacheControl = new CacheControl();

  public Apis() {
    cacheControl.setMaxAge(MAX_AGE);
  }

  public List<Operations> getOperations() {
    return this.operations;
  }

  public void setOperations(List<Operations> operations) {
    this.operations = operations;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return query;
  }

  public CacheControl getCacheControl() {
    return cacheControl;
  }

  public void setCacheControl(CacheControl cacheControl) {
    this.cacheControl = cacheControl;
  }

}
