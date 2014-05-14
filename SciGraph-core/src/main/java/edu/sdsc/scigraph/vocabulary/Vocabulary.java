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
package edu.sdsc.scigraph.vocabulary;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.NodeProperties;

/***
 * A vocabulary allows interaction with a backing store of terms, ids, categories, and ontologies.
 * <p>It is designed to be used for concept retrieval from IDs or terms and for auto-completion.
 * 
 * @param N The {@link NodeProperties} to return
 */
public interface Vocabulary<N> {

  /***
   * Get a concept from a URI
   * @param uri  the URI
   * @return an optional concept matching the URI
   */
  public Optional<N> getConceptFromUri(String uri);

  /***
   * Get concepts that match either a URI fragment or a CURIE.
   * <p>Due to differences in representation "fragment" means either:
   * <p><ul>
   * <li>http://example.org/thing#<b>fragment</b>
   * <li>http://example.org/thing/<b>fragment</b>
   * </ul>
   * <p>
   * CURIE prefixes must be specified during ontology loading.
   * Given the following mapping: <i>http://example.org/CUR_ -> CUR</i> a concept with URI http://example.org/CUR_1 
   * would be retrievable as CUR:1.
   * 
   * @param query  a {@link Query} with the fragment or CURIE as input
   * @return concepts matching the query. Uniqueness is probable but not guaranteed.
   */
  public Collection<N> getConceptFromId(Query query);

  /***
   * Gets concepts from a prefix string - useful for auto-complete
   * 
   * @param query  a {@link Query} with the prefix as input
   * @return a list of matching concepts
   */
  public List<N> getConceptsFromPrefix(Query query);

  /***
   * Search concepts as free text.
   * <p>The label of the resulting concept may not be prefixed with the search term 
   * (ie: "foo bar" could be returned by a search for "bar").
   * 
   * @param query  a {@link Query} with the term as input
   * @return a list of matching concepts
   */
  public List<N> searchConcepts(Query query);

  /***
   * Attempts to match the label of a concept as closely as possible ("exact-ish" match).
   * <p>The extent of "exact-ish" depends on the implementing class. It may include:
   * <p><ul>
   * <li>stemming
   * <li>lowercasing
   * <li>lemmatization
   * </ul>
   * A best attempt to match the complete label should be made 
   * (ie: "foo bar" would not be returned by a search for "bar").
   * 
   * @param query  a {@link Query} with the term as input
   * @return a list of matching concepts
   */
  public List<N> getConceptsFromTerm(Query query);

  /***
   * @return a set of categories in the vocabulary
   */
  public Set<String> getAllCategories();

  /*** 
   * @return a set of ontologies in the vocabulary
   */
  public Set<String> getAllOntologies();

  /***
   * Provides "did you mean" functionality based on the labels of concepts in the vocabulary.
   * @param query  a query string
   * @return a list of suggestions
   */
  public List<String> getSuggestions(String query);

  /***
   * A builder class with common query refinement options.
   */
  public static class Query {
    private final String input;
    private final int limit;
    private final boolean includeSynonyms;
    private final boolean includeAcronyms;
    private final boolean includeAbbreviations;
    private final Collection<String> ontologies;
    private final Collection<String> categories;

    public static class Builder {
      private final String input;
      private int limit = 1000;
      private boolean includeSynonyms = true;
      private boolean includeAcronyms = false;
      private boolean includeAbbreviations = false;
      private Collection<String> ontologies = new HashSet<>();
      private Collection<String> categories = new HashSet<>();

      /***
       * The input could be an ID, a fragment, a CURIE, or a term.
       * 
       * @param input  the relevant input for the query.
       */
      public Builder(String input) {
        this.input = input;
      }

      /***
       * @param limit  the maximum number results to return
       * @return the builder
       */
      public Builder limit(int limit) {
        this.limit = limit; return this;
      }

      public Builder includeSynonyms(boolean include) {
        this.includeSynonyms = include; return this;
      }

      public Builder includeAcronyms(boolean include) {
        this.includeAcronyms = include; return this;
      }

      public Builder includeAbbreviations(boolean include) {
        this.includeAbbreviations = include; return this;
      }

      /***
       * @param ontologies  a set of required ontologies
       * @return the builder
       */
      public Builder ontologies(Collection<String> ontologies) {
        this.ontologies = newHashSet(ontologies); return this;
      }

      /***
       * @param categories  a set of required categories
       * @return the builder
       */
      public Builder categories(Collection<String> categories) {
        this.categories = newHashSet(categories); return this;
      }

      /***
       * @return the built query
       */
      public Query build() {
        return new Query(this);
      }

    }

    private Query(Builder builder) {
      this.input = builder.input;
      this.limit = builder.limit;
      this.includeSynonyms = builder.includeSynonyms;
      this.includeAcronyms = builder.includeAcronyms;
      this.includeAbbreviations = builder.includeAbbreviations;
      this.ontologies = builder.ontologies;
      this.categories = builder.categories;
    }

    public String getInput() {
      return input;
    }

    public int getLimit() {
      return limit;
    }

    public boolean isIncludeSynonyms() {
      return includeSynonyms;
    }

    public boolean isIncludeAcronyms() {
      return includeAcronyms;
    }

    public boolean isIncludeAbbreviations() {
      return includeAbbreviations;
    }

    public Collection<String> getOntologies() {
      return ontologies;
    }

    public Collection<String> getCategories() {
      return categories;
    }

  }

}
