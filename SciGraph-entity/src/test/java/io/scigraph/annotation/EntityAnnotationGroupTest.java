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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import io.scigraph.annotation.EntityAnnotation;
import io.scigraph.annotation.EntityAnnotationGroup;

import org.junit.Before;
import org.junit.Test;

public class EntityAnnotationGroupTest {

  EntityAnnotation annot1;
  EntityAnnotation annot2;
  EntityAnnotation annot3;
  EntityAnnotation annot4;
  EntityAnnotation annot5;

  EntityAnnotationGroup group1;

  @Before
  public void setUp() throws Exception {
    annot1 = new EntityAnnotation(null, -5, 6);
    annot2 = new EntityAnnotation(null, 1, 6);
    annot3 = new EntityAnnotation(null, 7, 20);
    annot4 = new EntityAnnotation(null, 0, 21);
    annot5 = new EntityAnnotation(null, 2, 3);
    group1 = new EntityAnnotationGroup();
    group1.add(annot1);
    group1.add(annot2);
  }

  @Test
  public void testIntersects() {
    assertFalse(group1.intersects(annot3));
    assertTrue(group1.intersects(annot5));
    assertTrue(group1.intersects(annot4));
  }

  @Test
  public void testGetEnd() {
    assertEquals(6, group1.getEnd());
    group1.add(annot3);
    assertEquals(20, group1.getEnd());
  }

  @Test
  public void testGetStart() {
    assertEquals(-5, group1.getStart());
    group1.add(annot3);
    assertEquals(-5, group1.getStart());
  }

  @Test
  public void testPriorityQueue() {
    EntityAnnotationGroup group = new EntityAnnotationGroup();

    group.add(new EntityAnnotation(null, 0, 5));
    group.add(new EntityAnnotation(null, 0, 10));
    group.add(new EntityAnnotation(null, 0, 20));

    assertEquals(20, group.peek().length());
  }

  @Test
  public void testEquality() {
    EntityAnnotationGroup group = new EntityAnnotationGroup();
    group.add(new EntityAnnotation(null, 0, 5));
    EntityAnnotationGroup group2 = new EntityAnnotationGroup();
    group2.add(new EntityAnnotation(null, 0, 5));
    assertThat(group, is(group2));
  }

  @Test
  public void testNonEquality() {
    EntityAnnotationGroup group = new EntityAnnotationGroup();
    group.add(new EntityAnnotation(null, 0, 7));
    EntityAnnotationGroup group2 = new EntityAnnotationGroup();
    group2.add(new EntityAnnotation(null, 0, 5));
    assertThat(group, is(not(group2)));
  }

}
