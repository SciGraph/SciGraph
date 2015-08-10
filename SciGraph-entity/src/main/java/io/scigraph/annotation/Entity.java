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
package io.scigraph.annotation;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

// TODO: Can this be replaced by Concept?
public final class Entity {

  private final Set<String> term;

  private final String id;

  private Set<String> categories;

  public Entity(Iterable<String> terms, String id) {
    this(terms, id, Collections.<String> emptySet());
  }

  public Entity(String term, String id) {
    this(newArrayList(term), id, Collections.<String> emptySet());
  }

  public Entity(Iterable<String> terms, String id, Iterable<String> categories) {
    this.term = ImmutableSet.copyOf(terms);
    this.id = id;
    this.categories = ImmutableSet.copyOf(categories);
  }

  public Set<String> getTerms() {
    return term;
  }

  public String getId() {
    return id;
  }

  public Set<String> getCategories() {
    return categories;
  }

  protected static String escape(String value) {
    return (null == value) ? "" : value.replace(",", "\\,").replace("|", "\\|");
  }

  /***
   * @return a serialized version of the entity
   */
  public String serialize() {
    return Joiner.on(",")
        .join(escape(getFirst(getTerms(), "")), escape(getId()), escape(getFirst(categories, "")));
  }

  @Override
  public String toString() {
    return format("%s (%s)", term, id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(term, id);
  }

  @Override
  public boolean equals(Object obj) {
    if (null == obj) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Entity other = (Entity) obj;
    return Objects.equals(term, other.term) && Objects.equals(id, other.id);
  }

}
