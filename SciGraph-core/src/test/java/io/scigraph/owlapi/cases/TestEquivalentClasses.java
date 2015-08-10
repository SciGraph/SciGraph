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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.owlapi.OwlRelationships;

import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class TestEquivalentClasses extends OwlTestCase {

  /**
   * See https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#equivalence-axioms
   */
  @Test
  public void testEquivalentToIntersectionOf() {
    Node x = getNode("http://example.org/x");
    Node y = getNode("http://example.org/y");
    Node z = getNode("http://example.org/z");

    assertThat("equivalence is symmetric and holds between all members.",
        GraphUtil.getRelationships(x, y, OwlRelationships.OWL_EQUIVALENT_CLASS, false),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
    assertThat("equivalence is symmetric and holds between all members.",
        GraphUtil.getRelationships(x, z, OwlRelationships.OWL_EQUIVALENT_CLASS, false),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
    assertThat("equivalence is symmetric and holds between all members.",
        GraphUtil.getRelationships(y, z, OwlRelationships.OWL_EQUIVALENT_CLASS, false),
        is(IsIterableWithSize.<Relationship> iterableWithSize(1)));
  }

}
