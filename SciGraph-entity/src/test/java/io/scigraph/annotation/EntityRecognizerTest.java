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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.scigraph.annotation.Entity;
import io.scigraph.annotation.EntityFormatConfiguration;
import io.scigraph.annotation.EntityRecognizer;
import io.scigraph.frames.Concept;
import io.scigraph.vocabulary.Vocabulary;
import io.scigraph.vocabulary.Vocabulary.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.prefixcommons.CurieUtil;

public class EntityRecognizerTest {

  EntityFormatConfiguration config = mock(EntityFormatConfiguration.class);
  Concept concept;
  EntityRecognizer recognizer;

  @Before
  public void setUp() throws Exception {
    Vocabulary vocabulary = mock(Vocabulary.class);
    concept = new Concept(1L);
    concept.getLabels().add("foo");
    concept.setIri("http://x.org/1");
    when(vocabulary.getConceptsFromTerm(any(Query.class))).thenReturn(singletonList(concept));
    Map<String, String> curieMap = new HashMap<>();
    curieMap.put("X", "http://x.org/");
    CurieUtil curieUtil = new CurieUtil(curieMap);
    recognizer = new EntityRecognizer(vocabulary, curieUtil);
  }

  @Test
  public void testKnownEntity() {
    Collection<Entity> entities = recognizer.getEntities("foo", config);
    assertThat(entities, contains(new Entity(concept.getLabels(), "X:1")));
  }

}
