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

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.scigraph.annotation.Entity;
import io.scigraph.annotation.EntityAnnotation;
import io.scigraph.annotation.EntityAnnotationGroup;
import io.scigraph.annotation.EntityFormatConfiguration;
import io.scigraph.annotation.EntityProcessorImpl;
import io.scigraph.annotation.EntityRecognizer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class EntityProcessorImplTest {

  EntityProcessorImpl processor;
  EntityFormatConfiguration config = mock(EntityFormatConfiguration.class);

  final static String text = "Sentence about Spinal muscular atrophy (SMA).";

  List<EntityAnnotation> expectedAnnotations = new ArrayList<>();

  Entity mockEntity = new Entity("SMA", "1");
  Entity mockEntity2 = new Entity("muscular atrophy", "1");

  List<EntityAnnotation> annotationList = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    when(config.getDataAttrName()).thenReturn("data-entity");
    EntityRecognizer recognizer = mock(EntityRecognizer.class);

    when(recognizer.getEntities("Spinal muscular atrophy", config)).thenReturn(
        singleton(mockEntity));
    when(recognizer.getEntities("muscular atrophy", config)).thenReturn(singleton(mockEntity2));
    when(recognizer.getEntities("cerebellum", config)).thenReturn(singleton(mockEntity));
    when(recognizer.getEntities("in cerebellum", config)).thenReturn(singleton(mockEntity));
    when(recognizer.getEntities("in cerebellum of", config)).thenReturn(singleton(mockEntity));
    when(recognizer.getEntities("cerebellum of", config)).thenReturn(singleton(mockEntity));
    when(recognizer.getEntities("SMA", config)).thenReturn(singleton(mockEntity));
    when(recognizer.getCssClass()).thenReturn("mock");
    processor = new EntityProcessorImpl(recognizer);

    expectedAnnotations.add(new EntityAnnotation(mockEntity2, 22, 38));
    expectedAnnotations.add(new EntityAnnotation(mockEntity, 15, 38));
    expectedAnnotations.add(new EntityAnnotation(mockEntity, 39, 45));
  }

  @Test
  public void testGetAnnotations() throws IOException, InterruptedException {
    List<EntityAnnotation> annotations = processor.getAnnotations(text, config);
    assertThat(annotations, equalTo(expectedAnnotations));
  }

  @Test
  public void testGetAnnotationsWithMinLength() throws Exception {
    when(config.getMinLength()).thenReturn(Integer.MAX_VALUE);
    List<EntityAnnotation> annotations = processor.getAnnotations(text, config);
    assertThat(annotations, is(empty()));
  }

  @Test
  public void testGetAnnotationsWithEdgeStopWords() throws Exception {
    when(config.isLongestOnly()).thenReturn(true);
    List<EntityAnnotation> annotations = processor.getAnnotations("female in cerebellum of cells", config);

    assertThat(getOnlyElement(annotations).getStart(), is(10));
    assertThat(getOnlyElement(annotations).getEnd(), is(20));
  }

  @Test
  public void testInsertAllSpans() throws Exception {
    // TODO: Hack to get java 7 and 8 tests to pass
    String java7Expected = "Sentence about <span class=\"mock\" data-entity=\"SMA,1,|muscular atrophy,1,\">Spinal muscular atrophy</span>"
        + " <span class=\"mock\" data-entity=\"SMA,1,\">(SMA).</span>";
    String java8Expected = "Sentence about <span class=\"mock\" data-entity=\"muscular atrophy,1,|SMA,1,\">Spinal muscular atrophy</span>"
        + " <span class=\"mock\" data-entity=\"SMA,1,\">(SMA).</span>";
    assertThat(processor.insertSpans(expectedAnnotations, text, config), isOneOf(java7Expected, java8Expected));
  }

  @Test
  public void testInsertLongestOnlySpans() throws Exception {
    when(config.isLongestOnly()).thenReturn(true);
    String expected = "Sentence about <span class=\"mock\" data-entity=\"SMA,1,\">Spinal muscular atrophy</span>"
        + " <span class=\"mock\" data-entity=\"SMA,1,\">(SMA).</span>";
    assertThat(processor.insertSpans(expectedAnnotations, text, config), is(expected));
  }

  @Test
  public void testGetSeparateAnnotationGroups() {
    EntityAnnotation annot1 = new EntityAnnotation(mockEntity, 0, 4); 
    EntityAnnotation annot2 = new EntityAnnotation(mockEntity2, 5, 10);
    annotationList.add(annot1);
    annotationList.add(annot2);
    EntityAnnotationGroup expected1 = new EntityAnnotationGroup();
    expected1.add(annot2);
    EntityAnnotationGroup expected2 = new EntityAnnotationGroup();
    expected2.add(annot1);
    assertThat(EntityProcessorImpl.getAnnotationGroups(annotationList, false), contains(expected1, expected2));
  }

  @Test
  public void testGetJoinedAnnotationGroups() {
    EntityAnnotation annot1 = new EntityAnnotation(mockEntity, 0, 4); 
    EntityAnnotation annot2 = new EntityAnnotation(mockEntity2, 3, 5);
    annotationList.add(annot1);
    annotationList.add(annot2);
    EntityAnnotationGroup expected = new EntityAnnotationGroup();
    expected.add(annot1);
    expected.add(annot2);
    assertThat(EntityProcessorImpl.getAnnotationGroups(annotationList, false), contains(expected));
  }

  @Test
  public void testGetLongestAnnotationGroups() {
    EntityAnnotation annot1 = new EntityAnnotation(mockEntity, 0, 4); 
    EntityAnnotation annot2 = new EntityAnnotation(mockEntity2, 3, 5);
    annotationList.add(annot1);
    annotationList.add(annot2);
    EntityAnnotationGroup expected = new EntityAnnotationGroup();
    expected.add(annot1);
    assertThat(EntityProcessorImpl.getAnnotationGroups(annotationList, true), contains(expected));
  }

  @Test
  public void testGetMultipleLongestAnnotationGroups() {
    EntityAnnotation annot1 = new EntityAnnotation(mockEntity, 0, 4); 
    EntityAnnotation annot2 = new EntityAnnotation(mockEntity2, 3, 10);
    annotationList.add(annot1);
    annotationList.add(annot2);
    EntityAnnotationGroup expected = new EntityAnnotationGroup();
    expected.add(annot2);
    assertThat(EntityProcessorImpl.getAnnotationGroups(annotationList, true), contains(expected));
  }

  @Test
  public void testUniqueAnnotations() {
    EntityAnnotation annot1 = new EntityAnnotation(mockEntity, 0, 4); 
    EntityAnnotation annot2 = new EntityAnnotation(mockEntity, 3, 5);
    annotationList.add(annot1);
    annotationList.add(annot2);
    EntityAnnotationGroup group1 = new EntityAnnotationGroup();
    group1.add(annot1);
    assertThat(EntityProcessorImpl.getAnnotationGroups(annotationList, true), contains(group1));
  }

  @Test
  public void testGetBase() throws MalformedURLException {
    assertThat(EntityProcessorImpl.getBase(new URL("http://example.org:9000/foo/bar.html")),
        is("http://example.org:9000/foo/"));
  }

}
