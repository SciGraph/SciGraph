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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.scigraph.annotation.EntityFormatConfiguration;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class EntityFormatConfigurationTest {

  EntityFormatConfiguration.Builder builder;
  Reader reader;

  @Before
  public void setup() {
    reader = new StringReader("");
    builder = new EntityFormatConfiguration.Builder(reader);
  }

  @Test
  public void verifyAllBuilderOptions() throws MalformedURLException {
    Set<String> excludeCats = newHashSet("exclude");
    Set<String> includeCats = newHashSet("include");
    Set<String> ignoreTags = newHashSet("ignore");
    List<String> scripts = newArrayList("script");
    List<String> stylesheets = newArrayList("stylesheet");
    Set<String> classes = newHashSet("class");
    Set<String> ids = newHashSet("id");
    URL url = new URL("http://example.org");
    Writer writer = new StringWriter();
    builder.dataAttrName("dataAttrName")
    .excludeCategories(excludeCats)
    .ignoreTags(ignoreTags)
    .includeAbbreviations(true)
    .includeAncronyms(true)
    .includeCategories(includeCats)
    .includeNumbers(true)
    .longestOnly(true)
    .minLength(5)
    .query("query")
    .scripts(scripts)
    .spanClassName("span")
    .stylesheets(stylesheets)
    .targetClasses(classes)
    .targetIds(ids)
    .url(url)
    .writeTo(writer);
    EntityFormatConfiguration config = builder.get();
    assertThat(config.getDataAttrName(), is("dataAttrName"));
    assertThat(config.getExcludeCategories(), is(excludeCats));
    assertThat(config.getIgnoreTags(), is(ignoreTags));
    assertThat(config.getIncludeCategories(), is(includeCats));
    assertThat(config.getMinLength(), is(5));
    assertThat(config.getQuery(), is("query"));
    assertThat(config.getReader(), is(reader));
    assertThat(config.getScripts(), is(scripts));
    assertThat(config.getSpanClassName(), is("span"));
    assertThat(config.getStylesheets(), is(stylesheets));
    assertThat(config.getTargetClasses(), is(classes));
    assertThat(config.getTargetIds(), is(ids));
    assertThat(config.getUrl(), is(url));
    assertThat(config.getWriter(), is(writer));
    assertThat(config.isIncludeAbbreviations(), is(true));
    assertThat(config.isIncludeAcronyms(), is(true));
    assertThat(config.isIncludeNumbers(), is(true));
    assertThat(config.isLongestOnly(), is(true));
  }

  @Test
  public void setReader() {
    EntityFormatConfiguration config = builder.get();
    Reader newReader = new StringReader("");
    config.setReader(newReader);
    assertThat(config.getReader(), is(newReader));
  }

}
