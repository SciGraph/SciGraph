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

import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import org.apache.commons.io.output.NullWriter;

import net.htmlparser.jericho.HTMLElementName;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

public class EntityFormatConfiguration {

  private Reader reader;
  private final Writer writer;
  private final List<String> stylesheets;
  private final List<String> scripts;
  private final String spanClassName;
  private final String dataAttrName;
  private final Set<String> targetIds;
  private final Set<String> targetClasses;
  private final Set<String> ignoreTags;
  private final Set<String> includeCategories;
  private final Set<String> excludeCategories;
  private final boolean longestOnly;
  private final boolean includeAcronyms;
  private final boolean includeAbbreviations;
  private final boolean includeNumbers;
  private final String query;
  private final URL url;
  private final int minLength;

  public static class Builder implements Supplier<EntityFormatConfiguration> {

    /***
     * The default set of categories to ignore
     */
    public final static Set<String> DEFAULT_IGNORABLE_CATEGORIES = ImmutableSet.of("cell role",
        "chemical role", "data role", "familal role", "gene", "molecule", "molecule role", "null",
        "quality");

    /***
     * The default set of HTML tags to ignore
     */
    public final static Set<String> DEFAULT_IGNORABLE_TAGS = ImmutableSet.of(
        HTMLElementName.SCRIPT, HTMLElementName.HEAD);

    private final Reader reader;
    private Writer writer = new NullWriter();

    private List<String> stylesheets = Collections.emptyList();
    private List<String> scripts = Collections.emptyList();

    private Set<String> targetIds = Collections.emptySet();
    private Set<String> targetClasses = Collections.emptySet();
    private Set<String> ignoreTags = DEFAULT_IGNORABLE_TAGS;

    private Set<String> includeCategories = Collections.emptySet();
    private Set<String> excludeCategories = DEFAULT_IGNORABLE_CATEGORIES;

    private String spanClassName = "graph-annotation";
    private String dataAttrName = "data-sciGraph";

    private int minLength = 3;

    private String query;

    private boolean longestOnly = true;
    private boolean includeAcronyms = false;
    private boolean includeAbbreviations = false;
    private boolean includeNumbers = false;

    private URL url = null;

    /***
     * @param reader
     *          A Reader with the content to annotate
     */
    public Builder(Reader reader) {
      checkNotNull(reader);
      this.reader = reader;
    }

    /***
     * @param writer
     *          A writer for the annotated content
     * @return Builder
     */
    public Builder writeTo(Writer writer) {
      checkNotNull(writer);
      this.writer = writer;
      return this;
    }

    /***
     * Specify stylesheets to inject into the HTML. These sheets will only be
     * added if a &lt;head&gt; element is found in the content.
     * 
     * @param stylesheets
     *          A list of stylesheets to inject
     * @return Builder
     */
    public Builder stylesheets(List<String> stylesheets) {
      checkNotNull(stylesheets);
      this.stylesheets = newArrayList(stylesheets);
      return this;
    }

    /***
     * Specify scripts to inject into the HTML. These scripts will only be added
     * if a &lt;head&gt; element is found in the content.
     * 
     * @param scripts
     *          A list of scripts to inject
     * @return Builder
     */
    public Builder scripts(List<String> scripts) {
      checkNotNull(scripts);
      this.scripts = newArrayList(scripts);
      return this;
    }

    public Builder spanClassName(String spanClassName) {
      checkNotNull(spanClassName);
      this.spanClassName = spanClassName;
      return this;
    }

    public Builder dataAttrName(String dataAttrName) {
      checkNotNull(dataAttrName);
      this.dataAttrName = dataAttrName;
      return this;
    }

    /***
     * @param targetClasses
     *          The target CSS classes to annotate
     * @return Builder
     */
    public Builder targetClasses(Set<String> targetClasses) {
      checkNotNull(targetClasses);
      this.targetClasses = newHashSet(targetClasses);
      return this;
    }

    /***
     * @param targetIds
     *          The target element IDs to annotate
     * @return Builder
     */
    public Builder targetIds(Set<String> targetIds) {
      checkNotNull(targetIds);
      this.targetIds = newHashSet(targetIds);
      return this;
    }

    /***
     * @param ignoreTags
     *          HTML tags that should be ignored
     * @return Builder
     */
    public Builder ignoreTags(Set<String> ignoreTags) {
      checkNotNull(ignoreTags);
      this.ignoreTags = newHashSet(ignoreTags);
      return this;
    }

    /***
     * A set of categories to include. If the set is empty (the default) then
     * all categories will be included. Exclusion takes precedence over
     * inclusion.
     * 
     * @param categories
     *          a set of categories to include
     * @return Builder
     */
    public Builder includeCategories(Set<String> categories) {
      checkNotNull(categories);
      this.includeCategories = newHashSet(categories);
      return this;
    }

    /***
     * Exclude terms based on categories. Exclusion takes precedence over
     * inclusion.
     * 
     * @param categories
     *          a set of categories to exclude
     * @return Builder
     */
    public Builder excludeCategories(Set<String> categories) {
      checkNotNull(categories);
      this.excludeCategories = newHashSet(categories);
      return this;
    }

    /***
     * @param longestOnly
     *          Only return the longest annotations from a group
     * @return Builder
     */
    public Builder longestOnly(boolean longestOnly) {
      this.longestOnly = longestOnly;
      return this;
    }

    /***
     * @param include
     *          Should acronyms be included
     * @return Builder
     */
    public Builder includeAncronyms(boolean include) {
      this.includeAcronyms = include;
      return this;
    }

    /***
     * @param include
     *          Should abbreviations be included
     * @return Builder
     */
    public Builder includeAbbreviations(boolean include) {
      this.includeAbbreviations = include;
      return this;
    }

    /***
     * @param include
     *          Should numbers be annotated
     * @return Builder
     */
    public Builder includeNumbers(boolean include) {
      this.includeNumbers = include;
      return this;
    }

    /***
     * @param query
     *          The query that was used to
     * @return Builder
     */
    public Builder query(String query) {
      this.query = query;
      return this;
    }

    /***
     * If this is specified then a base attribute will be added to the
     * &lt;head&gt; element to ensure that relative links resolve properly.
     * 
     * @param url
     *          The url that the Reader is coming from
     * @return Builder
     */
    public Builder url(URL url) {
      this.url = url;
      return this;
    }

    /***
     * @param minLength
     *          The minimum length threshold for entities
     * @return Builder
     */
    public Builder minLength(int minLength) {
      this.minLength = minLength;
      return this;
    }

    @Override
    public EntityFormatConfiguration get() {
      return new EntityFormatConfiguration(this);
    }

  }

  private EntityFormatConfiguration(Builder builder) {
    reader = builder.reader;
    writer = builder.writer;
    stylesheets = builder.stylesheets;
    scripts = builder.scripts;
    spanClassName = builder.spanClassName;
    dataAttrName = builder.dataAttrName;
    targetClasses = builder.targetClasses;
    targetIds = builder.targetIds;
    ignoreTags = builder.ignoreTags;
    includeCategories = builder.includeCategories;
    excludeCategories = builder.excludeCategories;
    longestOnly = builder.longestOnly;
    includeAcronyms = builder.includeAcronyms;
    includeAbbreviations = builder.includeAbbreviations;
    includeNumbers = builder.includeNumbers;
    query = builder.query;
    url = builder.url;
    minLength = builder.minLength;
  }

  /**
   * @return the reader
   */
  public Reader getReader() {
    return reader;
  }

  public void setReader(Reader reader) {
    this.reader = reader;
  }

  /**
   * @return the writer
   */
  public Writer getWriter() {
    return writer;
  }

  /**
   * @return the stylesheets
   */
  public List<String> getStylesheets() {
    return stylesheets;
  }

  /**
   * @return the scripts
   */
  public List<String> getScripts() {
    return scripts;
  }

  /**
   * @return the spanClassName
   */
  public String getSpanClassName() {
    return spanClassName;
  }

  /**
   * @return the dataAttrName
   */
  public String getDataAttrName() {
    return dataAttrName;
  }

  /**
   * @return the targetIds
   */
  public Set<String> getTargetIds() {
    return targetIds;
  }

  /**
   * @return the targetClasses
   */
  public Set<String> getTargetClasses() {
    return targetClasses;
  }

  /**
   * @return the ignoreTags
   */
  public Set<String> getIgnoreTags() {
    return ignoreTags;
  }

  /**
   * @return the includeCategories
   */
  public Set<String> getIncludeCategories() {
    return includeCategories;
  }

  /**
   * @return the excludeCategories
   */
  public Set<String> getExcludeCategories() {
    return excludeCategories;
  }

  /**
   * @return the longestOnly
   */
  public boolean isLongestOnly() {
    return longestOnly;
  }

  public boolean isIncludeAcronyms() {
    return includeAcronyms;
  }

  public boolean isIncludeAbbreviations() {
    return includeAbbreviations;
  }

  public boolean isIncludeNumbers() {
    return includeNumbers;
  }

  public String getQuery() {
    return query;
  }

  public URL getUrl() {
    return url;
  }

  public int getMinLength() {
    return minLength;
  }

}
