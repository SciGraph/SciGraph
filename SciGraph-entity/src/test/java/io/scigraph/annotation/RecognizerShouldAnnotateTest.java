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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.scigraph.annotation.EntityFormatConfiguration;
import io.scigraph.annotation.EntityRecognizer;
import io.scigraph.frames.Concept;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

public class RecognizerShouldAnnotateTest {

  EntityFormatConfiguration config = mock(EntityFormatConfiguration.class);
  Concept concept = new Concept(1L);
  EntityRecognizer recognizer;

  @Before
  public void setUp() throws Exception {
    recognizer = new EntityRecognizer(null, null);
    concept.setIri("http://x.org/1");
    concept.getLabels().add("Label");
    when(config.getExcludeCategories()).thenReturn(Collections.<String> emptySet());
  }

  @Test
  public void testNonExcludedCategory() {
    when(config.getExcludeCategories()).thenReturn(singleton("foo"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(true));
  }

  @Test
  public void testExcludedCategory() {
    when(config.getExcludeCategories()).thenReturn(singleton("foo"));
    concept.getCategories().add("foo");
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testExcludedCategories() {
    when(config.getExcludeCategories()).thenReturn(newHashSet("foo", "bar"));
    concept.getCategories().addAll(newHashSet("foo", "baz"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testInclusion() {
    when(config.getIncludeCategories()).thenReturn(singleton("foo"));
    concept.getCategories().addAll(newHashSet("foo", "baz"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(true));
  }

  @Test
  public void testNotListedInclusion() {
    when(config.getIncludeCategories()).thenReturn(singleton("foo"));
    concept.getCategories().addAll(newHashSet("faz", "baz"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testNumericExclusion() {
    when(config.isIncludeNumbers()).thenReturn(false);
    concept.getLabels().add("123");
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testNumericInclusion() {
    when(config.isIncludeNumbers()).thenReturn(true);
    concept.getLabels().add("123");
    assertThat(recognizer.shouldAnnotate(concept, config), is(true));
  }

}
