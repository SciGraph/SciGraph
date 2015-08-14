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
package io.scigraph.owlapi.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectVisitor;
import org.semanticweb.owlapi.model.OWLOntology;

public class OwlOntologyConsumerTest {

  OwlOntologyConsumer consumer;
  BlockingQueue<OWLCompositeObject> queue = new LinkedBlockingQueue<>();
  OWLOntology ontology = mock(OWLOntology.class);
  OWLObject object = mock(OWLObject.class);

  @Before
  public void setup() {
    consumer = new OwlOntologyConsumer(queue, null, 1, Collections.<MappedProperty>emptyList(), new AtomicInteger(1));
  }

  @Test
  public void consumerShutsDown_whenNothingQueuedAndProducersShutdown() {
    assertThat(consumer.call(), is(0L));
  }

  @Test
  public void consumerProcessesSingleObject() {
    queue.add(new OWLCompositeObject("http://example.org", object));
    assertThat(consumer.call(), is(1L));
  }

  @Test
  public void acceptThrowsUncheckedException() {
    doThrow(new RuntimeException()).when(object).accept(any(OWLObjectVisitor.class));
    queue.add(new OWLCompositeObject("http://example.org", object));
    assertThat(consumer.call(), is(1L));
  }

}
