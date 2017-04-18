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
package io.scigraph.services.refine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RefineQuery {

  String query;

  Optional<Integer> limit = Optional.empty();

  Optional<String> type = Optional.empty();

  Optional<String> type_strict = Optional.empty();

  Map<String, Object> properties = new HashMap<>();

  /***
   * @return A string to search for
   */
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  /***
   * @return An integer to specify how many results to return
   */
  public Optional<Integer> getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = Optional.of(limit);
  }

  /*** 
   * @return A single string, or an array of strings, specifying the types of result e.g., person, product,
   *  ... The actual format of each type depends on the service 
   *  (e.g., "/government/politician" as a Freebase type).
   */
  public Optional<String> getType() {
    return type;
  }

  public void setType(String type) {
    this.type = Optional.of(type);
  }

  /***
   * @return A string, one of "any", "all", "should"
   */
  public Optional<String> getType_strict() {
    return type_strict;
  }

  public void setType_strict(String type_strict) {
    this.type_strict = Optional.of(type_strict);
  }

  /***
   * @return Array of json object literals.
   */
  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public String toString() {
    return String.format("%s", query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(query, limit, type, type_strict, properties);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RefineQuery other = (RefineQuery) obj;
    return Objects.equals(this.query, other.query)
        && Objects.equals(this.limit, other.limit)
        && Objects.equals(this.type, other.type)
        && Objects.equals(this.type_strict, other.type_strict)
        && Objects.equals(this.properties, other.properties);
  }

}
