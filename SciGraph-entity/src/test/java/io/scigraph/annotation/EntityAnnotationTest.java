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
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.scigraph.annotation.EntityAnnotation;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class EntityAnnotationTest {

  EntityAnnotation annot1, annot2, annot3, annot4, annot5;

  @Before
  public void setUp() throws Exception {
    annot1 = new EntityAnnotation(null, -5, 6);
    annot2 = new EntityAnnotation(null, 1, 6);
    annot3 = new EntityAnnotation(null, 7, 20);
    annot4 = new EntityAnnotation(null, 0, 21);
    annot5 = new EntityAnnotation(null, 2, 3);
  }

  @Test
  public void testCompareTo() {
    List<EntityAnnotation> list = Lists.newArrayList(annot1, annot2, annot3, annot4, annot5);
    Collections.sort(list);
    List<EntityAnnotation> expected = newArrayList(annot5, annot2, annot1, annot3, annot4);
    assertThat(list, is(expected));
  }

}
