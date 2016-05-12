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
package io.scigraph.neo4j;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import io.scigraph.neo4j.DirectedRelationshipType;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

public class DirectedRelationshipTypeTest {

  @Test
  public void verifyEquals() {
    RelationshipType foo = RelationshipType.withName("foo");
    RelationshipType bar = RelationshipType.withName("bar");
    EqualsVerifier.forClass(DirectedRelationshipType.class)
    .withPrefabValues(RelationshipType.class, foo, bar)
    .suppress(Warning.NULL_FIELDS).verify();
  }

  @Test
  public void testEquals() {
    assertThat(new DirectedRelationshipType(RelationshipType.withName("foo"), Direction.INCOMING),
        is(equalTo(new DirectedRelationshipType(RelationshipType.withName("foo"), Direction.INCOMING))));
  }

}
