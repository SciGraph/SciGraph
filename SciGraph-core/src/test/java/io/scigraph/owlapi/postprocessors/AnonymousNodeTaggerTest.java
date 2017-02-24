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
package io.scigraph.owlapi.postprocessors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import io.scigraph.owlapi.OwlLabels;
import io.scigraph.util.GraphTestBase;

public class AnonymousNodeTaggerTest extends GraphTestBase {

  AnonymousNodeTagger anonymousNodeTagger;

  Node n1;
  Node n2;
  String anonymousProperty = "anonymousProperty";

  @Before
  public void setup() {
    n1 = createNode("http://x.org/a");
    n2 = createNode("http://x.org/b");
    n1.setProperty("anonymousProperty", true);

    anonymousNodeTagger = new AnonymousNodeTagger(anonymousProperty, graphDb);
    anonymousNodeTagger.run();
  }

  @Test
  public void nodesAreTagged() {
    assertThat(n1.hasLabel(OwlLabels.OWL_ANONYMOUS), is(true));
    assertThat(n2.hasLabel(OwlLabels.OWL_ANONYMOUS), is(false));
  }
}
