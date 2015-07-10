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
package edu.sdsc.scigraph.owlapi.cases;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.google.common.base.Optional;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.neo4j.GraphUtil;
import edu.sdsc.scigraph.owlapi.OwlLabels;
import edu.sdsc.scigraph.owlapi.OwlRelationships;

public class TestClassAssertion extends OwlTestCase {

  @Test
  public void testObjectPropertyAssertion() {
    Node i = getNode("http://example.org/i");
    Node c = getNode("http://example.org/c");
    assertThat("classes are labeled as such", c.hasLabel(OwlLabels.OWL_CLASS));
    assertThat("named individuals are labeled as such", i.hasLabel(OwlLabels.OWL_NAMED_INDIVIDUAL));
    assertThat("fragment properties are set",
        GraphUtil.getProperty(c, CommonProperties.FRAGMENT, String.class), is(Optional.of("c")));
    assertThat("fragment properties are set",
        GraphUtil.getProperty(i, CommonProperties.FRAGMENT, String.class), is(Optional.of("i")));
    Relationship relationship = getOnlyElement(GraphUtil.getRelationships(i, c,
        OwlRelationships.RDF_TYPE));
    assertThat("OPE edge should start with the subject.", relationship.getStartNode(), is(i));
    assertThat("OPE edge should start with the target.", relationship.getEndNode(), is(c));
  }

  @Test
  public void anonymousLabelsAreAppliedToAnonymousIndividuals() {
    Node anon = getNode("_:anonymousIndividual");
    assertThat(anon.hasLabel(OwlLabels.OWL_ANONYMOUS), is(true));
    
  }

}
