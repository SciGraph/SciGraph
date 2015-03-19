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
package edu.sdsc.scigraph.annotation;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.vocabulary.Vocabulary;
import edu.sdsc.scigraph.vocabulary.Vocabulary.Query;

public class EntityRecognizerTest {

  EntityFormatConfiguration config = mock(EntityFormatConfiguration.class);
  Concept concept;
  EntityRecognizer recognizer;

  @Before
  public void setUp() throws Exception {
    Vocabulary vocabulary = mock(Vocabulary.class);
    concept = new Concept(1L);
    concept.getLabels().add("foo");
    concept.setUri("http://x.org/1");
    when(vocabulary.getConceptsFromTerm(any(Query.class))).thenReturn(singletonList(concept));
    recognizer = new EntityRecognizer(vocabulary);
  }

  @Test
  public void testKnownEntity() {
    Collection<Entity> entities = recognizer.getEntities("foo", config);
    assertThat(entities, contains(new Entity(concept)));
  }

}
