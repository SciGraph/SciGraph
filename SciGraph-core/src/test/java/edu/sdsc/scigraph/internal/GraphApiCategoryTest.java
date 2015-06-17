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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlRelationships;
import edu.sdsc.scigraph.util.GraphTestBase;

public class GraphApiCategoryTest extends GraphTestBase {

  GraphApi graphApi;
  
  Node a, b, c;

  @Before
  public void addNodes() throws Exception {
    a = graphDb.createNode(OwlLabels.OWL_CLASS);
    b = graphDb.createNode(OwlLabels.OWL_CLASS);
    c = graphDb.createNode(OwlLabels.OWL_CLASS);
    a.createRelationshipTo(b, OwlRelationships.RDFS_SUBCLASS_OF);
    this.graphApi = new GraphApi(graphDb, null);
  }

  @Test
  public void testFoundClass() {
    assertThat(graphApi.classIsInCategory(a, b), is(true));
  }

  @Test
  public void testUnconnectedClass() {
    assertThat(graphApi.classIsInCategory(b, c), is(false));
  }

}
