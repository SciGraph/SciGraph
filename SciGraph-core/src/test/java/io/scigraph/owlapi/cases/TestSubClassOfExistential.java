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

import java.util.Optional;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import io.scigraph.frames.CommonProperties;
import io.scigraph.neo4j.GraphUtil;
import io.scigraph.owlapi.OwlRelationships;

public class TestSubClassOfExistential extends OwlTestCase {

  /**
   * See https://github.com/SciCrunch/SciGraph/wiki/MappingToOWL#subclassof-axioms
   * 
   * Reduction step should give us a simple edge {sub p super}
   */
  @Test
  public void testSubclass() {
    Node subclass = getNode("http://example.org/subclass");
    Node superclass = getNode("http://example.org/superclass");

    RelationshipType p = RelationshipType.withName("http://example.org/p");
    Relationship relationship = getOnlyElement(GraphUtil.getRelationships(subclass, superclass, p));
    assertThat("subclassOf relationship should start with the subclass.",
        relationship.getStartNode(), is(subclass));
    assertThat("subclassOf relationship should end with the superclass.",
        relationship.getEndNode(), is(superclass));
    assertThat("relationship has the correct iri",
        GraphUtil.getProperty(relationship, CommonProperties.IRI, String.class),
        is(Optional.of("http://example.org/p")));
    assertThat("relationship is asserted",
        GraphUtil.getProperty(relationship, CommonProperties.CONVENIENCE, Boolean.class),
        is(Optional.of(true)));
    assertThat("owltype is added",
        GraphUtil.getProperty(relationship, CommonProperties.OWL_TYPE, String.class),
        is(Optional.of(OwlRelationships.RDFS_SUBCLASS_OF.name())));
  }

}
