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
import io.scigraph.owlapi.OwlLabels;
import io.scigraph.owlapi.OwlRelationships;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class TestGCISubClassOf extends OwlTestCase {

  Node subclass, superclass;
  RelationshipType p = RelationshipType.withName("http://example.org/p");
  
  @Before
  public void setup() {
    subclass = getNode("http://example.org/subclass");
    superclass = getNode("http://example.org/superclass");
  }

  @Test
  public void testSubclassBlankNodeFiller() {
    Relationship r = getOnlyElement(subclass.getRelationships(Direction.INCOMING, OwlRelationships.FILLER));
    Node blankNode1 = r.getOtherNode(subclass);
    assertThat(blankNode1.hasLabel(OwlLabels.OWL_ANONYMOUS), is(true));
    r = getOnlyElement(blankNode1.getRelationships(Direction.OUTGOING, OwlRelationships.RDFS_SUBCLASS_OF));
    Node blankNode2 = r.getOtherNode(blankNode1);
    assertThat(blankNode2.hasLabel(OwlLabels.OWL_ANONYMOUS), is(true));
    r = getOnlyElement(blankNode1.getRelationships(Direction.OUTGOING, p));
    assertThat(r.getOtherNode(blankNode1), is(superclass));
  }

}
