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
package io.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import io.scigraph.owlapi.OwlRelationships;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class TestInferredEdges extends OwlTestCase {

  public TestInferredEdges() {
    performInference = true;
  }
  
  @Test
  public void testInferredEdges() {
    Node cx = getNode("http://example.org/cx");
    Node dx = getNode("http://example.org/dx");

    Iterable<Relationship> superclasses = dx.getRelationships(OwlRelationships.RDFS_SUBCLASS_OF, Direction.OUTGOING);
    Relationship r = getOnlyElement(superclasses);
    assertThat("A subclassOf relationship is introduced.    ", r.getOtherNode(dx), is(cx));
  }

}
