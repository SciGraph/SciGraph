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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.scigraph.neo4j.GraphUtil;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class TestObjectPropertyAssertion extends OwlTestCase {

  @Test
  public void testObjectPropertyAssertion() {
    Node i = getNode("http://example.org/i");
    Node j = getNode("http://example.org/j");

    RelationshipType p = RelationshipType.withName("http://example.org/p");
    Relationship relationship = getOnlyElement(GraphUtil.getRelationships(i, j, p));
    assertThat("OPE edge should start with the subject.", relationship.getStartNode(), is(i));
    assertThat("OPE edge should start with the target.", relationship.getEndNode(), is(j));
  }

}
