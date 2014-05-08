package edu.sdsc.scigraph.annotation;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import edu.sdsc.scigraph.frames.Concept;

public class RecognizerShouldAnnotateTest {

  EntityFormatConfiguration config = mock(EntityFormatConfiguration.class);
  Concept concept = mock(Concept.class);
  EntityRecognizer recognizer;

  @Before
  public void setUp() throws Exception {
    recognizer = new EntityRecognizer(null);
    when(concept.getLabel()).thenReturn("Label");
    when(concept.getCategories()).thenReturn(Collections.<String> emptySet());
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
    when(concept.getCategories()).thenReturn(singleton("foo"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testExcludedCategories() {
    when(config.getExcludeCategories()).thenReturn(newHashSet("foo", "bar"));
    when(concept.getCategories()).thenReturn(newHashSet("foo", "baz"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testInclusion() {
    when(config.getIncludeCategories()).thenReturn(singleton("foo"));
    when(concept.getCategories()).thenReturn(newHashSet("foo", "baz"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(true));
  }

  @Test
  public void testNotListedInclusion() {
    when(config.getIncludeCategories()).thenReturn(singleton("foo"));
    when(concept.getCategories()).thenReturn(newHashSet("faz", "baz"));
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testLengthExclusion() {
    when(config.getMinLength()).thenReturn(1000);
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testNumericExclusion() {
    when(config.isIncludeNumbers()).thenReturn(false);
    when(concept.getLabel()).thenReturn("123");
    assertThat(recognizer.shouldAnnotate(concept, config), is(false));
  }

  @Test
  public void testNumericInclusion() {
    when(config.isIncludeNumbers()).thenReturn(true);
    when(concept.getLabel()).thenReturn("123");
    assertThat(recognizer.shouldAnnotate(concept, config), is(true));
  }

}
