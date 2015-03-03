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
package edu.sdsc.scigraph.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.neo4j.DirectedRelationshipType;
import edu.sdsc.scigraph.owlapi.CurieUtil;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiTest extends GraphTestBase {

  GraphApi graphApi;
  Node a, b, c;
  CurieUtil curieUtil = mock(CurieUtil.class);

  @Before
  public void addNodes() throws Exception {
    a = graphDb.createNode();
    b = graphDb.createNode();
    c = graphDb.createNode();
    b.createRelationshipTo(a, OwlRelationships.RDFS_SUBCLASS_OF);
    c.createRelationshipTo(b, OwlRelationships.OWL_EQUIVALENT_CLASS);
    when(curieUtil.getCurie(anyString())).thenReturn(Optional.of("curie"));
    graphApi = new GraphApi(graphDb, curieUtil);
  }

  @Test
  public void entailment_isReturned() {
    Collection<Node> entailment = graphApi.getEntailment(a, new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING), false);
    assertThat(entailment, contains(a, b));
  }

  @Test
  public void equivalentEntailment_isReturned() {
    Collection<Node> entailment = graphApi.getEntailment(a, new DirectedRelationshipType(OwlRelationships.RDFS_SUBCLASS_OF, Direction.INCOMING), true);
    assertThat(entailment, contains(a, b, c));
  }

}
