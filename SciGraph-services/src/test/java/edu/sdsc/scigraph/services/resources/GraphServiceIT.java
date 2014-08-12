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
package edu.sdsc.scigraph.services.resources;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.dropwizard.testing.junit.ResourceTestRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.Concept;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.vocabulary.Vocabulary;

public class GraphServiceIT {

  @SuppressWarnings("unchecked")
  private static final Vocabulary vocabulary = mock(Vocabulary.class);
  @SuppressWarnings("unchecked")
  private static final Graph graph = mock(Graph.class);

  private final Concept foo = mock(Concept.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
      .addResource(new GraphService(vocabulary, graph)).build();

  @Before
  public void setup() {
    when(vocabulary.getConceptFromUri("http://example.org/none")).thenReturn(
        Optional.<Concept> absent());
    when(vocabulary.getConceptFromUri("http://example.org/foo")).thenReturn(Optional.of(foo));
  }

  @Test
  public void testPrefix() {

  }

}
